package com.example.musicplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "root_directories")
data class RootDirectoryEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val addedAt: Long,
    val lastScannedAt: Long?,
)
