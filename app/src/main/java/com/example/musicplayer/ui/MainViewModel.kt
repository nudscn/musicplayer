package com.example.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.data.db.AlbumSummary
import com.example.musicplayer.data.db.ArtistSummary
import com.example.musicplayer.data.db.FolderSummary
import com.example.musicplayer.data.db.PlaylistSummary
import com.example.musicplayer.data.db.RootDirectoryEntity
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.playback.EqPreset
import com.example.musicplayer.playback.PlaybackSnapshot
import com.example.musicplayer.ui.model.BrowserTarget
import com.example.musicplayer.ui.model.LibraryTab
import com.example.musicplayer.ui.model.MusicPlayerUiState
import com.example.musicplayer.ui.model.ScanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MusicPlayerApp
    private val repository = app.repository
    private val playbackManager = app.playbackManager

    private val selectedTab = MutableStateFlow(LibraryTab.SONGS)
    private val browserTarget = MutableStateFlow<BrowserTarget?>(null)
    private val scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    private val playlistBrowserTracks = browserTarget.flatMapLatest { target ->
        when (target) {
            is BrowserTarget.Playlist -> repository.observePlaylistTracks(target.id)
            else -> flowOf(emptyList())
        }
    }

    val uiState: StateFlow<MusicPlayerUiState> = combine(
        repository.observeRoots(),
        repository.observeTracks(),
        repository.observeArtists(),
        repository.observeAlbums(),
        repository.observeFolders(),
        repository.observePlaylists(),
        repository.observeFavoriteTracks(),
        repository.observeRecentTracks(),
        selectedTab,
        browserTarget,
        playlistBrowserTracks,
        scanState,
        playbackManager.snapshot,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val roots = values[0] as List<RootDirectoryEntity>
        @Suppress("UNCHECKED_CAST")
        val tracks = values[1] as List<TrackEntity>
        @Suppress("UNCHECKED_CAST")
        val artists = values[2] as List<ArtistSummary>
        @Suppress("UNCHECKED_CAST")
        val albums = values[3] as List<AlbumSummary>
        @Suppress("UNCHECKED_CAST")
        val folders = values[4] as List<FolderSummary>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[5] as List<PlaylistSummary>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[6] as List<TrackEntity>
        @Suppress("UNCHECKED_CAST")
        val recents = values[7] as List<TrackEntity>
        val tab = values[8] as LibraryTab
        val browser = values[9] as BrowserTarget?
        @Suppress("UNCHECKED_CAST")
        val playlistTracks = values[10] as List<TrackEntity>
        val scan = values[11] as ScanState
        val playback = values[12] as PlaybackSnapshot

        val resolvedBrowserTracks = when (browser) {
            is BrowserTarget.Playlist -> playlistTracks
            else -> tracks.filterFor(browser)
        }

        val refreshedQueue = playback.queue.map { queued ->
            tracks.firstOrNull { it.contentUri == queued.contentUri } ?: queued
        }
        val refreshedCurrent = playback.currentTrack?.let { current ->
            tracks.firstOrNull { it.contentUri == current.contentUri } ?: current
        }

        MusicPlayerUiState(
            roots = roots,
            allTracks = tracks,
            artists = artists,
            albums = albums,
            folders = folders,
            playlists = playlists,
            favorites = favorites,
            recents = recents,
            selectedTab = tab,
            browserTarget = browser,
            scanState = scan,
            playback = playback.copy(
                currentTrack = refreshedCurrent,
                queue = refreshedQueue,
            ),
            browserTracks = resolvedBrowserTracks,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MusicPlayerUiState(),
    )

    fun addMusicDirectory(uri: Uri) {
        viewModelScope.launch {
            scanState.value = ScanState.Scanning("\u6b63\u5728\u626b\u63cf\u65b0\u76ee\u5f55\u2026")
            runCatching {
                repository.addRootAndScan(uri)
            }.onSuccess { result ->
                scanState.value =
                    ScanState.Finished("\u5df2\u52a0\u5165 ${result.rootName}\uff0c\u53d1\u73b0 ${result.trackCount} \u9996\u6b4c")
            }.onFailure { error ->
                scanState.value = ScanState.Error(error.message ?: "\u76ee\u5f55\u626b\u63cf\u5931\u8d25")
            }
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            scanState.value = ScanState.Scanning("\u6b63\u5728\u5237\u65b0\u672c\u5730\u66f2\u5e93\u2026")
            runCatching {
                repository.rescanAllRoots()
            }.onSuccess { results ->
                val totalTracks = results.sumOf { it.trackCount }
                scanState.value =
                    ScanState.Finished("\u5237\u65b0\u5b8c\u6210\uff0c\u5171\u7d22\u5f15 $totalTracks \u9996\u6b4c")
            }.onFailure { error ->
                scanState.value = ScanState.Error(error.message ?: "\u5237\u65b0\u5931\u8d25")
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        selectedTab.value = tab
        if (!tabSupportsTarget(tab, browserTarget.value)) {
            browserTarget.value = null
        }
    }

    fun openArtist(name: String) {
        selectedTab.value = LibraryTab.ARTISTS
        browserTarget.value = BrowserTarget.Artist(name)
    }

    fun openAlbum(name: String, artist: String) {
        selectedTab.value = LibraryTab.ALBUMS
        browserTarget.value = BrowserTarget.Album(name, artist)
    }

    fun openFolder(path: String) {
        selectedTab.value = LibraryTab.FOLDERS
        browserTarget.value = BrowserTarget.Folder(path)
    }

    fun openPlaylist(id: Long, name: String) {
        selectedTab.value = LibraryTab.PLAYLISTS
        browserTarget.value = BrowserTarget.Playlist(id, name)
    }

    fun clearBrowser() {
        browserTarget.value = null
    }

    fun playTrack(track: TrackEntity) {
        val state = uiState.value
        val source = if (state.browserTarget != null) state.browserTracks else state.selectedTrackList()
        val index = source.indexOfFirst { it.contentUri == track.contentUri }
        playbackManager.setQueue(source, index.coerceAtLeast(0), playWhenReady = true)
    }

    fun playCurrentBrowser() {
        val state = uiState.value
        if (state.browserTarget == null || state.browserTracks.isEmpty()) return
        playbackManager.setQueue(state.browserTracks, 0, playWhenReady = true)
    }

    fun playFromQueue(track: TrackEntity) {
        val index = uiState.value.playback.queue.indexOfFirst { it.contentUri == track.contentUri }
        playbackManager.playTrackFromQueue(index)
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.setFavorite(track, !track.isFavorite)
        }
    }

    fun createPlaylist(name: String, initialTrack: TrackEntity? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(trimmed)
            if (initialTrack != null) {
                repository.addTrackToPlaylist(playlistId, initialTrack)
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: TrackEntity) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun startSleepTimer(minutes: Int) = playbackManager.startSleepTimer(minutes)

    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()

    fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, trimmed)
            val current = browserTarget.value
            if (current is BrowserTarget.Playlist && current.id == playlistId) {
                browserTarget.value = current.copy(name = trimmed)
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            val current = browserTarget.value
            if (current is BrowserTarget.Playlist && current.id == playlistId) {
                browserTarget.value = null
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, track: TrackEntity) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, track)
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun next() = playbackManager.next()
    fun previous() = playbackManager.previous()
    fun seekTo(fraction: Float) = playbackManager.seekTo(fraction)
    fun cyclePlaybackMode() = playbackManager.cycleQueueMode()
    fun selectEqPreset(preset: EqPreset) = playbackManager.setEqPreset(preset)

    private fun tabSupportsTarget(tab: LibraryTab, target: BrowserTarget?): Boolean =
        when (target) {
            is BrowserTarget.Artist -> tab == LibraryTab.ARTISTS
            is BrowserTarget.Album -> tab == LibraryTab.ALBUMS
            is BrowserTarget.Folder -> tab == LibraryTab.FOLDERS
            is BrowserTarget.Playlist -> tab == LibraryTab.PLAYLISTS
            null -> true
        }
}

private fun List<TrackEntity>.filterFor(target: BrowserTarget?): List<TrackEntity> =
    when (target) {
        is BrowserTarget.Artist -> filter { it.artist == target.artist }
        is BrowserTarget.Album -> filter { it.album == target.album && it.artist == target.artist }
        is BrowserTarget.Folder -> filter { it.folder == target.folder }
        is BrowserTarget.Playlist,
        null -> emptyList()
    }.sortedBy { it.trackNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }

private fun MusicPlayerUiState.selectedTrackList(): List<TrackEntity> =
    when (selectedTab) {
        LibraryTab.SONGS -> allTracks
        LibraryTab.PLAYLISTS -> browserTracks
        LibraryTab.FAVORITES -> favorites
        LibraryTab.RECENT -> recents
        LibraryTab.ARTISTS -> browserTracks
        LibraryTab.ALBUMS -> browserTracks
        LibraryTab.FOLDERS -> browserTracks
    }
