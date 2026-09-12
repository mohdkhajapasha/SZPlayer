package com.shaaztechno.videoplayer.domain.usecase

import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo
import com.shaaztechno.videoplayer.domain.repository.VideoUrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckVideoUrlUseCase(
    private val repository: VideoUrlRepository
) {
    suspend operator fun invoke(url: String): Result<VideoMediaInfo> {
        return withContext(Dispatchers.IO) {
            repository.checkUrl(url.trim())
        }
    }
}
