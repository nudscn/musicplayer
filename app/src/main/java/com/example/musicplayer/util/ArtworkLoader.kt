package com.example.musicplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

fun loadArtworkBitmap(
    context: Context,
    source: String?,
    targetSize: Int,
): Bitmap? {
    if (source.isNullOrBlank()) return null

    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(source))
            val bytes = retriever.embeddedPicture ?: return@runCatching null
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
            Bitmap.createScaledBitmap(original, targetSize, targetSize, true)
        } finally {
            retriever.release()
        }
    }.getOrNull()
}
