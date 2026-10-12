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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
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

    /**
     * Checks if the URL is accessible and retrieves basic metadata.
     * Prefers GET with Range to get both headers and initial bytes in one request.
     */
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

        // 1. Primary: GET request with byte range (bytes=0-65535)
        val rangeResponse = try {
            val getRequest = Request.Builder()
                .url(validUrl)
                .get()
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-65535")
                .header("Accept", "video/*,audio/*,application/x-mpegurl,application/dash+xml,text/html,*/*;q=0.8")
                .build()

            client.newCall(getRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    val contentType = response.header("Content-Type")
                    val contentLength = parseContentLengthFromGet(response)
                    val effectiveUrl = response.request.url.toString()
                    val bodyBytes = readBodyBytes(response)

                    UrlCheckResponse(
                        isAccessible = true,
                        statusCode = response.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        effectiveUrl = effectiveUrl,
                        initialBytes = bodyBytes
                    )
                } else if (response.code == 403 || response.code == 416 || response.code == 405 || response.code == 400) {
                    null // Fall through to full GET or HEAD
                } else {
                    return handleHttpError(response.code, validUrl)
                }
            }
        } catch (e: Exception) {
            null
        }

        if (rangeResponse != null) return rangeResponse

        // 2. Secondary: HEAD request
        val headResponse = try {
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
                    
                    // If content type is ambiguous, we MUST get bytes to identify it
                    val isAmbiguous = contentType == null || 
                                     contentType.contains("text/html", ignoreCase = true) ||
                                     contentType == "application/octet-stream"
                    
                    val initialBytes = if (isAmbiguous) fetchInitialBytes(effectiveUrl) else null

                    UrlCheckResponse(
                        isAccessible = true,
                        statusCode = response.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        effectiveUrl = effectiveUrl,
                        initialBytes = initialBytes
                    )
                } else {
                    null // Try full GET
                }
            }
        } catch (e: Exception) {
            null
        }
        
        if (headResponse != null) return headResponse

        // 3. Last Fallback: Full GET but only read a bit
        return try {
            val getRequest = Request.Builder()
                .url(validUrl)
                .get()
                .header("User-Agent", userAgent)
                .header("Accept", "video/*,audio/*,application/x-mpegurl,application/dash+xml,text/html,*/*;q=0.8")
                .build()

            client.newCall(getRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type")
                    val contentLength = parseContentLength(response)
                    val effectiveUrl = response.request.url.toString()
                    val bodyBytes = readBodyBytes(response)

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
            UrlCheckResponse(isAccessible = false, statusCode = 0, contentType = null, contentLength = null, effectiveUrl = validUrl, errorMessage = "Secure connection failed (SSL).")
        } catch (e: UnknownHostException) {
            UrlCheckResponse(isAccessible = false, statusCode = 0, contentType = null, contentLength = null, effectiveUrl = validUrl, errorMessage = "Network failed. Unable to resolve address.")
        } catch (e: SocketTimeoutException) {
            UrlCheckResponse(isAccessible = false, statusCode = 408, contentType = null, contentLength = null, effectiveUrl = validUrl, errorMessage = "Connection timed out.")
        } catch (e: Exception) {
            UrlCheckResponse(isAccessible = false, statusCode = 0, contentType = null, contentLength = null, effectiveUrl = validUrl, errorMessage = "Unable to access video: ${e.message}")
        }
    }

    private fun readBodyBytes(response: Response): ByteArray? {
        return try {
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
    }

    private fun handleHttpError(code: Int, url: String): UrlCheckResponse {
        val message = when (code) {
            400 -> "Invalid request. The URL appears malformed."
            401 -> "Authentication required. This video is private."
            403 -> "Access denied (403). The link may have expired or is restricted."
            404 -> "Video not found (404). The link may be broken."
            408 -> "Request timed out."
            429 -> "Too many requests. Please try again later."
            in 500..599 -> "Server error ($code)."
            else -> "Unable to access video (HTTP $code)."
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
        // First try with range
        val rangeResult = try {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-65535")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful || response.code == 206) readBodyBytes(response) else null
            }
        } catch (e: Exception) {
            null
        }
        if (rangeResult != null) return rangeResult

        // Try without range
        return try {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", userAgent)
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) readBodyBytes(response) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
