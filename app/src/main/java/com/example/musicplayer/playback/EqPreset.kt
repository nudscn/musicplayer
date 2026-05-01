package com.example.musicplayer.playback

enum class EqPreset(val label: String) {
    OFF("\u5173\u95ed"),
    POP("\u6d41\u884c"),
    CLASSICAL("\u53e4\u5178"),
}

enum class QueueMode(val label: String) {
    SEQUENTIAL("\u987a\u5e8f"),
    REPEAT_ALL("\u5217\u8868\u5faa\u73af"),
    REPEAT_ONE("\u5355\u66f2\u5faa\u73af"),
    SHUFFLE("\u968f\u673a"),
}
