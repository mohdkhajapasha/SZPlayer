package com.shaaztechno.videoplayer.domain.repository

import com.shaaztechno.videoplayer.domain.model.InstagramReel

interface InstagramRepository {
    suspend fun getReel(url: String): Result<InstagramReel>
}
