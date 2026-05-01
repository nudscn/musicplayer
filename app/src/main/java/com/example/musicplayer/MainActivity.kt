package com.example.musicplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.ui.MainViewModel
import com.example.musicplayer.ui.MainViewModelFactory
import com.example.musicplayer.ui.MusicPlayerScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application)
    }

    private val openDocumentTree = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val takeFlags = IntentFlags.readWriteFlags
            contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
            viewModel.addMusicDirectory(selectedUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MusicPlayerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MusicPlayerScreen(
                    state = uiState,
                    onAddDirectory = { openDocumentTree.launch(null) },
                    onRescan = viewModel::rescanLibrary,
                    onSelectTab = viewModel::selectTab,
                    onPlayTrack = viewModel::playTrack,
                    onPlayCurrentBrowser = viewModel::playCurrentBrowser,
                    onOpenArtist = viewModel::openArtist,
                    onOpenAlbum = viewModel::openAlbum,
                    onOpenFolder = viewModel::openFolder,
                    onOpenPlaylist = viewModel::openPlaylist,
                    onBackFromBrowser = viewModel::clearBrowser,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist,
                    onStartSleepTimer = viewModel::startSleepTimer,
                    onCancelSleepTimer = viewModel::cancelSleepTimer,
                    onTogglePlayback = viewModel::togglePlayPause,
                    onNext = viewModel::next,
                    onPrevious = viewModel::previous,
                    onSeek = viewModel::seekTo,
                    onCyclePlaybackMode = viewModel::cyclePlaybackMode,
                    onSelectEqPreset = viewModel::selectEqPreset,
                    onSelectQueueTrack = viewModel::playFromQueue,
                )
            }
        }
    }
}

private object IntentFlags {
    const val readWriteFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}
