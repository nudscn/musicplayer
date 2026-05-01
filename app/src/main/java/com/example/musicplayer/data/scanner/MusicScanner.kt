package com.example.musicplayer.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile
import com.example.musicplayer.data.db.TrackEntity
import java.util.Locale

class MusicScanner(
    private val context: Context,
) {
    fun scanTree(
        rootDocument: DocumentFile,
        rootUri: String,
        previousTracks: Map<String, TrackEntity>,
    ): List<TrackEntity> {
        val tracks = mutableListOf<TrackEntity>()
        walkDirectory(
            directory = rootDocument,
            rootUri = rootUri,
            relativePath = rootDocument.name ?: "Music",
            previousTracks = previousTracks,
            output = tracks,
        )
        return tracks
    }

    private fun walkDirectory(
        directory: DocumentFile,
        rootUri: String,
        relativePath: String,
        previousTracks: Map<String, TrackEntity>,
        output: MutableList<TrackEntity>,
    ) {
        val children = directory.listFiles().toList()
        val lyricLookup = children
            .filter { it.isFile && it.name?.substringAfterLast('.', "")?.lowercase(Locale.US) == "lrc" }
            .associateBy { it.name?.substringBeforeLast('.')?.lowercase(Locale.US).orEmpty() }

        children.filter { it.isFile && it.isAudioFile() }
            .sortedBy { it.name?.lowercase(Locale.US) }
            .forEach { file ->
                val stem = file.name?.substringBeforeLast('.')?.lowercase(Locale.US).orEmpty()
                val previous = previousTracks[file.uri.toString()]
                output += file.toTrackEntity(
                    rootUri = rootUri,
                    folder = relativePath,
                    lyricsUri = lyricLookup[stem]?.uri?.toString(),
                    previous = previous,
                )
            }

        children.filter { it.isDirectory }
            .sortedBy { it.name?.lowercase(Locale.US) }
            .forEach { child ->
                val nextPath = listOf(relativePath, child.name ?: "Folder").joinToString(" / ")
                walkDirectory(child, rootUri, nextPath, previousTracks, output)
            }
    }

    private fun DocumentFile.toTrackEntity(
        rootUri: String,
        folder: String,
        lyricsUri: String?,
        previous: TrackEntity?,
    ): TrackEntity {
        val uriString = uri.toString()
        val metadata = readMetadata(uriString)
        return TrackEntity(
            contentUri = uriString,
            rootUri = rootUri,
            title = metadata.title ?: name?.substringBeforeLast('.') ?: "Unknown Title",
            artist = metadata.artist ?: "Unknown Artist",
            album = metadata.album ?: "Unknown Album",
            folder = folder,
            displayName = name ?: "Unknown File",
            mimeType = type ?: "audio/*",
            durationMs = metadata.durationMs ?: 0L,
            trackNumber = metadata.trackNumber ?: 0,
            discNumber = metadata.discNumber ?: 0,
            sizeBytes = length(),
            modifiedTime = lastModified(),
            artworkSource = if (metadata.hasArtwork) uriString else null,
            lyricsUri = lyricsUri,
            isFavorite = previous?.isFavorite ?: false,
            lastPlayedAt = previous?.lastPlayedAt,
            playCount = previous?.playCount ?: 0,
        )
    }

    private fun DocumentFile.isAudioFile(): Boolean {
        val extension = name?.substringAfterLast('.', "")?.lowercase(Locale.US)
        return extension in supportedExtensions
    }

    private fun readMetadata(uriString: String): TrackMetadata {
        val uri = android.net.Uri.parse(uriString)
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val picture = retriever.embeddedPicture
                TrackMetadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                    trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                        ?.substringBefore('/')
                        ?.toIntOrNull(),
                    discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                        ?.toIntOrNull(),
                    hasArtwork = picture != null,
                )
            } finally {
                retriever.release()
            }
        }.getOrElse {
            TrackMetadata()
        }
    }

    companion object {
        private val supportedExtensions = setOf(
            "mp3",
            "flac",
            "aac",
            "m4a",
            "ogg",
            "wav",
            "opus",
        )
    }
}

data class TrackMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val hasArtwork: Boolean = false,
)
