package com.shaaztechno.videoplayer.domain.repository

import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo

interface VideoUrlRepository {
    suspend fun checkUrl(url: String): Result<VideoMediaInfo>
}
