package com.shaaztechno.videoplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val url: String,
    val type: String,
    val thumbnailUrl: String?,
    val lastPosition: Long,
    val duration: Long,
    val timestamp: Long
) {
    fun toVideo(): Video {
        return Video(
            id = videoId,
            title = title,
            url = url,
            thumbnailUrl = thumbnailUrl,
            type = try { VideoType.valueOf(type) } catch (e: Exception) { VideoType.ONLINE },
            duration = duration,
            dateAdded = timestamp
        )
    }
}
