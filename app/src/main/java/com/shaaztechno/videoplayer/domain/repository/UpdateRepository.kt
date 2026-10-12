package com.shaaztechno.videoplayer.domain.repository

import com.shaaztechno.videoplayer.domain.model.UpdateInfo
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    suspend fun checkForUpdates(): Result<UpdateStatus>
    fun getDownloadProgress(): Flow<Int>
    suspend fun downloadApk(url: String, fileName: String): Result<java.io.File>
}

sealed class UpdateStatus {
    object UpToDate : UpdateStatus()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateStatus()
}
