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
                .header("User-Agent", "SZPlayer/1.0 (Linux; Android)")
                .build()

            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type")
                    val contentLength = parseContentLength(response)
                    val effectiveUrl = response.request.url.toString()
                    return UrlCheckResponse(
                        isAccessible = true,
                        statusCode = response.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        effectiveUrl = effectiveUrl
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

        // 2. Fallback: GET request with byte range (bytes=0-8191) to fetch headers and initial magic bytes
        return try {
            val getRequest = Request.Builder()
                .url(validUrl)
                .get()
                .header("User-Agent", "SZPlayer/1.0 (Linux; Android)")
                .header("Range", "bytes=0-8191")
                .build()

            client.newCall(getRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    val contentType = response.header("Content-Type")
                    val contentLength = parseContentLengthFromGet(response)
                    val effectiveUrl = response.request.url.toString()
                    val bodyBytes = try {
                        response.body?.byteStream()?.use { stream ->
                            val buf = ByteArray(8192)
                            val out = java.io.ByteArrayOutputStream()
                            var remaining = 8192
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
}
