package com.shaaztechno.videoplayer.domain.model

import android.net.Uri

data class Video(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val category: String? = null,
    val duration: Long = 0,
    val type: VideoType = VideoType.ONLINE,
    val localUri: String? = null,
    val size: Long = 0,
    val folder: String? = null,
    val dateAdded: Long = 0,
    val downloadId: Long? = null
)

enum class VideoType {
    ONLINE,
    LOCAL,
    DOWNLOADED
}
