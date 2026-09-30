package com.shaaztechno.videoplayer.data.remote

import android.util.Log
import com.shaaztechno.videoplayer.domain.model.InstagramReel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class InstagramMediaExtractor(private val okHttpClient: OkHttpClient) {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun extract(url: String): Result<InstagramReel> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            Log.d("InstagramExtractor", "Fetching Reel from: $normalizedUrl")

            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Upgrade-Insecure-Requests", "1")
                .build()

            val result = okHttpClient.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                
                if (finalUrl.contains("accounts/login")) {
                    Log.w("InstagramExtractor", "Redirected to login page: $finalUrl")
                    return@use Result.failure(Exception("Unable to retrieve this Reel. The Reel may be private, login-protected, unavailable, or temporarily blocked by Instagram."))
                }

                if (!response.isSuccessful) {
                    Log.e("InstagramExtractor", "HTTP error: ${response.code}")
                    return@use Result.failure(Exception("Unable to connect to Instagram (HTTP ${response.code})."))
                }

                val html = response.body?.string() ?: return@use Result.failure(Exception("Empty response from Instagram."))
                
                // Bot protection check
                if (html.contains("Please wait a few minutes before you try again") || html.contains("Checking if the site connection is secure")) {
                    return@use Result.failure(Exception("Instagram is temporarily limiting access. Please try again later."))
                }

                val videoUrl = extractVideoUrl(html)
                val thumbnailUrl = extractThumbnailUrl(html)
                val caption = extractCaption(html)
                val username = extractUsername(html)

                if (videoUrl != null && videoUrl.startsWith("http")) {
                    Result.success(
                        InstagramReel(
                            videoUrl = cleanUrl(videoUrl),
                            thumbnailUrl = thumbnailUrl?.let { if (it.startsWith("http")) cleanUrl(it) else "" } ?: "",
                            caption = caption?.let { decodeHtmlEntities(it) },
                            username = username
                        )
                    )
                } else {
                    if (html.contains("Login") && html.contains("Password") || html.contains("login_page")) {
                        Result.failure(Exception("Unable to retrieve this Reel. The Reel may be private, login-protected, unavailable, or temporarily blocked by Instagram."))
                    } else {
                        Result.failure(Exception("Unable to retrieve media information. Public Reel format may have changed."))
                    }
                }
            }
            result
        } catch (e: Exception) {
            Log.e("InstagramExtractor", "Extraction failed", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http")) {
            normalized = "https://$normalized"
        }
        val reelIdMatch = Pattern.compile("/(reels?|p)/([a-zA-Z0-9_-]+)").matcher(normalized)
        if (reelIdMatch.find()) {
            val type = reelIdMatch.group(1)
            val id = reelIdMatch.group(2)
            return "https://www.instagram.com/$type/$id/"
        }
        return normalized
    }

    private fun cleanUrl(url: String): String {
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
            sb.toString()
        } catch (e: Exception) {
            cleaned
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
    }

    private fun extractVideoUrl(html: String): String? {
        val patterns = listOf(
            "\"video_url\":\"(.*?)\"",
            "<meta property=\"og:video\" content=\"(.*?)\"",
            "<meta property=\"og:video:secure_url\" content=\"(.*?)\"",
            "\"contentUrl\":\"(.*?)\"",
            "\"xdt_api__v1__media__direct_path\":\"(.*?)\"",
            "\"video_versions\":\\[\\{[^}]*\"url\":\"(.*?)\"",
            "\"video_url\":\\s*\"(.*?)\"",
            "playable_url\":\"(.*?)\"",
            "\"shortcode_media\":\\{.*?\"video_url\":\"(.*?)\"",
            "\"base_url\":\"(.*?)\""
        )
        return findFirstMatch(html, patterns)
    }

    private fun extractThumbnailUrl(html: String): String? {
        val patterns = listOf(
            "<meta property=\"og:image\" content=\"(.*?)\"",
            "\"display_url\":\"(.*?)\"",
            "\"thumbnailUrl\":\"(.*?)\"",
            "\"thumbnail_src\":\"(.*?)\"",
            "\"thumbnail_resources\":\\[.*\"src\":\"(.*?)\"",
            "\"display_resources\":\\[.*\"src\":\"(.*?)\""
        )
        return findFirstMatch(html, patterns)
    }

    private fun extractCaption(html: String): String? {
        val patterns = listOf(
            "<meta property=\"og:title\" content=\"(.*?)\"",
            "<meta name=\"description\" content=\"(.*?)\"",
            "\"caption\":\"(.*?)\"",
            "\"edge_media_to_caption\":\\{\"edges\":\\[\\{\"node\":\\{\"text\":\"(.*?)\""
        )
        return findFirstMatch(html, patterns)
    }

    private fun extractUsername(html: String): String? {
        val patterns = listOf(
            "\"username\":\"(.*?)\"",
            "\"owner\":\\{\"username\":\"(.*?)\"",
            "og:description\" content=\"(.*?)\""
        )
        val match = findFirstMatch(html, patterns)
        return if (match?.contains("on Instagram:") == true) {
            val parts = match.split(" on Instagram:")
            if (parts.isNotEmpty()) parts[0].trim() else match
        } else {
            match
        }
    }

    private fun findFirstMatch(html: String, patterns: List<String>): String? {
        for (patternStr in patterns) {
            try {
                val pattern = Pattern.compile(patternStr)
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val result = matcher.group(1)
                    if (!result.isNullOrBlank()) return result
                }
            } catch (e: Exception) {}
        }
        return null
    }
}
