package com.example.musicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM root_directories ORDER BY addedAt ASC")
    suspend fun getRootsOnce(): List<RootDirectoryEntity>

    @Query("SELECT * FROM root_directories ORDER BY addedAt ASC")
    fun observeRoots(): Flow<List<RootDirectoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoot(root: RootDirectoryEntity)

    @Query("UPDATE root_directories SET lastScannedAt = :lastScannedAt WHERE uri = :rootUri")
    suspend fun updateRootScanTime(rootUri: String, lastScannedAt: Long)

    @Query("SELECT * FROM tracks WHERE rootUri = :rootUri")
    suspend fun getTracksForRoot(rootUri: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE contentUri = :contentUri LIMIT 1")
    suspend fun getTrackByContentUri(contentUri: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE contentUri IN (:contentUris)")
    suspend fun getTracksByContentUris(contentUris: List<String>): List<TrackEntity>

    @Query(
        """
        SELECT playlists.id, playlists.name, COUNT(playlist_tracks.trackUri) AS trackCount
        FROM playlists
        LEFT JOIN playlist_tracks ON playlist_tracks.playlistId = playlists.id
        GROUP BY playlists.id, playlists.name, playlists.createdAt
        ORDER BY LOWER(playlists.name) ASC, playlists.createdAt ASC
        """,
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query(
        """
        SELECT tracks.*
        FROM tracks
        INNER JOIN playlist_tracks ON playlist_tracks.trackUri = tracks.contentUri
        WHERE playlist_tracks.playlistId = :playlistId
        ORDER BY playlist_tracks.addedAt ASC
        """,
    )
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrackCrossRef(crossRef: PlaylistTrackCrossRef): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackUri = :trackUri")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackUri: String)

    @Query("DELETE FROM tracks WHERE rootUri = :rootUri")
    suspend fun deleteTracksByRoot(rootUri: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks ORDER BY LOWER(title) ASC")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY LOWER(title) ASC")
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun observeRecentTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT artist, COUNT(*) AS trackCount
        FROM tracks
        GROUP BY artist
        ORDER BY LOWER(artist) ASC
        """,
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT album, artist, COUNT(*) AS trackCount
        FROM tracks
        GROUP BY album, artist
        ORDER BY LOWER(album) ASC
        """,
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """
        SELECT folder, COUNT(*) AS trackCount
        FROM tracks
        GROUP BY folder
        ORDER BY LOWER(folder) ASC
        """,
    )
    fun observeFolders(): Flow<List<FolderSummary>>

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE contentUri = :contentUri")
    suspend fun setFavorite(contentUri: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE tracks
        SET title = :title,
            artist = :artist,
            album = :album,
            customTitle = :customTitle,
            customArtist = :customArtist,
            customAlbum = :customAlbum
        WHERE contentUri = :contentUri
        """,
    )
    suspend fun updateTrackMetadata(
        contentUri: String,
        title: String,
        artist: String,
        album: String,
        customTitle: String?,
        customArtist: String?,
        customAlbum: String?,
    )

    @Query(
        """
        UPDATE tracks
        SET lastPlayedAt = :playedAt,
            playCount = playCount + 1
        WHERE contentUri = :contentUri
        """,
    )
    suspend fun markPlayed(contentUri: String, playedAt: Long)

    @Transaction
    suspend fun replaceRootTracks(
        rootUri: String,
        tracks: List<TrackEntity>,
        scannedAt: Long,
    ) {
        deleteTracksByRoot(rootUri)
        if (tracks.isNotEmpty()) {
            insertTracks(tracks)
        }
        updateRootScanTime(rootUri, scannedAt)
    }
}
