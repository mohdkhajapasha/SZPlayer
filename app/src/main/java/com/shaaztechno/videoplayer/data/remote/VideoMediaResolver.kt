package com.shaaztechno.videoplayer.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.regex.Pattern

class VideoMediaResolver(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
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

        // 2. Extract HTML content if available for rich metadata
        val html = if (initialBytes != null && initialBytes.isNotEmpty()) {
            try { String(initialBytes, Charsets.UTF_8) } catch (e: Exception) { "" }
        } else ""

        // 3. Derive Metadata (Title, Thumbnail, Source, Author)
        val sourceName = extractSiteName(effectiveUrl, html)
        val authorName = extractAuthorName(html)
        val title = deriveTitle(originalUrl, effectiveUrl, mediaType, html, sourceName, authorName)
        var thumbnailUrl = deriveThumbnailUrl(effectiveUrl, html)
        if (thumbnailUrl.isNullOrBlank()) {
            thumbnailUrl = extractVideoFrameThumbnail(effectiveUrl)
        }

        // 4. Verify Media3 can create a media source
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

        // 5. Try to determine duration (for progressive files)
        val duration = if (mediaType.startsWith("HLS", false) || mediaType.startsWith("DASH", false)) {
            null
        } else {
            fetchDuration(effectiveUrl)
        }

        // 6. Download capability
        val isDownloadable = checkResponse.isAccessible

        return Result.success(
            VideoMediaInfo(
                url = effectiveUrl,
                title = title,
                mediaType = mediaType,
                mimeType = mimeType,
                thumbnailUrl = thumbnailUrl,
                sourceName = sourceName,
                authorName = authorName,
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

        if (initialBytes != null && initialBytes.isNotEmpty()) {
            val textPrefix = try {
                String(initialBytes.take(256).toByteArray(), Charsets.UTF_8)
            } catch (e: Exception) { "" }

            if (textPrefix.startsWith("#EXTM3U", false)) return "HLS Stream" to MimeTypes.APPLICATION_M3U8
            if (textPrefix.contains("<MPD") || textPrefix.contains("urn:mpeg:dash")) return "DASH Stream" to MimeTypes.APPLICATION_MPD

            if (initialBytes.size >= 8) {
                val boxType = String(initialBytes.sliceArray(4..7), Charsets.US_ASCII)
                if (boxType.lowercase(Locale.ROOT) == "ftyp") return "MP4" to MimeTypes.VIDEO_MP4
            }
        }

        when {
            lowerUrl.endsWith(".mp4") -> return "MP4" to MimeTypes.VIDEO_MP4
            lowerUrl.endsWith(".m3u8") -> return "HLS Stream" to MimeTypes.APPLICATION_M3U8
            lowerUrl.endsWith(".mpd") -> return "DASH Stream" to MimeTypes.APPLICATION_MPD
            lowerUrl.endsWith(".webm") -> return "WebM" to MimeTypes.VIDEO_WEBM
            lowerUrl.endsWith(".mkv") -> return "MKV" to MimeTypes.VIDEO_MATROSKA
            lowerUrl.endsWith(".mov") -> return "QuickTime MOV" to MimeTypes.VIDEO_MP4
            lowerUrl.endsWith(".3gp") -> return "3GP" to MimeTypes.VIDEO_H263
        }

        return null to null
    }

    private fun deriveTitle(
        originalUrl: String,
        effectiveUrl: String,
        mediaType: String,
        html: String,
        sourceName: String?,
        authorName: String?
    ): String {
        // 1. Try to extract a rich caption from HTML
        val caption = extractCaption(html)?.let { cleanTitle(decodeHtmlEntities(it)) }
        
        if (!caption.isNullOrBlank()) {
            // For social sites, match the "Proper Title" format of Instagram Reels
            if (effectiveUrl.contains("instagram.com") || effectiveUrl.contains("facebook.com") || effectiveUrl.contains("tiktok.com")) {
                return caption
            }
            // For other sites, adding the source name makes it look cleaner
            return if (!sourceName.isNullOrBlank() && !caption.contains(sourceName, true)) {
                "$caption • $sourceName"
            } else {
                caption
            }
        }

        // 2. If no caption, but we have an author
        if (!authorName.isNullOrBlank()) {
            return if (!sourceName.isNullOrBlank()) "$sourceName Video by $authorName" else "Video by $authorName"
        }

        // 3. Fallback to URL filename
        val uri = Uri.parse(effectiveUrl)
        val lastPathSegment = uri.lastPathSegment ?: Uri.parse(originalUrl).lastPathSegment
        if (!lastPathSegment.isNullOrBlank() && lastPathSegment.length > 2 && isKnownExtension(lastPathSegment)) {
            return lastPathSegment.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
        }

        // 4. Default fallback
        return if (!sourceName.isNullOrBlank()) "$sourceName Video ($mediaType)" else "Online Video ($mediaType)"
    }

    private fun deriveThumbnailUrl(effectiveUrl: String, html: String): String? {
        val ytThumbnail = getYouTubeThumbnail(effectiveUrl)
        if (ytThumbnail != null) return ytThumbnail

        if (html.isBlank()) return null

        val patterns = listOf(
            "<meta\\s+property=\"og:image\"\\s+content=\"(.*?)\"",
            "<meta\\s+property=\"og:image:secure_url\"\\s+content=\"(.*?)\"",
            "\"display_url\":\"(.*?)\"",
            "\"thumbnail_src\":\"(.*?)\"",
            "\"thumbnail_resources\":\\[.*\"src\":\"(.*?)\"",
            "\"display_resources\":\\[.*\"src\":\"(.*?)\"",
            "<meta\\s+name=\"twitter:image\"\\s+content=\"(.*?)\"",
            "<link\\s+rel=\"image_src\"\\s+href=\"(.*?)\"",
            "<meta\\s+name=\"thumbnail\"\\s+content=\"(.*?)\"",
            "\"thumbnailUrl\":\"(.*?)\"",
            "\"thumbnail_url\":\"(.*?)\""
        )

        for (patternStr in patterns) {
            try {
                val matcher = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE).matcher(html)
                if (matcher.find()) {
                    val rawResult = matcher.group(1)
                    if (!rawResult.isNullOrBlank()) {
                        val cleaned = cleanExtractedUrl(rawResult)
                        if (cleaned.isNotBlank()) {
                            return if (cleaned.startsWith("http")) cleaned else resolveRelativeUrl(effectiveUrl, cleaned)
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return null
    }

    private fun cleanExtractedUrl(url: String): String {
        val cleaned = url
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u0025", "%")
            .replace("\\/", "/")
            .replace("&amp;", "&")

        return try {
            val pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})")
            val matcher = pattern.matcher(cleaned)
            val sb = StringBuffer()
            while (matcher.find()) {
                val hex = matcher.group(1) ?: continue
                val codePoint = hex.toInt(16)
                val str = String(Character.toChars(codePoint))
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(str))
            }
            matcher.appendTail(sb)
            sb.toString().trim()
        } catch (e: Exception) {
            cleaned.trim()
        }
    }

    private fun extractVideoFrameThumbnail(url: String): String? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            val urlHash = url.hashCode().toString()
            val thumbDir = File(context.cacheDir, "video_thumbnails")
            if (!thumbDir.exists()) thumbDir.mkdirs()
            val thumbFile = File(thumbDir, "thumb_$urlHash.jpg")
            if (thumbFile.exists() && thumbFile.length() > 0) {
                return Uri.fromFile(thumbFile).toString()
            }

            val targetUrl = if (url.substringBefore('?').lowercase(Locale.ROOT).endsWith(".m3u8")) {
                resolveHlsFirstSegment(url) ?: url
            } else {
                url
            }

            retriever = MediaMetadataRetriever()
            retriever.setDataSource(targetUrl, mapOf("User-Agent" to "SZPlayer/1.0 (Linux; Android)"))
            val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                Uri.fromFile(thumbFile).toString()
            } else null
        } catch (e: Exception) {
            null
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }
    }

    private fun resolveHlsFirstSegment(m3u8Url: String): String? {
        return try {
            val request = Request.Builder()
                .url(m3u8Url)
                .header("User-Agent", "SZPlayer/1.0 (Linux; Android)")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null

                var firstSegment: String? = null
                for (line in body.lineSequence()) {
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        firstSegment = trimmed
                        break
                    }
                }

                if (firstSegment != null) {
                    if (firstSegment.startsWith("http://") || firstSegment.startsWith("https://")) {
                        firstSegment
                    } else {
                        val base = m3u8Url.substringBeforeLast('/') + "/"
                        base + firstSegment
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractSiteName(url: String, html: String): String? {
        val patterns = listOf(
            "<meta\\s+property=\"og:site_name\"\\s+content=\"(.*?)\"",
            "<meta\\s+name=\"twitter:site\"\\s+content=\"(.*?)\""
        )
        for (patternStr in patterns) {
            try {
                val matcher = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE).matcher(html)
                if (matcher.find()) return matcher.group(1)
            } catch (e: Exception) {}
        }
        val host = Uri.parse(url).host ?: "Online"
        return host.replace("www.", "").split('.').firstOrNull()?.replaceFirstChar { it.uppercase() } ?: host
    }

    private fun extractAuthorName(html: String): String? {
        val patterns = listOf(
            "<meta\\s+name=\"author\"\\s+content=\"(.*?)\"",
            "<meta\\s+property=\"twitter:creator\"\\s+content=\"(.*?)\"",
            "\"username\":\"(.*?)\"",
            "\"owner\":\\{\"username\":\"(.*?)\""
        )
        for (patternStr in patterns) {
            try {
                val matcher = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE).matcher(html)
                if (matcher.find()) {
                    val match = matcher.group(1)
                    if (!match.isNullOrBlank()) return match.split(" on Instagram:").first().trim()
                }
            } catch (e: Exception) {}
        }
        return null
    }

    private fun extractCaption(html: String): String? {
        val patterns = listOf(
            "<meta\\s+property=\"og:title\"\\s+content=\"(.*?)\"",
            "<meta\\s+name=\"description\"\\s+content=\"(.*?)\"",
            "\"caption\":\"(.*?)\"",
            "<title>(.*?)</title>"
        )
        for (patternStr in patterns) {
            try {
                val matcher = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
                if (matcher.find()) {
                    val res = matcher.group(1)
                    if (!res.isNullOrBlank() && res.length > 3) return res
                }
            } catch (e: Exception) {}
        }
        return null
    }

    private fun resolveRelativeUrl(base: String, relative: String): String {
        val baseUri = Uri.parse(base)
        return when {
            relative.startsWith("//") -> "${baseUri.scheme ?: "https"}:$relative"
            relative.startsWith("/") -> "${baseUri.scheme ?: "https"}://${baseUri.host}$relative"
            else -> {
                val path = baseUri.path?.substringBeforeLast('/') ?: ""
                "${baseUri.scheme ?: "https"}://${baseUri.host}$path/$relative"
            }
        }
    }

    private fun getYouTubeThumbnail(url: String): String? {
        val matcher = Pattern.compile("(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})", Pattern.CASE_INSENSITIVE).matcher(url)
        return if (matcher.find()) "https://img.youtube.com/vi/${matcher.group(1)}/hqdefault.jpg" else null
    }

    private fun isKnownExtension(segment: String): Boolean {
        return listOf(".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".mov", ".3gp", ".ts").any { segment.endsWith(it, ignoreCase = true) }
    }

    private fun cleanTitle(title: String): String = title
        .replace(Regex("(?i).* on Instagram: \"?"), "")
        .replace(Regex("(?i) - YouTube$"), "")
        .replace(Regex("(?i) \\| Video$"), "")
        .replace(Regex("(?i) \\| Instagram$"), "")
        .replace("\"", "").trim()

    private fun decodeHtmlEntities(text: String): String = text
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
        .replace("\\n", " ").replace("\\r", " ").trim()

    private fun fetchDuration(url: String): Long? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(url, mapOf("User-Agent" to "SZPlayer/1.0 (Linux; Android)"))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0 }
        } catch (e: Exception) { null } finally { try { retriever?.release() } catch (e: Exception) {} }
    }
}
