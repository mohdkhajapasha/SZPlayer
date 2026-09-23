package com.shaaztechno.videoplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shaaztechno.videoplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE type = :type")
    fun getVideosByType(type: String): Flow<List<VideoEntity>>

    @Query("SELECT id FROM videos WHERE type = :type")
    suspend fun getVideoIdsByType(type: String): List<String>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE downloadId = :downloadId")
    suspend fun getVideoByDownloadId(downloadId: Long): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<String>)

    @Query("DELETE FROM videos WHERE type = 'ONLINE'")
    suspend fun clearOnlineCatalog()
}
