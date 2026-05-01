package com.example.musicplayer.util

fun Long.formatAsClock(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
