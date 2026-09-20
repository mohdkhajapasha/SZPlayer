package com.shaaztechno.videoplayer.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.model.WhatsAppStatus
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import com.shaaztechno.videoplayer.domain.repository.VideoStorageRepository
import com.shaaztechno.videoplayer.domain.repository.WhatsAppStatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class WhatsAppStatusRepositoryImpl(
    private val context: Context,
    private val videoStorageRepository: VideoStorageRepository,
    private val videoRepository: VideoRepository
) : WhatsAppStatusRepository {

    private val _statuses = MutableStateFlow<List<WhatsAppStatus>>(emptyList())

    override fun getStatuses(): Flow<List<WhatsAppStatus>> = _statuses

    override suspend fun fetchStatuses() {
        withContext(Dispatchers.IO) {
            val statusList = mutableListOf<WhatsAppStatus>()
            
            val roots = listOf(
                Environment.getExternalStorageDirectory()
            )

            val relativePaths = listOf(
                "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                "WhatsApp/Media/.Statuses",
                "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                "WhatsApp Business/Media/.Statuses"
            )

            for (root in roots) {
                for (relPath in relativePaths) {
                    val folder = File(root, relPath)
                    if (folder.exists() && folder.isDirectory) {
                        folder.listFiles()?.forEach { file ->
                            if (file.isFile && isMediaFile(file)) {
                                val isVideo = file.extension.lowercase() == "mp4"
                                var duration = 0L
                                if (isVideo) {
                                    duration = getVideoDuration(file)
                                }
                                statusList.add(
                                    WhatsAppStatus(
                                        id = file.absolutePath,
                                        uri = Uri.fromFile(file),
                                        name = file.name,
                                        isVideo = isVideo,
                                        duration = duration,
                                        size = file.length(),
                                        lastModified = file.lastModified()
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            _statuses.value = statusList.sortedByDescending { it.lastModified }
        }
    }

    private fun isMediaFile(file: File): Boolean {
        val extensions = listOf("mp4", "jpg", "jpeg", "png", "gif")
        return extensions.contains(file.extension.lowercase())
    }

    private fun getVideoDuration(file: File): Long {
        var duration = 0L
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
        return duration
    }

    override suspend fun saveStatus(status: WhatsAppStatus): Result<Unit> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val typePrefix = if (status.isVideo) "SZ_WhatsApp_Status" else "SZ_WhatsApp_Image"
        val fileName = "${typePrefix}_$timestamp"
        val mimeType = if (status.isVideo) "video/mp4" else "image/jpeg"
        val relativePath = if (status.isVideo) "Movies/SZ Player/" else "Pictures/SZ Player/"

        videoStorageRepository.saveVideoToMediaStore(
            sourceUri = status.uri,
            fileName = fileName,
            mimeType = mimeType,
            relativePath = relativePath
        ).map { uri ->
            // Add to library if it's a video
            if (status.isVideo) {
                val video = Video(
                    id = uri.toString(),
                    title = "WhatsApp Status $timestamp",
                    url = uri.toString(),
                    type = VideoType.DOWNLOADED,
                    localUri = uri.toString(),
                    size = status.size,
                    duration = status.duration,
                    dateAdded = timestamp
                )
                videoRepository.addVideo(video)
            }
            Unit
        }
    }

    override suspend fun shareStatus(status: WhatsAppStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (status.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, status.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Status")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
