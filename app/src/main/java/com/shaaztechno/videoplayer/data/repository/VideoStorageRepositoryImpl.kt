package com.shaaztechno.videoplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.shaaztechno.videoplayer.domain.repository.VideoStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class VideoStorageRepositoryImpl(
    private val context: Context
) : VideoStorageRepository {

    override suspend fun saveVideoToMediaStore(
        sourceUri: Uri,
        fileName: String,
        mimeType: String,
        relativePath: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                saveStreamToMediaStore(inputStream, fileName, mimeType, relativePath)
            } ?: Result.failure(Exception("Failed to open input stream from URI"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveStreamToMediaStore(
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        relativePath: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val isVideo = mimeType.startsWith("video/", ignoreCase = true)
            
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
            } else {
                if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: if (isVideo) "mp4" else "jpg"
            val finalFileName = if (fileName.contains(".")) fileName else "$fileName.$extension"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val itemUri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            try {
                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                } ?: throw Exception("Failed to open output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                }
                
                Result.success(itemUri)
            } catch (e: Exception) {
                resolver.delete(itemUri, null, null)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteVideoFromMediaStore(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete from MediaStore or not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getVideoUri(id: String): Uri? {
        return try {
            Uri.parse(id)
        } catch (e: Exception) {
            null
        }
    }
}
