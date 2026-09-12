package com.shaaztechno.videoplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val description: String?,
    val category: String?,
    val duration: Long,
    val type: String, // ONLINE, LOCAL, DOWNLOADED
    val localUri: String?,
    val size: Long,
    val folder: String?,
    val dateAdded: Long,
    val downloadId: Long? = null
) {
    fun toDomain(): Video {
        return Video(
            id = id,
            title = title,
            url = url,
            thumbnailUrl = thumbnailUrl,
            description = description,
            category = category,
            duration = duration,
            type = VideoType.valueOf(type),
            localUri = localUri,
            size = size,
            folder = folder,
            dateAdded = dateAdded,
            downloadId = downloadId
        )
    }
}

fun Video.toEntity(): VideoEntity {
    return VideoEntity(
        id = id,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        description = description,
        category = category,
        duration = duration,
        type = type.name,
        localUri = localUri,
        size = size,
        folder = folder,
        dateAdded = dateAdded,
        downloadId = downloadId
    )
}
