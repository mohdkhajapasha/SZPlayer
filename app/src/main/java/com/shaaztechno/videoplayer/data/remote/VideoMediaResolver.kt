package com.shaaztechno.videoplayer.data.remote

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo
import java.util.Locale

class VideoMediaResolver(
    private val context: Context
) {

    fun resolveMediaInfo(
        originalUrl: String,
        checkResponse: UrlCheckResponse
    ): Result<VideoMediaInfo> {
        val effectiveUrl = checkResponse.effectiveUrl
        val contentType = checkResponse.contentType?.lowercase(Locale.ROOT)
        val initialBytes = checkResponse.initialBytes

        // 1. Detect format & MIME type
        val (mediaType, mimeType) = detectMediaType(effectiveUrl, contentType, initialBytes)

        if (mediaType == null) {
            return Result.failure(
                IllegalArgumentException("This media format is not supported or not recognized as video.")
            )
        }

        // 2. Derive title from URL
        val title = deriveTitle(originalUrl, effectiveUrl, mediaType)

        // 3. Verify Media3 can create a media source
        val uri = Uri.parse(effectiveUrl)
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }
        val mediaItem = mediaItemBuilder.build()

        val isPlayable = try {
            val factory = DefaultMediaSourceFactory(context)
            factory.createMediaSource(mediaItem)
            true
        } catch (e: Exception) {
            false
        }

        if (!isPlayable) {
            return Result.failure(
                IllegalArgumentException("SZ Player could not initialize playback for this media stream.")
            )
        }

        // 4. Try to determine duration (for progressive files)
        val duration = if (mediaType.startsWith("HLS", false) || mediaType.startsWith("DASH", false)) {
            null
        } else {
            fetchDuration(effectiveUrl)
        }

        // 5. Download capability
        // Media3 supports progressive, HLS, and DASH
        val isDownloadable = checkResponse.isAccessible

        return Result.success(
            VideoMediaInfo(
                url = effectiveUrl,
                title = title,
                mediaType = mediaType,
                mimeType = mimeType,
                fileSize = checkResponse.contentLength,
                duration = duration,
                isPlayable = true,
                isDownloadable = isDownloadable
            )
        )
    }

    private fun detectMediaType(
        url: String,
        contentType: String?,
        initialBytes: ByteArray?
    ): Pair<String?, String?> {
        val lowerUrl = url.lowercase(Locale.ROOT).substringBefore('?')

        // Check Content-Type header first
        if (contentType != null) {
            when {
                contentType.contains("video/mp4") || contentType.contains("application/mp4") ->
                    return "MP4" to MimeTypes.VIDEO_MP4
                contentType.contains("video/webm") ->
                    return "WebM" to MimeTypes.VIDEO_WEBM
                contentType.contains("video/x-matroska") || contentType.contains("video/mkv") ->
                    return "MKV" to MimeTypes.VIDEO_MATROSKA
                contentType.contains("application/x-mpegurl") ||
                contentType.contains("application/vnd.apple.mpegurl") ||
                contentType.contains("audio/mpegurl") ->
                    return "HLS Stream" to MimeTypes.APPLICATION_M3U8
                contentType.contains("application/dash+xml") ->
                    return "DASH Stream" to MimeTypes.APPLICATION_MPD
                contentType.contains("video/quicktime") ->
                    return "QuickTime MOV" to MimeTypes.VIDEO_MP4
                contentType.contains("video/3gpp") ->
                    return "3GP" to MimeTypes.VIDEO_H263
                contentType.startsWith("video/") ->
                    return "Video" to null
            }
        }

        // Check magic bytes (e.g. when content-type is octet-stream or generic)
        if (initialBytes != null && initialBytes.isNotEmpty()) {
            val textPrefix = try {
                String(initialBytes.take(256).toByteArray(), Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }

            if (textPrefix.startsWith("#EXTM3U", false)) {
                return "HLS Stream" to MimeTypes.APPLICATION_M3U8
            }
            if (textPrefix.contains("<MPD") || textPrefix.contains("urn:mpeg:dash")) {
                return "DASH Stream" to MimeTypes.APPLICATION_MPD
            }

            // Check MP4 ftyp box (bytes 4..7: "ftyp")
            if (initialBytes.size >= 8) {
                val boxType = String(initialBytes.sliceArray(4..7), Charsets.US_ASCII)
                if (boxType.lowercase(Locale.ROOT) == "ftyp") {
                    return "MP4" to MimeTypes.VIDEO_MP4
                }
            }

            // Check WebM / Matroska EBML ID: 0x1A 0x45 0xDF 0xA3
            if (initialBytes.size >= 4 &&
                initialBytes[0] == 0x1A.toByte() &&
                initialBytes[1] == 0x45.toByte() &&
                initialBytes[2] == 0xDF.toByte() &&
                initialBytes[3] == 0xA3.toByte()
            ) {
                return "WebM" to MimeTypes.VIDEO_WEBM
            }
        }

        // Check URL path extension
        when {
            lowerUrl.endsWith(".mp4") -> return "MP4" to MimeTypes.VIDEO_MP4
            lowerUrl.endsWith(".m3u8") -> return "HLS Stream" to MimeTypes.APPLICATION_M3U8
            lowerUrl.endsWith(".mpd") -> return "DASH Stream" to MimeTypes.APPLICATION_MPD
            lowerUrl.endsWith(".webm") -> return "WebM" to MimeTypes.VIDEO_WEBM
            lowerUrl.endsWith(".mkv") -> return "MKV" to MimeTypes.VIDEO_MATROSKA
            lowerUrl.endsWith(".mov") -> return "QuickTime MOV" to MimeTypes.VIDEO_MP4
            lowerUrl.endsWith(".3gp") -> return "3GP" to MimeTypes.VIDEO_H263
            lowerUrl.endsWith(".ts") -> return "MPEG-TS" to MimeTypes.VIDEO_MP2T
        }

        return null to null
    }

    private fun deriveTitle(originalUrl: String, effectiveUrl: String, mediaType: String): String {
        val uri = Uri.parse(effectiveUrl)
        val origUri = Uri.parse(originalUrl)
        val lastPathSegment = uri.lastPathSegment ?: origUri.lastPathSegment

        if (!lastPathSegment.isNullOrBlank() &&
            lastPathSegment.length > 2 &&
            !lastPathSegment.contains("?") &&
            (lastPathSegment.endsWith(".mp4", ignoreCase = true) ||
             lastPathSegment.endsWith(".m3u8", ignoreCase = true) ||
             lastPathSegment.endsWith(".mpd", ignoreCase = true) ||
             lastPathSegment.endsWith(".webm", ignoreCase = true))
        ) {
            return lastPathSegment.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { "Video ($mediaType)" }
        }

        // Check query parameters for title or filename hints
        val filenameParam = uri.getQueryParameter("title")
            ?: uri.getQueryParameter("filename")
            ?: uri.getQueryParameter("name")

        if (!filenameParam.isNullOrBlank()) {
            return filenameParam.substringBeforeLast('.')
        }

        // Fallback title with host
        val host = uri.host ?: "Online"
        return "$host Video ($mediaType)"
    }

    private fun fetchDuration(url: String): Long? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            val headers = mapOf("User-Agent" to "SZPlayer/1.0 (Linux; Android)")
            retriever.setDataSource(url, headers)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull()?.takeIf { it > 0 }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
