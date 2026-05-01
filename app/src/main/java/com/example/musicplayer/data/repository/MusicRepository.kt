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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class ScanResult(
    val rootUri: String,
    val rootName: String,
    val trackCount: Int,
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

    suspend fun addRootAndScan(uri: Uri): ScanResult = withContext(Dispatchers.IO) {
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
        )

        dao.replaceRootTracks(rootUri, scannedTracks, scannedAt)
        ScanResult(rootUri, rootName, scannedTracks.size)
    }

    suspend fun rescanAllRoots(): List<ScanResult> = withContext(Dispatchers.IO) {
        val roots = dao.getRootsOnce()
        roots.map { root ->
            val rootDocument = DocumentFile.fromTreeUri(context, Uri.parse(root.uri))
                ?: return@map ScanResult(root.uri, root.displayName, 0)
            val previousTracks = dao.getTracksForRoot(root.uri).associateBy { it.contentUri }
            val tracks = scanner.scanTree(rootDocument, root.uri, previousTracks)
            dao.replaceRootTracks(root.uri, tracks, System.currentTimeMillis())
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

    suspend fun markPlayed(trackUri: String) {
        dao.markPlayed(trackUri, System.currentTimeMillis())
    }
}
