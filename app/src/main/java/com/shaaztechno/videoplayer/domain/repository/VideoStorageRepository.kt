package com.shaaztechno.videoplayer.domain.repository

import android.net.Uri
import java.io.InputStream

interface VideoStorageRepository {
    suspend fun saveVideoToMediaStore(
        sourceUri: Uri,
        fileName: String,
        mimeType: String,
        relativePath: String = "Movies/SZ Player/"
    ): Result<Uri>

    suspend fun saveStreamToMediaStore(
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        relativePath: String = "Movies/SZ Player/"
    ): Result<Uri>

    suspend fun deleteVideoFromMediaStore(uri: Uri): Result<Unit>
    
    fun getVideoUri(id: String): Uri?
}
