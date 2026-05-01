package com.example.musicplayer.util

import android.content.Context
import android.net.Uri

fun loadLyricsPreview(context: Context, lyricsUri: String): String? =
    loadLyricsLines(context, lyricsUri, maxLines = 4)
        ?.joinToString("\n")
        ?.takeIf { it.isNotBlank() }

fun loadLyricsBody(context: Context, lyricsUri: String): String? =
    loadLyricsLines(context, lyricsUri, maxLines = 120)
        ?.joinToString("\n")
        ?.takeIf { it.isNotBlank() }

private fun loadLyricsLines(
    context: Context,
    lyricsUri: String,
    maxLines: Int,
): List<String>? {
    return context.contentResolver.openInputStream(Uri.parse(lyricsUri))
        ?.bufferedReader()
        ?.use { reader ->
            reader.lineSequence()
                .map(::stripLyricsTimestamp)
                .filter { it.isNotBlank() }
                .take(maxLines)
                .toList()
                .takeIf { it.isNotEmpty() }
        }
}

private fun stripLyricsTimestamp(line: String): String =
    line.replace(Regex("""\[[^\]]+\]"""), "").trim()
