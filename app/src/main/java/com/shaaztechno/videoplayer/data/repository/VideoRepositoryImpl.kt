package com.shaaztechno.videoplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.shaaztechno.videoplayer.data.local.dao.HistoryDao
import com.shaaztechno.videoplayer.data.local.dao.VideoDao
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.data.local.entity.toEntity
import com.shaaztechno.videoplayer.data.remote.GoogleSheetParser
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import com.shaaztechno.videoplayer.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VideoRepositoryImpl(
    private val videoDao: VideoDao,
    private val historyDao: HistoryDao,
    private val googleSheetParser: GoogleSheetParser,
    private val context: Context
) : VideoRepository {

    override fun getAllVideos(): Flow<List<Video>> {
        return videoDao.getAllVideos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getVideosByType(type: VideoType): Flow<List<Video>> {
        return videoDao.getVideosByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVideoById(id: String): Video? {
        return videoDao.getVideoById(id)?.toDomain()
    }

    override suspend fun refreshOnlineCatalog() {
        withContext(Dispatchers.IO) {
            try {
                val videos = googleSheetParser.fetchCatalog(Config.GOOGLE_SHEET_CSV_URL)
                if (videos.isNotEmpty()) {
                    videoDao.clearOnlineCatalog()
                    videoDao.insertVideos(videos)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun refreshLocalVideos() {
        withContext(Dispatchers.IO) {
            val localVideos = mutableListOf<Video>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val duration = it.getLong(durationColumn)
                    val size = it.getLong(sizeColumn)
                    val date = it.getLong(dateColumn)
                    val folder = it.getString(bucketColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    localVideos.add(
                        Video(
                            id = id.toString(),
                            title = name,
                            url = contentUri,
                            duration = duration,
                            type = VideoType.LOCAL,
                            localUri = contentUri,
                            size = size,
                            dateAdded = date,
                            folder = folder
                        )
                    )
                }
            }
            videoDao.insertVideos(localVideos.map { it.toEntity() })
        }
    }

    override suspend fun addVideo(video: Video) {
        videoDao.insertVideo(video.toEntity())
    }

    override suspend fun deleteVideo(id: String) {
        videoDao.deleteVideo(id)
    }

    override fun getPlaybackHistory(): Flow<List<HistoryEntity>> {
        return historyDao.getHistory()
    }

    override suspend fun getHistoryForVideo(videoId: String): HistoryEntity? {
        return historyDao.getHistoryForVideo(videoId)
    }

    override suspend fun updateHistory(
        video: Video,
        position: Long,
        duration: Long
    ) {
        historyDao.insertHistory(
            HistoryEntity(
                videoId = video.id,
                title = video.title,
                url = video.url,
                type = video.type.name,
                thumbnailUrl = video.thumbnailUrl,
                lastPosition = position,
                duration = duration,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
