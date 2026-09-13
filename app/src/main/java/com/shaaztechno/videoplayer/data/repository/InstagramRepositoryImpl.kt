package com.shaaztechno.videoplayer.data.repository

import com.shaaztechno.videoplayer.data.remote.InstagramMediaExtractor
import com.shaaztechno.videoplayer.domain.model.InstagramReel
import com.shaaztechno.videoplayer.domain.repository.InstagramRepository
import java.io.IOException
import java.net.SocketTimeoutException

class InstagramRepositoryImpl(
    private val extractor: InstagramMediaExtractor
) : InstagramRepository {

    override suspend fun getReel(url: String): Result<InstagramReel> {
        return try {
            extractor.extract(url)
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Request timed out. Please check your internet connection."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "An unknown error occurred while fetching the Reel."))
        }
    }
}
