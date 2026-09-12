package com.shaaztechno.videoplayer.domain.model

data class VideoMediaInfo(
    val url: String,
    val title: String,
    val mediaType: String, // MP4, WebM, HLS Stream, DASH Stream, etc.
    val mimeType: String? = null,
    val fileSize: Long? = null, // In bytes, null if unknown/variable
    val duration: Long? = null, // In ms, null if unknown
    val isPlayable: Boolean = true,
    val isDownloadable: Boolean = true
)
