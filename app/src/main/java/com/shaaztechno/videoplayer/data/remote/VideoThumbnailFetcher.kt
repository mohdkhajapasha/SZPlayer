package com.shaaztechno.videoplayer.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.regex.Pattern

/**
 * Custom Coil Fetcher that extracts video frame thumbnails from remote HTTP/HTTPS video URLs
 * (such as MP4, MKV, WebM, HLS M3U8, etc.) and YouTube URLs without downloading full video files.
 * Caches extracted frames to the app's cache directory for fast subsequent loads.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val urlString: String,
    private val options: Options,
    private val httpClient: OkHttpClient
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val cleanUrl = urlString.trim()

        // 1. YouTube thumbnail handling
        val ytThumbnailUrl = getYouTubeThumbnail(cleanUrl)
        if (ytThumbnailUrl != null) {
            val ytBitmap = fetchBitmapFromUrl(ytThumbnailUrl)
            if (ytBitmap != null) {
                return@withContext DrawableResult(
                    drawable = BitmapDrawable(context.resources, ytBitmap),
                    isSampled = false,
                    dataSource = DataSource.NETWORK
                )
            }
        }

        // 2. Check disk cache for previously extracted video frame
        val urlHash = cleanUrl.hashCode().toString()
        val cacheDir = File(context.cacheDir, "video_thumbnails")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, "thumb_$urlHash.jpg")

        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val cachedBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (cachedBitmap != null) {
                    return@withContext DrawableResult(
                        drawable = BitmapDrawable(context.resources, cachedBitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (e: Exception) {
                Log.w("VideoThumbnailFetcher", "Failed to decode cached thumbnail", e)
            }
        }

        // 3. For HLS (.m3u8), resolve the first media segment URL
        val targetVideoUrl = if (cleanUrl.substringBefore('?').lowercase(Locale.ROOT).endsWith(".m3u8")) {
            resolveHlsFirstSegment(cleanUrl) ?: cleanUrl
        } else {
            cleanUrl
        }

        // 4. Extract frame using MediaMetadataRetriever
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(targetVideoUrl, mapOf("User-Agent" to "SZPlayer/1.0 (Linux; Android)"))

            // Prefer frame at 1.0 second (1,000,000 microseconds) to avoid black intro frames
            val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                try {
                    FileOutputStream(cacheFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                } catch (e: Exception) {
                    Log.w("VideoThumbnailFetcher", "Failed to save thumbnail cache", e)
                }

                return@withContext DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.NETWORK
                )
            }
        } catch (e: Exception) {
            Log.w("VideoThumbnailFetcher", "Frame extraction failed for: $targetVideoUrl: ${e.message}")
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }

        null
    }

    private fun fetchBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "SZPlayer/1.0 (Linux; Android)")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes() ?: return null
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else null
            }
        } catch (e: Exception) {
            null
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

    companion object {
        fun getYouTubeThumbnail(url: String): String? {
            val matcher = Pattern.compile(
                "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})",
                Pattern.CASE_INSENSITIVE
            ).matcher(url)
            return if (matcher.find()) "https://img.youtube.com/vi/${matcher.group(1)}/hqdefault.jpg" else null
        }

        fun isVideoUrl(url: String): Boolean {
            val lower = url.substringBefore('?').lowercase(Locale.ROOT)
            val isKnownVideo = listOf(
                ".mp4", ".mkv", ".webm", ".m3u8", ".mov", ".3gp", ".ts", ".mpd", ".flv", ".avi"
            ).any { lower.endsWith(it) }
            val isYouTube = url.contains("youtube.com") || url.contains("youtu.be")
            return isKnownVideo || isYouTube
        }
    }

    class UriFactory(
        private val context: Context,
        private val httpClient: OkHttpClient
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme?.lowercase(Locale.ROOT) ?: return null
            if (scheme != "http" && scheme != "https") return null

            val urlString = data.toString()
            val lowerPath = (data.path ?: "").lowercase(Locale.ROOT)

            // Let normal Coil HttpUriFetcher handle standard image files
            val isImage = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".svg").any {
                lowerPath.endsWith(it)
            }
            if (isImage) return null

            if (isVideoUrl(urlString)) {
                return VideoThumbnailFetcher(context, urlString, options, httpClient)
            }

            return null
        }
    }

    class StringFactory(
        private val context: Context,
        private val httpClient: OkHttpClient
    ) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            val trimmed = data.trim()
            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                return null
            }

            val lowerPath = trimmed.substringBefore('?').lowercase(Locale.ROOT)
            val isImage = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".svg").any {
                lowerPath.endsWith(it)
            }
            if (isImage) return null

            if (isVideoUrl(trimmed)) {
                return VideoThumbnailFetcher(context, trimmed, options, httpClient)
            }

            return null
        }
    }
}
