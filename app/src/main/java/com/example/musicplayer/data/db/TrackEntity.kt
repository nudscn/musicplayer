package com.example.musicplayer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["folder"]),
        Index(value = ["rootUri"]),
    ],
)
data class TrackEntity(
    @PrimaryKey val contentUri: String,
    val rootUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val folder: String,
    val displayName: String,
    val mimeType: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val artworkSource: String?,
    val lyricsUri: String?,
    val isFavorite: Boolean,
    val lastPlayedAt: Long?,
    val playCount: Int,
)
