package com.example.musicplayer.ui.model

import com.example.musicplayer.data.db.AlbumSummary
import com.example.musicplayer.data.db.ArtistSummary
import com.example.musicplayer.data.db.FolderSummary
import com.example.musicplayer.data.db.PlaylistSummary
import com.example.musicplayer.data.db.RootDirectoryEntity
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.playback.PlaybackSnapshot

enum class LibraryTab(val label: String) {
    SONGS("\u6b4c\u66f2"),
    PLAYLISTS("\u6b4c\u5355"),
    ARTISTS("\u827a\u672f\u5bb6"),
    ALBUMS("\u4e13\u8f91"),
    FOLDERS("\u6587\u4ef6\u5939"),
    FAVORITES("\u6536\u85cf"),
    RECENT("\u6700\u8fd1\u64ad\u653e"),
}

sealed interface BrowserTarget {
    data class Artist(val artist: String) : BrowserTarget
    data class Album(val album: String, val artist: String) : BrowserTarget
    data class Folder(val folder: String) : BrowserTarget
    data class Playlist(val id: Long, val name: String) : BrowserTarget
}

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val message: String) : ScanState
    data class Finished(val message: String) : ScanState
    data class Error(val message: String) : ScanState
}

data class MusicPlayerUiState(
    val roots: List<RootDirectoryEntity> = emptyList(),
    val allTracks: List<TrackEntity> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val favorites: List<TrackEntity> = emptyList(),
    val recents: List<TrackEntity> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val browserTarget: BrowserTarget? = null,
    val browserTracks: List<TrackEntity> = emptyList(),
    val scanState: ScanState = ScanState.Idle,
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
)
