package com.shaaztechno.videoplayer.domain.repository

import com.shaaztechno.videoplayer.domain.model.WhatsAppStatus
import kotlinx.coroutines.flow.Flow

interface WhatsAppStatusRepository {
    fun getStatuses(): Flow<List<WhatsAppStatus>>
    suspend fun fetchStatuses()
    suspend fun saveStatus(status: WhatsAppStatus): Result<Unit>
    suspend fun shareStatus(status: WhatsAppStatus): Result<Unit>
}
