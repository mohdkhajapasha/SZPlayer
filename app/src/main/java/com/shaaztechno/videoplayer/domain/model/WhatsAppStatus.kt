package com.shaaztechno.videoplayer.domain.model

import android.net.Uri

data class WhatsAppStatus(
    val id: String,
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val duration: Long = 0,
    val size: Long = 0,
    val lastModified: Long = 0
)
