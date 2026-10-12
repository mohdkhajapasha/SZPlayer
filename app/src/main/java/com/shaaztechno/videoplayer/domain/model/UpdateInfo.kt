package com.shaaztechno.videoplayer.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UpdateInfo(
    @SerializedName("versionCode")
    val versionCode: Int,
    @SerializedName("versionName")
    val versionName: String,
    @SerializedName("releaseNotes")
    val releaseNotes: List<String>,
    @SerializedName("apkUrl")
    val apkUrl: String,
    @SerializedName("forceUpdate")
    val forceUpdate: Boolean
)
