package com.example.musicplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.musicplayer.MainActivity
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.R
import com.example.musicplayer.playback.PlaybackSnapshot
import com.example.musicplayer.playback.QueueMode
import com.example.musicplayer.util.loadArtworkBitmap

class PlaybackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val playbackManager = (context.applicationContext as MusicPlayerApp).playbackManager
        when (intent.action) {
            ACTION_PREVIOUS -> playbackManager.previous()
            ACTION_TOGGLE -> playbackManager.togglePlayPause()
            ACTION_NEXT -> playbackManager.next()
            ACTION_MODE -> playbackManager.cycleWidgetQueueMode()
            ACTION_REFRESH,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null -> Unit
        }
        updateAll(context)
    }

    companion object {
        const val ACTION_PREVIOUS = "com.example.musicplayer.widget.PREVIOUS"
        const val ACTION_TOGGLE = "com.example.musicplayer.widget.TOGGLE"
        const val ACTION_NEXT = "com.example.musicplayer.widget.NEXT"
        const val ACTION_MODE = "com.example.musicplayer.widget.MODE"
        const val ACTION_REFRESH = "com.example.musicplayer.widget.REFRESH"

        fun updateAll(
            context: Context,
            snapshot: PlaybackSnapshot = (context.applicationContext as MusicPlayerApp).playbackManager.snapshot.value,
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PlaybackWidgetProvider::class.java)
            val widgetIds = manager.getAppWidgetIds(component)
            if (widgetIds.isEmpty()) return

            widgetIds.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildRemoteViews(context, snapshot))
            }
        }

        private fun buildRemoteViews(
            context: Context,
            snapshot: PlaybackSnapshot,
        ): RemoteViews {
            val track = snapshot.currentTrack
            val views = RemoteViews(context.packageName, R.layout.widget_playback)

            views.setTextViewText(R.id.widget_title, track?.title ?: "Local Music Player")
            views.setTextViewText(R.id.widget_subtitle, track?.artist ?: "Ready to play")
            views.setTextViewText(R.id.widget_mode, widgetModeLabel(context, snapshot.queueMode))
            views.setImageViewResource(
                R.id.widget_toggle,
                if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )

            val progressMax = (snapshot.durationMs.coerceAtLeast(1L) / 1000L).toInt().coerceAtLeast(1)
            val progressValue = (snapshot.currentPositionMs.coerceAtLeast(0L) / 1000L).toInt().coerceIn(0, progressMax)
            views.setProgressBar(R.id.widget_progress, progressMax, progressValue, false)

            val artwork = loadArtworkBitmap(context, track?.artworkSource ?: track?.contentUri, 900)
            if (artwork != null) {
                views.setImageViewBitmap(R.id.widget_artwork, artwork)
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_album_placeholder)
            }

            views.setOnClickPendingIntent(
                R.id.widget_previous,
                broadcastPendingIntent(context, ACTION_PREVIOUS, 201),
            )
            views.setOnClickPendingIntent(
                R.id.widget_toggle,
                broadcastPendingIntent(context, ACTION_TOGGLE, 202),
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                broadcastPendingIntent(context, ACTION_NEXT, 203),
            )
            views.setOnClickPendingIntent(
                R.id.widget_mode,
                broadcastPendingIntent(context, ACTION_MODE, 204),
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                activityPendingIntent(context),
            )

            return views
        }

        private fun broadcastPendingIntent(
            context: Context,
            action: String,
            requestCode: Int,
        ): PendingIntent {
            val intent = Intent(context, PlaybackWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun activityPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                205,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun widgetModeLabel(context: Context, mode: QueueMode): String =
            when (mode) {
                QueueMode.SEQUENTIAL -> context.getString(R.string.widget_mode_sequential)
                QueueMode.REPEAT_ALL,
                QueueMode.REPEAT_ONE -> context.getString(R.string.widget_mode_list)
                QueueMode.SHUFFLE -> context.getString(R.string.widget_mode_shuffle)
            }
    }
}
