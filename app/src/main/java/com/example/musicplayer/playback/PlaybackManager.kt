package com.example.musicplayer.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.data.repository.MusicRepository
import com.example.musicplayer.widget.PlaybackWidgetProvider
import org.json.JSONArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PlaybackSnapshot(
    val currentTrack: TrackEntity? = null,
    val queue: List<TrackEntity> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val sleepTimerRemainingMs: Long? = null,
    val queueMode: QueueMode = QueueMode.SEQUENTIAL,
    val eqPreset: EqPreset = EqPreset.OFF,
    val errorMessage: String? = null,
)

class PlaybackManager(
    context: Context,
    private val repository: MusicRepository,
) {
    private companion object {
        const val PREF_QUEUE_MODE = "queue_mode"
        const val PREF_EQ_PRESET = "eq_preset"
        const val PREF_LAST_QUEUE = "last_queue"
        const val PREF_LAST_TRACK_URI = "last_track_uri"
        const val PREF_LAST_INDEX = "last_index"
        const val PREF_LAST_POSITION_MS = "last_position_ms"
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = appContext.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

    internal val player = ExoPlayer.Builder(appContext).build().apply {
        setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true,
        )
        repeatMode = Player.REPEAT_MODE_OFF
    }

    val mediaSession: MediaSession = MediaSession.Builder(appContext, player).build()

    private val _snapshot = MutableStateFlow(
        PlaybackSnapshot(
            queueMode = loadQueueMode(),
            eqPreset = loadEqPreset(),
        ),
    )
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var lastWidgetProgressSecond: Long = -1L
    private var sleepTimerJob: Job? = null

    init {
        applyQueueMode(_snapshot.value.queueMode)
        restorePlaybackState()
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    pushSnapshot(isPlaying = isPlaying)
                    persistPlaybackState()
                    notifyExternalSurfaces()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val track = currentTrack()
                    pushSnapshot(currentTrack = track)
                    persistPlaybackState()
                    track?.let { current ->
                        scope.launch(Dispatchers.IO) {
                            repository.markPlayed(current.contentUri)
                        }
                    }
                    attachEqualizerIfPossible()
                    notifyExternalSurfaces()
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    pushSnapshot(
                        currentTrack = currentTrack(),
                        isPlaying = player.isPlaying,
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    pushSnapshot(errorMessage = error.message)
                    notifyExternalSurfaces()
                }
            },
        )

        scope.launch {
            while (true) {
                val currentPosition = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration.coerceAtLeast(0L)
                val isPlaying = player.isPlaying
                pushSnapshot(
                    currentTrack = currentTrack(),
                    currentPositionMs = currentPosition,
                    durationMs = duration,
                    isPlaying = isPlaying,
                )
                if (isPlaying) {
                    val currentSecond = currentPosition / 1000L
                    if (currentSecond != lastWidgetProgressSecond) {
                        lastWidgetProgressSecond = currentSecond
                        persistPlaybackState(currentPosition)
                        notifyExternalSurfaces()
                    }
                }
                delay(500)
            }
        }
    }

    fun setQueue(tracks: List<TrackEntity>, startIndex: Int, playWhenReady: Boolean) {
        if (tracks.isEmpty()) return
        if (playWhenReady) {
            ensurePlaybackService()
        }
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.contentUri)
                .setUri(Uri.parse(track.contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build(),
                )
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.playWhenReady = playWhenReady
        pushSnapshot(
            queue = tracks,
            currentTrack = tracks.getOrNull(startIndex),
            isPlaying = playWhenReady,
        )
        persistPlaybackState(0L)
        attachEqualizerIfPossible()
        notifyExternalSurfaces()
    }

    fun playTrackFromQueue(index: Int) {
        if (index !in _snapshot.value.queue.indices) return
        ensurePlaybackService()
        player.seekTo(index, 0L)
        player.playWhenReady = true
        pushSnapshot(currentTrack = _snapshot.value.queue.getOrNull(index), isPlaying = true)
        persistPlaybackState(0L)
        notifyExternalSurfaces()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            ensurePlaybackService()
            player.play()
        }
        pushSnapshot(isPlaying = player.isPlaying)
        persistPlaybackState()
        notifyExternalSurfaces()
    }

    fun next() {
        player.seekToNextMediaItem()
        persistPlaybackState()
    }

    fun previous() {
        player.seekToPreviousMediaItem()
        persistPlaybackState()
    }

    fun seekTo(fraction: Float) {
        val duration = player.duration.takeIf { it > 0L } ?: return
        val target = (duration * fraction).toLong()
        player.seekTo(target)
        pushSnapshot(
            currentPositionMs = target,
            durationMs = duration,
            isPlaying = player.isPlaying,
        )
        persistPlaybackState(target)
        notifyExternalSurfaces()
    }

    fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it > 0L } ?: 0L
        val target = (player.currentPosition + deltaMs).coerceIn(0L, duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
        player.seekTo(target)
        pushSnapshot(
            currentPositionMs = target,
            durationMs = player.duration.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
        )
        persistPlaybackState(target)
        notifyExternalSurfaces()
    }

    fun cycleQueueMode() {
        val next = when (_snapshot.value.queueMode) {
            QueueMode.SEQUENTIAL -> QueueMode.REPEAT_ALL
            QueueMode.REPEAT_ALL -> QueueMode.REPEAT_ONE
            QueueMode.REPEAT_ONE -> QueueMode.SHUFFLE
            QueueMode.SHUFFLE -> QueueMode.SEQUENTIAL
        }
        applyQueueMode(next)
        pushSnapshot(queueMode = next)
        preferences.edit { putString(PREF_QUEUE_MODE, next.name) }
        persistPlaybackState()
        notifyExternalSurfaces()
    }

    fun cycleWidgetQueueMode() {
        val next = when (_snapshot.value.queueMode) {
            QueueMode.SEQUENTIAL -> QueueMode.REPEAT_ALL
            QueueMode.REPEAT_ALL,
            QueueMode.REPEAT_ONE -> QueueMode.SHUFFLE
            QueueMode.SHUFFLE -> QueueMode.SEQUENTIAL
        }
        applyQueueMode(next)
        pushSnapshot(queueMode = next)
        preferences.edit { putString(PREF_QUEUE_MODE, next.name) }
        persistPlaybackState()
        notifyExternalSurfaces()
    }

    fun setEqPreset(preset: EqPreset) {
        preferences.edit { putString(PREF_EQ_PRESET, preset.name) }
        _snapshot.value = _snapshot.value.copy(eqPreset = preset)
        attachEqualizerIfPossible()
    }

    fun startSleepTimer(durationMinutes: Int) {
        val durationMs = durationMinutes.coerceAtLeast(1).toLong() * 60_000L
        scheduleSleepTimer(durationMs)
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pushSnapshot(sleepTimerRemainingMs = null)
    }

    fun release() {
        sleepTimerJob?.cancel()
        equalizer?.release()
        mediaSession.release()
        player.release()
    }

    private fun currentTrack(): TrackEntity? {
        val mediaId = player.currentMediaItem?.mediaId ?: return null
        return _snapshot.value.queue.firstOrNull { it.contentUri == mediaId }
    }

    private fun pushSnapshot(
        currentTrack: TrackEntity? = _snapshot.value.currentTrack,
        queue: List<TrackEntity> = _snapshot.value.queue,
        isPlaying: Boolean = _snapshot.value.isPlaying,
        currentPositionMs: Long = _snapshot.value.currentPositionMs,
        durationMs: Long = _snapshot.value.durationMs,
        sleepTimerRemainingMs: Long? = _snapshot.value.sleepTimerRemainingMs,
        queueMode: QueueMode = _snapshot.value.queueMode,
        eqPreset: EqPreset = _snapshot.value.eqPreset,
        errorMessage: String? = null,
    ) {
        _snapshot.value = PlaybackSnapshot(
            currentTrack = currentTrack,
            queue = queue,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            queueMode = queueMode,
            eqPreset = eqPreset,
            errorMessage = errorMessage,
        )
    }

    private fun notifyExternalSurfaces() {
        PlaybackWidgetProvider.updateAll(appContext, _snapshot.value)
    }

    private fun ensurePlaybackService() {
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, PlaybackService::class.java),
        )
    }

    private fun applyQueueMode(mode: QueueMode) {
        when (mode) {
            QueueMode.SEQUENTIAL -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            QueueMode.REPEAT_ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            QueueMode.REPEAT_ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            QueueMode.SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }
        }
    }

    private fun attachEqualizerIfPossible() {
        val audioSessionId = player.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        if (equalizer != null && equalizerSessionId == audioSessionId) {
            applyEqualizerCurve(_snapshot.value.eqPreset)
            return
        }

        equalizer?.release()
        equalizer = runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull()
        equalizerSessionId = audioSessionId

        applyEqualizerCurve(_snapshot.value.eqPreset)
    }

    private fun applyEqualizerCurve(preset: EqPreset) {
        val eq = equalizer ?: return
        if (preset == EqPreset.OFF) {
            eq.enabled = false
            return
        }

        eq.enabled = true
        val baseCurve = when (preset) {
            EqPreset.OFF -> listOf(0f, 0f, 0f, 0f, 0f)
            EqPreset.POP -> listOf(0.35f, 0.15f, -0.1f, 0.15f, 0.35f)
            EqPreset.CLASSICAL -> listOf(0.2f, 0.1f, -0.15f, 0.2f, 0.3f)
        }
        val range = eq.bandLevelRange
        val minLevel = range[0]
        val maxLevel = range[1]
        val bandCount = eq.numberOfBands.toInt()
        repeat(bandCount) { bandIndex ->
            val curvePosition = if (bandCount == 1) 0f else bandIndex.toFloat() / (bandCount - 1).toFloat()
            val scaled = interpolateCurve(baseCurve, curvePosition)
            val level = (scaled * maxLevel).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
            eq.setBandLevel(bandIndex.toShort(), level)
        }
    }

    private fun interpolateCurve(curve: List<Float>, position: Float): Float {
        if (curve.size == 1) return curve.first()
        val scaled = position * (curve.lastIndex)
        val lowerIndex = scaled.toInt()
        val upperIndex = (lowerIndex + 1).coerceAtMost(curve.lastIndex)
        val fraction = scaled - lowerIndex
        val lower = curve[lowerIndex]
        val upper = curve[upperIndex]
        return lower + (upper - lower) * fraction
    }

    private fun scheduleSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        val endRealtime = SystemClock.elapsedRealtime() + durationMs
        pushSnapshot(sleepTimerRemainingMs = durationMs)
        sleepTimerJob = scope.launch {
            while (true) {
                val remainingMs = (endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                if (remainingMs <= 0L) {
                    player.pause()
                    pushSnapshot(
                        isPlaying = false,
                        sleepTimerRemainingMs = null,
                    )
                    notifyExternalSurfaces()
                    sleepTimerJob = null
                    break
                }
                pushSnapshot(sleepTimerRemainingMs = remainingMs)
                delay(1_000L)
            }
        }
    }

    private fun restorePlaybackState() {
        val queueUris = loadSavedQueueUris()
        val currentTrackUri = preferences.getString(PREF_LAST_TRACK_URI, null)
        val savedIndex = preferences.getInt(PREF_LAST_INDEX, -1)
        val savedPositionMs = preferences.getLong(PREF_LAST_POSITION_MS, 0L).coerceAtLeast(0L)

        if (queueUris.isEmpty() && currentTrackUri.isNullOrBlank()) {
            return
        }

        scope.launch(Dispatchers.IO) {
            val restoredQueue = repository.getTracksByContentUris(queueUris).toMutableList()
            if (restoredQueue.isEmpty() && !currentTrackUri.isNullOrBlank()) {
                repository.getTrackByContentUri(currentTrackUri)?.let(restoredQueue::add)
            } else if (!currentTrackUri.isNullOrBlank() && restoredQueue.none { it.contentUri == currentTrackUri }) {
                repository.getTrackByContentUri(currentTrackUri)?.let(restoredQueue::add)
            }

            if (restoredQueue.isEmpty()) return@launch

            val resolvedIndex = when {
                !currentTrackUri.isNullOrBlank() -> restoredQueue.indexOfFirst { it.contentUri == currentTrackUri }
                else -> savedIndex
            }.takeIf { it in restoredQueue.indices } ?: 0

            val startTrack = restoredQueue[resolvedIndex]
            val startPositionMs = savedPositionMs.coerceIn(0L, startTrack.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE)
            val mediaItems = restoredQueue.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.contentUri)
                    .setUri(Uri.parse(track.contentUri))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .build(),
                    )
                    .build()
            }

            launch(Dispatchers.Main.immediate) {
                player.setMediaItems(mediaItems, resolvedIndex, startPositionMs)
                player.prepare()
                player.playWhenReady = false
                pushSnapshot(
                    queue = restoredQueue,
                    currentTrack = startTrack,
                    currentPositionMs = startPositionMs,
                    durationMs = startTrack.durationMs,
                    isPlaying = false,
                )
                attachEqualizerIfPossible()
                notifyExternalSurfaces()
            }
        }
    }

    private fun persistPlaybackState(currentPositionOverrideMs: Long? = null) {
        val state = _snapshot.value
        val queue = state.queue
        val currentTrack = state.currentTrack ?: currentTrack()
        if (queue.isEmpty() && currentTrack == null) return

        val currentIndex = queue.indexOfFirst { it.contentUri == currentTrack?.contentUri }
            .takeIf { it >= 0 }
            ?: player.currentMediaItemIndex.takeIf { it >= 0 }
            ?: 0
        val positionMs = currentPositionOverrideMs ?: player.currentPosition.coerceAtLeast(0L)
        val queueJson = JSONArray(queue.map { it.contentUri }).toString()

        preferences.edit {
            putString(PREF_LAST_QUEUE, queueJson)
            putString(PREF_LAST_TRACK_URI, currentTrack?.contentUri ?: queue.getOrNull(currentIndex)?.contentUri)
            putInt(PREF_LAST_INDEX, currentIndex)
            putLong(PREF_LAST_POSITION_MS, positionMs)
        }
    }

    private fun loadSavedQueueUris(): List<String> =
        runCatching {
            val raw = preferences.getString(PREF_LAST_QUEUE, null).orEmpty()
            if (raw.isBlank()) return emptyList()
            val json = JSONArray(raw)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    json.optString(index)?.takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptyList())

    private fun loadQueueMode(): QueueMode =
        preferences.getString(PREF_QUEUE_MODE, QueueMode.SEQUENTIAL.name)
            ?.let { runCatching { QueueMode.valueOf(it) }.getOrNull() }
            ?: QueueMode.SEQUENTIAL

    private fun loadEqPreset(): EqPreset =
        preferences.getString(PREF_EQ_PRESET, EqPreset.OFF.name)
            ?.let { runCatching { EqPreset.valueOf(it) }.getOrNull() }
            ?: EqPreset.OFF
}
