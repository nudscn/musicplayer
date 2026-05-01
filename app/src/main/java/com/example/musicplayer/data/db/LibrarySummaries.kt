package com.example.musicplayer.data.db

data class ArtistSummary(
    val artist: String,
    val trackCount: Int,
)

data class AlbumSummary(
    val album: String,
    val artist: String,
    val trackCount: Int,
)

data class FolderSummary(
    val folder: String,
    val trackCount: Int,
)
