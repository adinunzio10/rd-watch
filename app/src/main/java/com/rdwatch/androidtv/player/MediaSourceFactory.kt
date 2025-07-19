package com.rdwatch.androidtv.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class MediaSourceFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val dataSourceFactory =
            DefaultDataSource.Factory(
                context,
                DefaultHttpDataSource.Factory()
                    .setUserAgent("RD-Watch/1.0")
                    .setConnectTimeoutMs(30_000)
                    .setReadTimeoutMs(30_000)
                    .setAllowCrossProtocolRedirects(true)
                    .setDefaultRequestProperties(mapOf("Connection" to "keep-alive"))
                    .apply {
                        android.util.Log.d("MediaSourceFactory", "HTTP DataSource configured with cross-protocol redirects enabled")
                    },
            )

        private val extractorsFactory = DefaultExtractorsFactory()

        fun createMediaSource(mediaItem: MediaItem): MediaSource {
            val uri = mediaItem.localConfiguration?.uri
            android.util.Log.d("MediaSourceFactory", "createMediaSource called with URI: $uri")

            if (uri == null) {
                android.util.Log.w("MediaSourceFactory", "URI is null, creating progressive media source")
                return createProgressiveMediaSource(mediaItem)
            }

            val mediaType = detectMediaType(uri)
            android.util.Log.d("MediaSourceFactory", "Detected media type: $mediaType for URI: $uri")

            return when (mediaType) {
                MediaType.DASH -> {
                    android.util.Log.d("MediaSourceFactory", "Creating DASH media source")
                    createDashMediaSource(mediaItem)
                }
                MediaType.HLS -> {
                    android.util.Log.d("MediaSourceFactory", "Creating HLS media source")
                    createHlsMediaSource(mediaItem)
                }
                MediaType.SMOOTH_STREAMING -> {
                    android.util.Log.d("MediaSourceFactory", "Creating Smooth Streaming media source")
                    createSmoothStreamingMediaSource(mediaItem)
                }
                MediaType.PROGRESSIVE -> {
                    android.util.Log.d("MediaSourceFactory", "Creating Progressive media source")
                    createProgressiveMediaSource(mediaItem)
                }
            }
        }

        private fun createDashMediaSource(mediaItem: MediaItem): MediaSource {
            return DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        private fun createHlsMediaSource(mediaItem: MediaItem): MediaSource {
            return HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        private fun createSmoothStreamingMediaSource(mediaItem: MediaItem): MediaSource {
            return SsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        private fun createProgressiveMediaSource(mediaItem: MediaItem): MediaSource {
            return ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                .createMediaSource(mediaItem)
        }

        private fun detectMediaType(uri: Uri): MediaType {
            val uriString = uri.toString().lowercase()
            val path = uri.path?.lowercase() ?: ""

            return when {
                // DASH detection
                uriString.contains(".mpd") ||
                    uriString.contains("dash") ||
                    path.endsWith(".mpd") -> MediaType.DASH

                // HLS detection
                uriString.contains(".m3u8") ||
                    uriString.contains("hls") ||
                    path.endsWith(".m3u8") -> MediaType.HLS

                // Smooth Streaming detection
                uriString.contains("isml") ||
                    uriString.contains("smoothstreaming") ||
                    path.contains("isml") -> MediaType.SMOOTH_STREAMING

                // Progressive formats
                else -> MediaType.PROGRESSIVE
            }
        }

        fun getSupportedFormats(): List<String> {
            return listOf(
                // Progressive formats
                "mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "3gp",
                // Audio formats
                "mp3", "aac", "m4a", "ogg", "flac", "wav",
                // Streaming formats
                "m3u8", "mpd", "isml",
            )
        }

        fun isFormatSupported(url: String): Boolean {
            val uri = Uri.parse(url)
            val path = uri.path?.lowercase() ?: ""
            val extension = path.substringAfterLast(".", "")

            return getSupportedFormats().contains(extension) ||
                detectMediaType(uri) != MediaType.PROGRESSIVE // Streaming formats always supported
        }

        private enum class MediaType {
            DASH,
            HLS,
            SMOOTH_STREAMING,
            PROGRESSIVE,
        }
    }

data class MediaMetadata(
    val title: String? = null,
    val description: String? = null,
    val duration: Long? = null,
    val thumbnailUrl: String? = null,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
)

data class SubtitleTrack(
    val language: String,
    val label: String,
    val url: String,
    val mimeType: String,
)

data class AudioTrack(
    val language: String,
    val label: String,
    val channels: Int = 2,
    val sampleRate: Int = 44100,
)
