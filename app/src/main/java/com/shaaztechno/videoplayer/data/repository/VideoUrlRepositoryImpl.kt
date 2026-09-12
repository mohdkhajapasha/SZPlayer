package com.shaaztechno.videoplayer.data.repository

import com.shaaztechno.videoplayer.data.remote.VideoMediaResolver
import com.shaaztechno.videoplayer.data.remote.VideoUrlChecker
import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo
import com.shaaztechno.videoplayer.domain.repository.VideoUrlRepository

class VideoUrlRepositoryImpl(
    private val urlChecker: VideoUrlChecker,
    private val mediaResolver: VideoMediaResolver
) : VideoUrlRepository {

    override suspend fun checkUrl(url: String): Result<VideoMediaInfo> {
        val checkResponse = urlChecker.checkAccessibility(url)
        if (!checkResponse.isAccessible) {
            val message = checkResponse.errorMessage ?: "Unable to access video. The URL may be invalid or expired."
            return Result.failure(Exception(message))
        }

        return mediaResolver.resolveMediaInfo(url, checkResponse)
    }
}
