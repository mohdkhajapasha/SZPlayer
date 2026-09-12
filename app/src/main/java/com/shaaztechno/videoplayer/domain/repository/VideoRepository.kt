package com.shaaztechno.videoplayer.domain.repository

import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getAllVideos(): Flow<List<Video>>
    fun getVideosByType(type: VideoType): Flow<List<Video>>
    suspend fun getVideoById(id: String): Video?
    suspend fun refreshOnlineCatalog()
    suspend fun refreshLocalVideos()
    suspend fun addVideo(video: Video)
    suspend fun deleteVideo(id: String)
    
    // History
    fun getPlaybackHistory(): Flow<List<HistoryEntity>>
    suspend fun getHistoryForVideo(videoId: String): HistoryEntity?
    suspend fun updateHistory(video: Video, position: Long, duration: Long)
    suspend fun clearHistory()
}
