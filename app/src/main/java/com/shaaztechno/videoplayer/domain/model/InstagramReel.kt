package com.shaaztechno.videoplayer.domain.model

data class InstagramReel(
    val videoUrl: String,
    val thumbnailUrl: String,
    val caption: String?,
    val username: String?
)
