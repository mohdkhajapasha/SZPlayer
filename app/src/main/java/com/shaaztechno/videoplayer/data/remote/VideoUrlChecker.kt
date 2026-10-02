package com.shaaztechno.videoplayer.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import java.util.concurrent.TimeUnit

data class UrlCheckResponse(
    val isAccessible: Boolean,
    val statusCode: Int,
    val contentType: String?,
    val contentLength: Long?,
    val effectiveUrl: String,
    val initialBytes: ByteArray? = null,
    val errorMessage: String? = null
)

class VideoUrlChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun validateUrlSyntax(urlString: String): Result<String> {
        val trimmed = urlString.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("URL cannot be empty"))
        }

        val url = try {
            URL(trimmed)
        } catch (e: MalformedURLException) {
            return Result.failure(IllegalArgumentException("Invalid URL format. Please enter a valid web address."))
        }

        val protocol = url.protocol?.lowercase()
        if (protocol != "http" && protocol != "https") {
            return Result.failure(IllegalArgumentException("URL must start with http:// or https://"))
        }

        try {
            URI(trimmed)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid URL syntax."))
        }

        return Result.success(trimmed)
    }

    suspend fun checkAccessibility(urlString: String): UrlCheckResponse {
        val syntaxCheck = validateUrlSyntax(urlString)
        if (syntaxCheck.isFailure) {
            return UrlCheckResponse(
                isAccessible = false,
                statusCode = 0,
                contentType = null,
                contentLength = null,
                effectiveUrl = urlString,
                errorMessage = syntaxCheck.exceptionOrNull()?.message ?: "Invalid URL"
            )
        }

        val validUrl = syntaxCheck.getOrThrow()

        // 1. Try HEAD request first
        try {
            val headRequest = Request.Builder()
                .url(validUrl)
                .head()
                .header("User-Agent", userAgent)
                .build()

            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type")
                    val contentLength = parseContentLength(response)
                    val effectiveUrl = response.request.url.toString()
                    
                    val isHtml = contentType == null ||
                            contentType.contains("text/html", ignoreCase = true) ||
                            contentType.contains("application/xhtml", ignoreCase = true) ||
                            !contentType.startsWith("video/", ignoreCase = true)
                    val initialBytes = if (isHtml) fetchInitialBytes(effectiveUrl) else null

                    return UrlCheckResponse(
                        isAccessible = true,
                        statusCode = response.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        effectiveUrl = effectiveUrl,
                        initialBytes = initialBytes
                    )
                }
                // If HEAD returns 405 (Method Not Allowed) or 403 (Forbidden on HEAD only),
                // fall through to range GET fallback below.
                if (response.code != 405 && response.code != 403 && response.code != 400) {
                    return handleHttpError(response.code, validUrl)
                }
            }
        } catch (e: SSLException) {
            return UrlCheckResponse(
                isAccessible = false,
                statusCode = 0,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Secure connection failed. Unable to verify SSL certificate."
            )
        } catch (e: UnknownHostException) {
            return UrlCheckResponse(
                isAccessible = false,
                statusCode = 0,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Network connection failed. Unable to resolve server address."
            )
        } catch (e: SocketTimeoutException) {
            return UrlCheckResponse(
                isAccessible = false,
                statusCode = 408,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Connection timed out while trying to reach the video server."
            )
        } catch (e: IOException) {
            // Fall through to GET fallback in case HEAD is not supported by endpoint
        }

        // 2. Fallback: GET request with byte range (bytes=0-65535) to fetch headers and initial magic bytes
        return try {
            val getRequest = Request.Builder()
                .url(validUrl)
                .get()
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-65535")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,video/*,*/*;q=0.8")
                .build()

            client.newCall(getRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    val contentType = response.header("Content-Type")
                    val contentLength = parseContentLengthFromGet(response)
                    val effectiveUrl = response.request.url.toString()
                    val bodyBytes = try {
                        response.body?.byteStream()?.use { stream ->
                            val buf = ByteArray(65536)
                            val out = java.io.ByteArrayOutputStream()
                            var remaining = 65536
                            var n = 0
                            while (remaining > 0 && stream.read(buf, 0, minOf(buf.size, remaining)).also { n = it } != -1) {
                                out.write(buf, 0, n)
                                remaining -= n
                            }
                            out.toByteArray()
                        }
                    } catch (e: Exception) {
                        null
                    }

                    UrlCheckResponse(
                        isAccessible = true,
                        statusCode = response.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        effectiveUrl = effectiveUrl,
                        initialBytes = bodyBytes
                    )
                } else {
                    handleHttpError(response.code, validUrl)
                }
            }
        } catch (e: SSLException) {
            UrlCheckResponse(
                isAccessible = false,
                statusCode = 0,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Secure connection failed. Unable to verify SSL certificate."
            )
        } catch (e: UnknownHostException) {
            UrlCheckResponse(
                isAccessible = false,
                statusCode = 0,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Network connection failed. Unable to resolve server address."
            )
        } catch (e: SocketTimeoutException) {
            UrlCheckResponse(
                isAccessible = false,
                statusCode = 408,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Connection timed out while trying to reach the video server."
            )
        } catch (e: Exception) {
            UrlCheckResponse(
                isAccessible = false,
                statusCode = 0,
                contentType = null,
                contentLength = null,
                effectiveUrl = validUrl,
                errorMessage = "Unable to access video. The URL may be invalid, expired, or private."
            )
        }
    }

    private fun handleHttpError(code: Int, url: String): UrlCheckResponse {
        val message = when (code) {
            400 -> "Invalid video request. The URL appears malformed."
            401 -> "Authentication required. This video is private."
            403 -> "Access denied. The video URL has expired, is private, or requires authorization."
            404 -> "Video not found. The URL may be broken or the video has been removed."
            408 -> "Server request timed out. Please try again."
            429 -> "Too many requests. The server is rate-limiting access."
            in 500..599 -> "Video server error ($code). The hosting server is currently unavailable."
            else -> "Unable to access video (HTTP $code). The URL may be invalid or expired."
        }
        return UrlCheckResponse(
            isAccessible = false,
            statusCode = code,
            contentType = null,
            contentLength = null,
            effectiveUrl = url,
            errorMessage = message
        )
    }

    private fun parseContentLength(response: Response): Long? {
        val lengthStr = response.header("Content-Length")
        return lengthStr?.toLongOrNull()?.takeIf { it > 0 }
    }

    private fun parseContentLengthFromGet(response: Response): Long? {
        val rangeHeader = response.header("Content-Range")
        if (!rangeHeader.isNullOrBlank() && rangeHeader.contains("/")) {
            val totalStr = rangeHeader.substringAfterLast("/")
            val total = totalStr.toLongOrNull()
            if (total != null && total > 0) return total
        }
        return parseContentLength(response)
    }

    private fun fetchInitialBytes(url: String): ByteArray? {
        return try {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-65535")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    response.body?.byteStream()?.use { stream ->
                        val buf = ByteArray(65536)
                        val out = java.io.ByteArrayOutputStream()
                        var remaining = 65536
                        var n = 0
                        while (remaining > 0 && stream.read(buf, 0, minOf(buf.size, remaining)).also { n = it } != -1) {
                            out.write(buf, 0, n)
                            remaining -= n
                        }
                        out.toByteArray()
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
