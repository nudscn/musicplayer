package com.example.musicplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.musicplayer.MainActivity
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.util.loadArtworkBitmap

class PlaybackService : MediaSessionService() {
    private val playbackManager: PlaybackManager
        get() = (application as MusicPlayerApp).playbackManager

    private var playerNotificationManager: PlayerNotificationManager? = null
    private var cachedArtworkSource: String? = null
    private var cachedArtworkBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForegroundService()
        createPlayerNotificationManager()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return playbackManager.mediaSession
    }

    private fun startAsForegroundService() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Local Music Player")
            .setContentText("正在准备后台播放")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Local music playback controls"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun createPlayerNotificationManager() {
        val contentIntent = PendingIntent.getActivity(
            this,
            1002,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID,
        )
            .setMediaDescriptionAdapter(
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player): PendingIntent {
                        return contentIntent
                    }

                    override fun getCurrentContentText(player: Player): CharSequence {
                        return player.mediaMetadata.artist ?: "本地音乐"
                    }

                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        return player.mediaMetadata.title ?: "Local Music Player"
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback,
                    ): Bitmap? {
                        val track = playbackManager.snapshot.value.currentTrack
                        val artworkSource = track?.artworkSource ?: track?.contentUri
                        if (artworkSource == cachedArtworkSource && cachedArtworkBitmap != null) {
                            return cachedArtworkBitmap
                        }

                        val bitmap = loadArtworkBitmap(
                            context = this@PlaybackService,
                            source = artworkSource,
                            targetSize = 512,
                        )
                        cachedArtworkSource = artworkSource
                        cachedArtworkBitmap = bitmap
                        return bitmap
                    }

                    override fun getCurrentSubText(player: Player): CharSequence? {
                        return player.mediaMetadata.albumTitle
                    }
                },
            )
            .setNotificationListener(
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean,
                    ) {
                        if (ongoing) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(
                                    notificationId,
                                    notification,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                                )
                            } else {
                                startForeground(notificationId, notification)
                            }
                        } else {
                            stopForeground(STOP_FOREGROUND_DETACH)
                        }
                    }

                    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                },
            )
            .build()
            .apply {
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setUseNextActionInCompactView(true)
                setUsePreviousActionInCompactView(true)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setMediaSessionToken(playbackManager.mediaSession.platformToken)
                setPlayer(playbackManager.player)
            }
    }

    override fun onDestroy() {
        cachedArtworkBitmap = null
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "music_playback_lock_v2"
        private const val NOTIFICATION_ID = 1001
    }
}
