package com.example.musicplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.musicplayer.data.db.AlbumSummary
import com.example.musicplayer.data.db.ArtistSummary
import com.example.musicplayer.data.db.FolderSummary
import com.example.musicplayer.data.db.LibraryDao
import com.example.musicplayer.data.db.PlaylistEntity
import com.example.musicplayer.data.db.PlaylistSummary
import com.example.musicplayer.data.db.PlaylistTrackCrossRef
import com.example.musicplayer.data.db.RootDirectoryEntity
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.data.scanner.MusicScanner
import com.example.musicplayer.data.scanner.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class ScanResult(
    val rootUri: String,
    val rootName: String,
    val trackCount: Int,
)

data class LibraryScanProgress(
    val rootName: String,
    val rootIndex: Int,
    val rootCount: Int,
    val rootTrackCount: Int,
    val totalTrackCount: Int,
    val currentTrackName: String,
    val currentFolder: String,
)

class MusicRepository(
    private val context: Context,
    private val dao: LibraryDao,
) {
    private val scanner = MusicScanner(context)

    fun observeRoots(): Flow<List<RootDirectoryEntity>> = dao.observeRoots()
    fun observeTracks(): Flow<List<TrackEntity>> = dao.observeTracks()
    fun observeFavoriteTracks(): Flow<List<TrackEntity>> = dao.observeFavoriteTracks()
    fun observeRecentTracks(): Flow<List<TrackEntity>> = dao.observeRecentTracks()
    fun observeArtists(): Flow<List<ArtistSummary>> = dao.observeArtists()
    fun observeAlbums(): Flow<List<AlbumSummary>> = dao.observeAlbums()
    fun observeFolders(): Flow<List<FolderSummary>> = dao.observeFolders()
    fun observePlaylists(): Flow<List<PlaylistSummary>> = dao.observePlaylists()
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>> = dao.observePlaylistTracks(playlistId)

    suspend fun getTrackByContentUri(contentUri: String): TrackEntity? = withContext(Dispatchers.IO) {
        dao.getTrackByContentUri(contentUri)
    }

    suspend fun getTracksByContentUris(contentUris: List<String>): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (contentUris.isEmpty()) return@withContext emptyList()
        val tracksByUri = dao.getTracksByContentUris(contentUris).associateBy { it.contentUri }
        contentUris.mapNotNull { tracksByUri[it] }
    }

    suspend fun addRootAndScan(
        uri: Uri,
        onProgress: ((LibraryScanProgress) -> Unit)? = null,
    ): ScanResult = withContext(Dispatchers.IO) {
        val rootDocument = DocumentFile.fromTreeUri(context, uri)
            ?: error("Unable to open selected directory.")
        val rootName = rootDocument.name ?: "Music Folder"
        val rootUri = uri.toString()
        val scannedAt = System.currentTimeMillis()
        val previousTracks = dao.getTracksForRoot(rootUri).associateBy { it.contentUri }

        dao.upsertRoot(
            RootDirectoryEntity(
                uri = rootUri,
                displayName = rootName,
                addedAt = scannedAt,
                lastScannedAt = null,
            ),
        )

        val scannedTracks = scanner.scanTree(
            rootDocument = rootDocument,
            rootUri = rootUri,
            previousTracks = previousTracks,
            onProgress = { progress ->
                onProgress?.invoke(
                    progress.toLibraryScanProgress(
                        rootName = rootName,
                        rootIndex = 1,
                        rootCount = 1,
                        totalOffset = 0,
                    ),
                )
            },
        )

        dao.replaceRootTracks(rootUri, scannedTracks, scannedAt)
        ScanResult(rootUri, rootName, scannedTracks.size)
    }

    suspend fun rescanAllRoots(
        onProgress: ((LibraryScanProgress) -> Unit)? = null,
    ): List<ScanResult> = withContext(Dispatchers.IO) {
        val roots = dao.getRootsOnce()
        var totalTrackCount = 0
        roots.mapIndexed { index, root ->
            val rootDocument = DocumentFile.fromTreeUri(context, Uri.parse(root.uri))
                ?: return@mapIndexed ScanResult(root.uri, root.displayName, 0)
            val previousTracks = dao.getTracksForRoot(root.uri).associateBy { it.contentUri }
            val tracks = scanner.scanTree(
                rootDocument = rootDocument,
                rootUri = root.uri,
                previousTracks = previousTracks,
                onProgress = { progress ->
                    onProgress?.invoke(
                        progress.toLibraryScanProgress(
                            rootName = root.displayName,
                            rootIndex = index + 1,
                            rootCount = roots.size,
                            totalOffset = totalTrackCount,
                        ),
                    )
                },
            )
            dao.replaceRootTracks(root.uri, tracks, System.currentTimeMillis())
            totalTrackCount += tracks.size
            ScanResult(root.uri, root.displayName, tracks.size)
        }
    }

    suspend fun setFavorite(track: TrackEntity, isFavorite: Boolean) {
        dao.setFavorite(track.contentUri, isFavorite)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: TrackEntity): Boolean = withContext(Dispatchers.IO) {
        dao.insertPlaylistTrackCrossRef(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackUri = track.contentUri,
                addedAt = System.currentTimeMillis(),
            ),
        ) != -1L
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) = withContext(Dispatchers.IO) {
        dao.renamePlaylist(playlistId, name.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, track: TrackEntity) = withContext(Dispatchers.IO) {
        dao.removeTrackFromPlaylist(playlistId, track.contentUri)
    }

    suspend fun editTrackMetadata(
        track: TrackEntity,
        title: String,
        artist: String,
        album: String,
    ) = withContext(Dispatchers.IO) {
        val trimmedTitle = title.trim()
        val trimmedArtist = artist.trim()
        val trimmedAlbum = album.trim()
        require(trimmedTitle.isNotEmpty()) { "Song title cannot be empty." }
        require(trimmedArtist.isNotEmpty()) { "Artist cannot be empty." }
        require(trimmedAlbum.isNotEmpty()) { "Album cannot be empty." }
        dao.updateTrackMetadata(
            contentUri = track.contentUri,
            title = trimmedTitle,
            artist = trimmedArtist,
            album = trimmedAlbum,
            customTitle = trimmedTitle,
            customArtist = trimmedArtist,
            customAlbum = trimmedAlbum,
        )
    }

    suspend fun markPlayed(trackUri: String) {
        dao.markPlayed(trackUri, System.currentTimeMillis())
    }
}

private fun ScanProgress.toLibraryScanProgress(
    rootName: String,
    rootIndex: Int,
    rootCount: Int,
    totalOffset: Int,
): LibraryScanProgress =
    LibraryScanProgress(
        rootName = rootName,
        rootIndex = rootIndex,
        rootCount = rootCount,
        rootTrackCount = scannedTrackCount,
        totalTrackCount = totalOffset + scannedTrackCount,
        currentTrackName = currentTrackName,
        currentFolder = currentFolder,
    )
