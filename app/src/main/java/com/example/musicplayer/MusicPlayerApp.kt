package com.example.musicplayer

import android.app.Application
import com.example.musicplayer.data.db.AppDatabase
import com.example.musicplayer.data.repository.MusicRepository
import com.example.musicplayer.playback.PlaybackManager

class MusicPlayerApp : Application() {
    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { MusicRepository(this, database.libraryDao()) }
    val playbackManager by lazy { PlaybackManager(this, repository) }
}
