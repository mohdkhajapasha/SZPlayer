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

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

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

        // 4. Verification
        // If we detected a valid media type or mimeType, we consider it playable.
        // We use MimeTypes.VIDEO_MP4 as a generic hint for progressive streams if unknown.
        val resolvedMimeType = mimeType ?: if (mediaType != "HTML") MimeTypes.VIDEO_MP4 else null
        val isPlayable = resolvedMimeType != null

        if (!isPlayable) {
            return Result.failure(
                IllegalArgumentException("SZ Player could not recognize a playable video stream at this URL.")
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
                mimeType = resolvedMimeType,
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
        val pureMime = contentType?.substringBefore(';')?.trim()

        // 1. Explicit Video MIME types from headers
        if (pureMime != null) {
            when {
                pureMime.contains("video/mp4") || pureMime.contains("application/mp4") ->
                    return "MP4" to MimeTypes.VIDEO_MP4
                pureMime.contains("video/webm") ->
                    return "WebM" to MimeTypes.VIDEO_WEBM
                pureMime.contains("video/x-matroska") || pureMime.contains("video/mkv") ->
                    return "MKV" to MimeTypes.VIDEO_MATROSKA
                pureMime.contains("application/x-mpegurl") ||
                pureMime.contains("application/vnd.apple.mpegurl") ||
                pureMime.contains("audio/mpegurl") ->
                    return "HLS Stream" to MimeTypes.APPLICATION_M3U8
                pureMime.contains("application/dash+xml") ->
                    return "DASH Stream" to MimeTypes.APPLICATION_MPD
                pureMime.contains("video/quicktime") ->
                    return "QuickTime MOV" to MimeTypes.VIDEO_MP4
                pureMime.contains("video/3gpp") ->
                    return "3GP" to MimeTypes.VIDEO_H263
                pureMime.startsWith("video/") ->
                    return "Video" to pureMime
            }
        }

        // 2. Extension check (High confidence)
        val extensionMatch = when {
            lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".m4v") -> "MP4" to MimeTypes.VIDEO_MP4
            lowerUrl.endsWith(".m3u8") -> "HLS Stream" to MimeTypes.APPLICATION_M3U8
            lowerUrl.endsWith(".mpd") -> "DASH Stream" to MimeTypes.APPLICATION_MPD
            lowerUrl.endsWith(".webm") -> "WebM" to MimeTypes.VIDEO_WEBM
            lowerUrl.endsWith(".mkv") -> "MKV" to MimeTypes.VIDEO_MATROSKA
            lowerUrl.endsWith(".mov") -> "QuickTime MOV" to MimeTypes.VIDEO_MP4
            lowerUrl.endsWith(".3gp") -> "3GP" to MimeTypes.VIDEO_H263
            lowerUrl.endsWith(".ts") -> "MPEG-TS" to MimeTypes.VIDEO_MP2T
            else -> null
        }
        if (extensionMatch != null) return extensionMatch

        // 3. Content Sniffing (Magic Bytes)
        if (initialBytes != null && initialBytes.size >= 4) {
            val hexPrefix = initialBytes.take(8).joinToString("") { "%02x".format(it) }
            
            // MP4 (ftyp)
            if (hexPrefix.contains("66747970")) return "MP4" to MimeTypes.VIDEO_MP4
            
            // Matroska / WebM (EBML)
            if (hexPrefix.startsWith("1a45dfa3")) return "Video" to MimeTypes.VIDEO_WEBM
            
            // MPEG-TS (Sync byte 0x47)
            if (initialBytes[0] == 0x47.toByte()) return "MPEG-TS" to MimeTypes.VIDEO_MP2T

            // HLS / M3U8
            val textPrefix = try { String(initialBytes.take(512).toByteArray(), Charsets.UTF_8) } catch (e: Exception) { "" }
            if (textPrefix.startsWith("#EXTM3U", false)) return "HLS Stream" to MimeTypes.APPLICATION_M3U8
            if (textPrefix.contains("<MPD") || textPrefix.contains("urn:mpeg:dash")) return "DASH Stream" to MimeTypes.APPLICATION_MPD

            // Ambiguous binary streams (e.g. googleusercontent download links)
            if (pureMime == "application/octet-stream" || pureMime == null) {
                // If it's not HTML or XML, assume it's a video stream (Progressive)
                if (!textPrefix.trimStart().startsWith("<") && !textPrefix.contains("<!DOCTYPE", true)) {
                    return "Online Video" to MimeTypes.VIDEO_MP4 // Hint for ExoPlayer to attempt progressive playback
                }
            }
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
        val caption = extractCaption(html)?.let { cleanTitle(decodeHtmlEntities(it)) }
        
        if (!caption.isNullOrBlank()) {
            if (effectiveUrl.contains("instagram.com") || effectiveUrl.contains("facebook.com") || effectiveUrl.contains("tiktok.com")) {
                return caption
            }
            return if (!sourceName.isNullOrBlank() && !caption.contains(sourceName, true)) {
                "$caption • $sourceName"
            } else {
                caption
            }
        }

        if (!authorName.isNullOrBlank()) {
            return if (!sourceName.isNullOrBlank()) "$sourceName Video by $authorName" else "Video by $authorName"
        }

        val uri = Uri.parse(effectiveUrl)
        val lastPathSegment = uri.lastPathSegment ?: Uri.parse(originalUrl).lastPathSegment
        if (!lastPathSegment.isNullOrBlank() && lastPathSegment.length > 2) {
            val name = lastPathSegment.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
            if (name.length > 3 && !name.contains("videoplayback", true)) return name
        }

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
            retriever.setDataSource(targetUrl, mapOf("User-Agent" to userAgent))
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
                .header("User-Agent", userAgent)
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
        return listOf(".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".mov", ".3gp", ".ts", ".m4v").any { segment.endsWith(it, ignoreCase = true) }
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
            retriever.setDataSource(url, mapOf("User-Agent" to userAgent))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0 }
        } catch (e: Exception) { null } finally { try { retriever?.release() } catch (e: Exception) {} }
    }
}
