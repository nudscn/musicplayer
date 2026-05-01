package com.example.musicplayer.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.db.AlbumSummary
import com.example.musicplayer.data.db.ArtistSummary
import com.example.musicplayer.data.db.FolderSummary
import com.example.musicplayer.data.db.PlaylistSummary
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.playback.EqPreset
import com.example.musicplayer.ui.model.BrowserTarget
import com.example.musicplayer.ui.model.LibraryTab
import com.example.musicplayer.ui.model.MusicPlayerUiState
import com.example.musicplayer.ui.model.ScanState
import com.example.musicplayer.ui.theme.Accent
import com.example.musicplayer.ui.theme.AccentWarm
import com.example.musicplayer.ui.theme.Obsidian
import com.example.musicplayer.ui.theme.Panel
import com.example.musicplayer.ui.theme.PanelAlt
import com.example.musicplayer.ui.theme.TextMuted
import com.example.musicplayer.util.formatAsClock
import com.example.musicplayer.util.loadLyricsBody
import com.example.musicplayer.util.loadLyricsPreview

@Composable
fun MusicPlayerScreen(
    state: MusicPlayerUiState,
    onAddDirectory: () -> Unit,
    onRescan: () -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onPlayCurrentBrowser: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (Long, String) -> Unit,
    onBackFromBrowser: () -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onCreatePlaylist: (String, TrackEntity?) -> Unit,
    onAddTrackToPlaylist: (Long, TrackEntity) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveTrackFromPlaylist: (Long, TrackEntity) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onSelectEqPreset: (EqPreset) -> Unit,
    onSelectQueueTrack: (TrackEntity) -> Unit,
) {
    var pendingPlaylistTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PlaylistSummary?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistSummary?>(null) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    fun openPlaylistPicker(track: TrackEntity) {
        pendingPlaylistTrack = track
        showPlaylistPicker = true
    }

    fun openCreatePlaylist(track: TrackEntity? = pendingPlaylistTrack) {
        pendingPlaylistTrack = track
        showPlaylistPicker = false
        showCreatePlaylistDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HeaderPanel(
                        rootCount = state.roots.size,
                        scanState = state.scanState,
                        onAddDirectory = onAddDirectory,
                        onRescan = onRescan,
                    )
                }
                item {
                    NowPlayingPanel(
                        state = state,
                        onTogglePlayback = onTogglePlayback,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onCyclePlaybackMode = onCyclePlaybackMode,
                        onSelectEqPreset = onSelectEqPreset,
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = ::openPlaylistPicker,
                        onOpenFullPlayer = { if (state.playback.currentTrack != null) showFullPlayer = true },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                    )
                }
                item {
                    LibraryTabs(
                        selected = state.selectedTab,
                        onSelectTab = onSelectTab,
                    )
                }
                if (state.selectedTab == LibraryTab.PLAYLISTS && state.browserTarget == null) {
                    item {
                        PlaylistActionPanel(
                            playlistCount = state.playlists.size,
                            onCreatePlaylist = { openCreatePlaylist(null) },
                        )
                    }
                }
                if (state.browserTarget != null) {
                    item {
                        BrowserHeader(
                            target = state.browserTarget,
                            browserTrackCount = state.browserTracks.size,
                            onBack = onBackFromBrowser,
                            onPlayBrowser = onPlayCurrentBrowser,
                            onRenamePlaylist = { target ->
                                playlistToRename = PlaylistSummary(
                                    id = target.id,
                                    name = target.name,
                                    trackCount = state.browserTracks.size,
                                )
                            },
                            onDeletePlaylist = { target ->
                                playlistToDelete = PlaylistSummary(
                                    id = target.id,
                                    name = target.name,
                                    trackCount = state.browserTracks.size,
                                )
                            },
                        )
                    }
                }

                when {
                    state.browserTarget != null -> {
                        items(state.browserTracks, key = { it.contentUri }) { track ->
                            TrackRow(
                                track = track,
                                currentTrack = state.playback.currentTrack,
                                onPlayTrack = onPlayTrack,
                                onToggleFavorite = onToggleFavorite,
                                onAddToPlaylist = ::openPlaylistPicker,
                                playlistIdForRemoval = (state.browserTarget as? BrowserTarget.Playlist)?.id,
                                onRemoveFromPlaylist = onRemoveTrackFromPlaylist,
                            )
                        }
                    }

                    state.selectedTab == LibraryTab.SONGS -> {
                        items(state.allTracks, key = { it.contentUri }) { track ->
                            TrackRow(
                                track = track,
                                currentTrack = state.playback.currentTrack,
                                onPlayTrack = onPlayTrack,
                                onToggleFavorite = onToggleFavorite,
                                onAddToPlaylist = ::openPlaylistPicker,
                            )
                        }
                    }

                    state.selectedTab == LibraryTab.PLAYLISTS -> {
                        if (state.playlists.isEmpty()) {
                            item { Spacer(Modifier.height(1.dp)) }
                        } else {
                            items(state.playlists, key = { it.id }) { playlist ->
                                PlaylistRow(
                                    playlist = playlist,
                                    onOpenPlaylist = onOpenPlaylist,
                                    onRenamePlaylist = { playlistToRename = playlist },
                                    onDeletePlaylist = { playlistToDelete = playlist },
                                )
                            }
                        }
                    }

                    state.selectedTab == LibraryTab.FAVORITES -> {
                        items(state.favorites, key = { it.contentUri }) { track ->
                            TrackRow(
                                track = track,
                                currentTrack = state.playback.currentTrack,
                                onPlayTrack = onPlayTrack,
                                onToggleFavorite = onToggleFavorite,
                                onAddToPlaylist = ::openPlaylistPicker,
                            )
                        }
                    }

                    state.selectedTab == LibraryTab.RECENT -> {
                        items(state.recents, key = { it.contentUri }) { track ->
                            TrackRow(
                                track = track,
                                currentTrack = state.playback.currentTrack,
                                onPlayTrack = onPlayTrack,
                                onToggleFavorite = onToggleFavorite,
                                onAddToPlaylist = ::openPlaylistPicker,
                            )
                        }
                    }

                    state.selectedTab == LibraryTab.ARTISTS -> {
                        items(state.artists, key = { it.artist }) { artist ->
                            ArtistRow(artist, onOpenArtist)
                        }
                    }

                    state.selectedTab == LibraryTab.ALBUMS -> {
                        items(state.albums, key = { "${it.album}:${it.artist}" }) { album ->
                            AlbumRow(album, onOpenAlbum)
                        }
                    }

                    state.selectedTab == LibraryTab.FOLDERS -> {
                        items(state.folders, key = { it.folder }) { folder ->
                            FolderRow(folder, onOpenFolder)
                        }
                    }
                }

                if (state.selectedTab != LibraryTab.PLAYLISTS && state.playback.queue.isNotEmpty()) {
                    item {
                        QueuePanel(
                            currentTrack = state.playback.currentTrack,
                            queue = state.playback.queue,
                            onSelectQueueTrack = onSelectQueueTrack,
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        if (showFullPlayer && state.playback.currentTrack != null) {
            FullPlayerOverlay(
                state = state,
                onDismiss = { showFullPlayer = false },
                onTogglePlayback = onTogglePlayback,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onCyclePlaybackMode = onCyclePlaybackMode,
                onSelectEqPreset = onSelectEqPreset,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = ::openPlaylistPicker,
                onOpenSleepTimer = { showSleepTimerDialog = true },
            )
        }
    }

    if (showPlaylistPicker && pendingPlaylistTrack != null) {
        PlaylistPickerDialog(
            playlists = state.playlists,
            track = pendingPlaylistTrack!!,
            onDismiss = { showPlaylistPicker = false },
            onCreateNew = { openCreatePlaylist(pendingPlaylistTrack) },
            onSelectPlaylist = { playlist ->
                onAddTrackToPlaylist(playlist.id, pendingPlaylistTrack!!)
                showPlaylistPicker = false
                pendingPlaylistTrack = null
            },
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreatePlaylistDialog = false
                pendingPlaylistTrack = null
            },
            onConfirm = { name ->
                onCreatePlaylist(name, pendingPlaylistTrack)
                showCreatePlaylistDialog = false
                pendingPlaylistTrack = null
            },
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = state.playback.sleepTimerRemainingMs,
            onDismiss = { showSleepTimerDialog = false },
            onPresetSelected = { minutes ->
                onStartSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                onCancelSleepTimer()
                showSleepTimerDialog = false
            },
        )
    }

    playlistToRename?.let { playlist ->
        RenamePlaylistDialog(
            playlist = playlist,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                onRenamePlaylist(playlist.id, newName)
                playlistToRename = null
            },
        )
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlist = playlist,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                onDeletePlaylist(playlist.id)
                playlistToDelete = null
            },
        )
    }
}

@Composable
private fun HeaderPanel(
    rootCount: Int,
    scanState: ScanState,
    onAddDirectory: () -> Unit,
    onRescan: () -> Unit,
) {
    LegacyPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("MusicPlayer", style = MaterialTheme.typography.headlineSmall)
                SpectrumBars()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAddDirectory) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("添加音乐目录")
                }
                TextButton(onClick = onRescan) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("重新扫描")
                }
            }
            Text("已添加目录：$rootCount", color = TextMuted)
            when (scanState) {
                ScanState.Idle -> Unit
                is ScanState.Scanning -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(scanState.message, color = AccentWarm)
                }
                is ScanState.Finished -> Text(scanState.message, color = Accent)
                is ScanState.Error -> Text(scanState.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NowPlayingPanel(
    state: MusicPlayerUiState,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onSelectEqPreset: (EqPreset) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (TrackEntity) -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
) {
    val track = state.playback.currentTrack
    val durationMs = state.playback.durationMs.takeIf { it > 0 } ?: track?.durationMs ?: 0L
    val progress = if (durationMs > 0L) {
        (state.playback.currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    LegacyPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("当前播放", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = track != null, onClick = onOpenFullPlayer)
                    .padding(2.dp),
            ) {
                ArtworkBox(track = track, modifier = Modifier.size(108.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = track?.title ?: "还没有开始播放",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.artist ?: "请选择一首歌曲开始播放",
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.album ?: "",
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.playback.eqPreset.label,
                        color = AccentWarm,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "点击打开全屏播放页",
                        color = Accent,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (track != null) {
                    IconButton(onClick = { onToggleFavorite(track) }) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (track.isFavorite) AccentWarm else Color.White,
                        )
                    }
                }
            }

            if (track != null) {
                TextButton(onClick = { onAddToPlaylist(track) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("加入歌单")
                }
            }

            Slider(
                value = progress,
                onValueChange = onSeek,
                enabled = track != null && durationMs > 0L,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(state.playback.currentPositionMs.formatAsClock(), color = TextMuted)
                Text(durationMs.formatAsClock(), color = TextMuted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = state.playback.queue.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Accent,
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = onTogglePlayback, enabled = track != null) {
                    Icon(
                        imageVector = if (state.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.playback.isPlaying) "暂停" else "播放",
                        tint = AccentWarm,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = onNext, enabled = state.playback.queue.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Accent,
                        modifier = Modifier.size(30.dp),
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = onCyclePlaybackMode,
                    label = { Text(state.playback.queueMode.label) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EqPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.playback.eqPreset == preset,
                        onClick = { onSelectEqPreset(preset) },
                        label = { Text(preset.label) },
                    )
                }
                SleepTimerChip(
                    remainingMs = state.playback.sleepTimerRemainingMs,
                    onClick = onOpenSleepTimer,
                )
            }

            LyricsPanel(track = track)

            state.playback.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PlaylistActionPanel(
    playlistCount: Int,
    onCreatePlaylist: () -> Unit,
) {
    LegacyPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("自建歌单", style = MaterialTheme.typography.titleLarge)
                Text("已创建 $playlistCount 个歌单", color = TextMuted)
            }
            TextButton(onClick = onCreatePlaylist) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("新建歌单")
            }
        }
    }
}

@Composable
private fun FullPlayerOverlay(
    state: MusicPlayerUiState,
    onDismiss: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onSelectEqPreset: (EqPreset) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (TrackEntity) -> Unit,
    onOpenSleepTimer: () -> Unit,
) {
    val track = state.playback.currentTrack ?: return
    val durationMs = state.playback.durationMs.takeIf { it > 0 } ?: track.durationMs
    val progress = if (durationMs > 0L) {
        (state.playback.currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val artwork by rememberArtwork(track.artworkSource ?: track.contentUri)
    val lyrics by rememberLyricsBody(track)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Obsidian,
                        Panel,
                        Obsidian,
                    ),
                ),
            ),
    ) {
        artwork?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentScale = ContentScale.Crop,
                alpha = 0.16f,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                FilterChip(
                    selected = false,
                    onClick = onCyclePlaybackMode,
                    label = { Text(state.playback.queueMode.label) },
                )
            }

            ArtworkBox(
                track = track,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted,
                )
                Text(
                    text = track.album,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onAddToPlaylist(track) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("加入歌单")
                }
                IconButton(onClick = { onToggleFavorite(track) }) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (track.isFavorite) AccentWarm else Color.White,
                    )
                }
            }

            Slider(
                value = progress,
                onValueChange = onSeek,
                enabled = durationMs > 0L,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(state.playback.currentPositionMs.formatAsClock(), color = TextMuted)
                Text(durationMs.formatAsClock(), color = TextMuted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = state.playback.queue.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Accent,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = onTogglePlayback) {
                    Icon(
                        imageVector = if (state.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.playback.isPlaying) "暂停" else "播放",
                        tint = AccentWarm,
                        modifier = Modifier.size(48.dp),
                    )
                }
                IconButton(onClick = onNext, enabled = state.playback.queue.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Accent,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EqPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.playback.eqPreset == preset,
                        onClick = { onSelectEqPreset(preset) },
                        label = { Text(preset.label) },
                    )
                }
                SleepTimerChip(
                    remainingMs = state.playback.sleepTimerRemainingMs,
                    onClick = onOpenSleepTimer,
                )
            }

            FullLyricsPanel(lyrics = lyrics)
        }
    }
}

@Composable
private fun ArtworkBox(
    track: TrackEntity?,
    modifier: Modifier = Modifier,
) {
    val artwork by rememberArtwork(track?.artworkSource ?: track?.contentUri)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(PanelAlt, Panel),
                ),
            )
            .border(1.dp, Accent.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = "MUSIC",
                color = TextMuted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing,
            )
        }
    }
}

@Composable
private fun LyricsPanel(track: TrackEntity?) {
    val lyricsPreview by rememberLyricsPreview(track)
    LegacySubPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("歌词", style = MaterialTheme.typography.titleLarge)
            Text(
                text = lyricsPreview ?: "如果存在同名 .lrc 文件，这里会显示前几行歌词预览。",
                color = TextMuted,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }
    }
}

@Composable
private fun FullLyricsPanel(lyrics: String?) {
    LegacySubPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("歌词", style = MaterialTheme.typography.titleLarge)
            Text(
                text = lyrics ?: "如果存在同名 .lrc 文件，这里会显示完整歌词。后面我们还可以继续升级成同步滚动歌词。",
                color = TextMuted,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }
    }
}

@Composable
private fun SleepTimerChip(
    remainingMs: Long?,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = remainingMs != null,
        onClick = onClick,
        label = {
            Text(
                if (remainingMs == null) {
                    "睡眠模式"
                } else {
                    "睡眠剩余 ${remainingMs.formatAsClock()}"
                },
            )
        },
    )
}

@Composable
private fun QueuePanel(
    currentTrack: TrackEntity?,
    queue: List<TrackEntity>,
    onSelectQueueTrack: (TrackEntity) -> Unit,
) {
    val currentIndex = currentTrack?.let { now ->
        queue.indexOfFirst { it.contentUri == now.contentUri }
    } ?: -1
    val upcomingQueue = when {
        queue.isEmpty() -> emptyList()
        currentIndex in queue.indices -> queue.drop(currentIndex + 1)
        else -> queue
    }

    LegacyPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("待播列表", style = MaterialTheme.typography.titleMedium)
            Text(
                "这里只显示当前播放队列中接下来将要播放的歌曲，不是历史记录。点击某一首可以直接切换过去。",
                color = TextMuted,
            )
            if (upcomingQueue.isEmpty()) {
                Text("当前没有待播歌曲。", color = TextMuted)
            } else {
                upcomingQueue.take(12).forEach { track ->
                    TrackQueueRow(
                        track = track,
                        isCurrent = false,
                        onSelectQueueTrack = onSelectQueueTrack,
                    )
                }
                if (upcomingQueue.size > 12) {
                    Text("还有 ${upcomingQueue.size - 12} 首未显示", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun LibraryTabs(
    selected: LibraryTab,
    onSelectTab: (LibraryTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryTab.entries.forEach { tab ->
            FilterChip(
                selected = tab == selected,
                onClick = { onSelectTab(tab) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun BrowserHeader(
    target: BrowserTarget,
    onBack: () -> Unit,
    onPlayBrowser: () -> Unit = {},
    browserTrackCount: Int = 0,
    onRenamePlaylist: (BrowserTarget.Playlist) -> Unit = {},
    onDeletePlaylist: (BrowserTarget.Playlist) -> Unit = {},
) {
    LegacyPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("浏览结果", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (target) {
                        is BrowserTarget.Artist -> target.artist
                        is BrowserTarget.Album -> "${target.album} / ${target.artist}"
                        is BrowserTarget.Folder -> target.folder
                        is BrowserTarget.Playlist -> target.name
                    },
                    color = AccentWarm,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (target is BrowserTarget.Playlist) {
                    if (browserTrackCount > 0) {
                        TextButton(onClick = onPlayBrowser) {
                            Text("播放歌单")
                        }
                    }
                    IconButton(onClick = { onRenamePlaylist(target) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "重命名歌单", tint = Accent)
                    }
                    IconButton(onClick = { onDeletePlaylist(target) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除歌单", tint = AccentWarm)
                    }
                }
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: TrackEntity,
    currentTrack: TrackEntity?,
    onPlayTrack: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (TrackEntity) -> Unit,
    playlistIdForRemoval: Long? = null,
    onRemoveFromPlaylist: ((Long, TrackEntity) -> Unit)? = null,
) {
    val active = currentTrack?.contentUri == track.contentUri
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onPlayTrack(track) },
        colors = CardDefaults.cardColors(containerColor = if (active) PanelAlt else Panel),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${track.artist} | ${track.album}",
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.folder,
                    color = AccentWarm,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(track.durationMs.formatAsClock(), color = TextMuted)
            if (playlistIdForRemoval != null && onRemoveFromPlaylist != null) {
                IconButton(onClick = { onRemoveFromPlaylist(playlistIdForRemoval, track) }) {
                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "移出歌单", tint = Accent)
                }
            } else {
                IconButton(onClick = { onAddToPlaylist(track) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "加入歌单", tint = Accent)
                }
            }
            IconButton(onClick = { onToggleFavorite(track) }) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (track.isFavorite) AccentWarm else Color.White,
                )
            }
        }
    }
}

@Composable
private fun ArtistRow(
    artist: ArtistSummary,
    onOpenArtist: (String) -> Unit,
) {
    SummaryRow(
        title = artist.artist,
        subtitle = "${artist.trackCount} 首歌",
        onClick = { onOpenArtist(artist.artist) },
    )
}

@Composable
private fun AlbumRow(
    album: AlbumSummary,
    onOpenAlbum: (String, String) -> Unit,
) {
    SummaryRow(
        title = album.album,
        subtitle = "${album.artist} | ${album.trackCount} 首歌",
        onClick = { onOpenAlbum(album.album, album.artist) },
    )
}

@Composable
private fun FolderRow(
    folder: FolderSummary,
    onOpenFolder: (String) -> Unit,
) {
    SummaryRow(
        title = folder.folder,
        subtitle = "${folder.trackCount} 首歌",
        onClick = { onOpenFolder(folder.folder) },
    )
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistSummary,
    onOpenPlaylist: (Long, String) -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpenPlaylist(playlist.id, playlist.name) },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("${playlist.trackCount} 首歌", color = TextMuted)
            }
            IconButton(onClick = onRenamePlaylist) {
                Icon(Icons.Rounded.Edit, contentDescription = "重命名歌单", tint = Accent)
            }
            IconButton(onClick = onDeletePlaylist) {
                Icon(Icons.Rounded.Delete, contentDescription = "删除歌单", tint = AccentWarm)
            }
        }
    }
}

@Composable
private fun SummaryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, color = TextMuted)
        }
    }
}

@Composable
private fun TrackQueueRow(
    track: TrackEntity,
    isCurrent: Boolean,
    onSelectQueueTrack: (TrackEntity) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) PanelAlt else Color.Transparent)
            .clickable { onSelectQueueTrack(track) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(track.durationMs.formatAsClock(), color = TextMuted)
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<PlaylistSummary>,
    track: TrackEntity,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onSelectPlaylist: (PlaylistSummary) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("把《${track.title}》加入歌单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (playlists.isEmpty()) {
                    Text("还没有歌单，可以先创建一个。", color = TextMuted)
                } else {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onSelectPlaylist(playlist) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(playlist.name)
                                Text("${playlist.trackCount} 首", color = TextMuted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNew) {
                Text("新建歌单")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建歌单") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("歌单名称") },
                placeholder = { Text("例如：英文歌 / 中文歌 / 睡前") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.text) },
                enabled = text.text.isNotBlank(),
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun RenamePlaylistDialog(
    playlist: PlaylistSummary,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(playlist.id) { mutableStateOf(TextFieldValue(playlist.name)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名歌单") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("歌单名称") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.text) },
                enabled = text.text.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlist: PlaylistSummary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除歌单") },
        text = {
            Text("确认删除《${playlist.name}》吗？这只会删除歌单和它的归类关系，不会删除手机里的音乐文件。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun SleepTimerDialog(
    remainingMs: Long?,
    onDismiss: () -> Unit,
    onPresetSelected: (Int) -> Unit,
    onCancelTimer: () -> Unit,
) {
    var customMinutes by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠模式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (remainingMs == null) {
                        "设定倒计时，到点后自动停止播放。"
                    } else {
                        "当前睡眠倒计时剩余 ${remainingMs.formatAsClock()}。"
                    },
                    color = TextMuted,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        FilterChip(
                            selected = false,
                            onClick = { onPresetSelected(minutes) },
                            label = { Text("${minutes}分钟") },
                        )
                    }
                }
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("自定义分钟数") },
                    placeholder = { Text("例如 20") },
                )
            }
        },
        confirmButton = {
            Row {
                if (remainingMs != null) {
                    TextButton(onClick = onCancelTimer) {
                        Text("关闭定时")
                    }
                }
                TextButton(
                    onClick = {
                        customMinutes.text.toIntOrNull()
                            ?.takeIf { it > 0 }
                            ?.let(onPresetSelected)
                    },
                    enabled = customMinutes.text.toIntOrNull()?.let { it > 0 } == true,
                ) {
                    Text("开始定时")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun LegacyPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun LegacySubPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelAlt)
            .padding(12.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SpectrumBars() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        listOf(16f, 24f, 12f, 30f, 20f, 14f).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(height.dp)
                    .background(
                        if (index % 2 == 0) Accent else AccentWarm,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
private fun rememberArtwork(source: String?): State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, key1 = source, key2 = context) {
        if (source == null) {
            value = null
            return@produceState
        }
        value = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(source))
                val bytes = retriever.embeddedPicture ?: return@runCatching null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }
}

@Composable
private fun rememberLyricsPreview(track: TrackEntity?): State<String?> {
    val context = LocalContext.current
    return produceState<String?>(initialValue = null, key1 = track?.lyricsUri, key2 = context) {
        value = track?.lyricsUri?.let { runCatching { loadLyricsPreview(context, it) }.getOrNull() }
    }
}

@Composable
private fun rememberLyricsBody(track: TrackEntity?): State<String?> {
    val context = LocalContext.current
    return produceState<String?>(initialValue = null, key1 = track?.lyricsUri, key2 = context) {
        value = track?.lyricsUri?.let { runCatching { loadLyricsBody(context, it) }.getOrNull() }
    }
}
