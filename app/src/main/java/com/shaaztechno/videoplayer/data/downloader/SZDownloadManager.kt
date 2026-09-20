package com.shaaztechno.videoplayer.data.downloader

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.shaaztechno.videoplayer.SZPlayerApplication
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import com.shaaztechno.videoplayer.domain.repository.VideoStorageRepository
import com.shaaztechno.videoplayer.service.SZDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.Executors

data class DownloadProgressItem(
    val id: String,
    val url: String,
    val title: String,
    val state: Int, // Download.STATE_*
    val percentage: Float, // 0f to 100f
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isCompleted: Boolean,
    val isFailed: Boolean,
    val isPaused: Boolean,
    val isDownloading: Boolean
)

@OptIn(UnstableApi::class)
class SZDownloadManager private constructor(
    private val context: Context,
    private val videoRepository: VideoRepository,
    private val videoStorageRepository: VideoStorageRepository,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
    val downloadCache: Cache = run {
        val downloadDirectory = File(context.getExternalFilesDir(null) ?: context.filesDir, "sz_downloads")
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs()
        }
        SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider)
    }

    val httpDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent("SZPlayer/1.0 (Linux; Android)")
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        .setAllowCrossProtocolRedirects(true)

    val defaultDataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    val cacheDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(defaultDataSourceFactory)
        .setCacheWriteDataSinkFactory(null)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    val downloadManager: DownloadManager = DownloadManager(
        context,
        databaseProvider,
        downloadCache,
        httpDataSourceFactory,
        Executors.newFixedThreadPool(3)
    ).apply {
        maxParallelDownloads = 2
    }

    private val _downloads = MutableStateFlow<List<DownloadProgressItem>>(emptyList())
    val downloads: StateFlow<List<DownloadProgressItem>> = _downloads.asStateFlow()

    private var progressPollingJob: Job? = null

    init {
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                updateDownloadList()
                if (download.state == Download.STATE_COMPLETED) {
                    processCompletedDownload(download)
                }
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                updateDownloadList()
            }

            override fun onIdle(downloadManager: DownloadManager) {
                updateDownloadList()
            }
        })

        updateDownloadList()
        startPollingProgress()
    }

    private fun startPollingProgress() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (isActive) {
                val hasActive = _downloads.value.any { it.isDownloading }
                if (hasActive) {
                    updateDownloadList()
                }
                delay(800)
            }
        }
    }

    @Synchronized
    private fun updateDownloadList() {
        val items = mutableListOf<DownloadProgressItem>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        try {
            while (cursor.moveToNext()) {
                val download = cursor.download
                val title = try {
                    if (download.request.data.isNotEmpty()) {
                        String(download.request.data, Charsets.UTF_8)
                    } else {
                        download.request.id
                    }
                } catch (e: Exception) {
                    download.request.id
                }

                val percentage = if (download.percentDownloaded >= 0) {
                    download.percentDownloaded
                } else {
                    0f
                }

                items.add(
                    DownloadProgressItem(
                        id = download.request.id,
                        url = download.request.uri.toString(),
                        title = title,
                        state = download.state,
                        percentage = percentage,
                        bytesDownloaded = download.bytesDownloaded,
                        totalBytes = download.contentLength,
                        isCompleted = download.state == Download.STATE_COMPLETED,
                        isFailed = download.state == Download.STATE_FAILED,
                        isPaused = download.state == Download.STATE_STOPPED,
                        isDownloading = download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        _downloads.value = items
    }

    private fun processCompletedDownload(download: Download) {
        scope.launch {
            val title = try {
                if (download.request.data.isNotEmpty()) {
                    String(download.request.data, Charsets.UTF_8)
                } else {
                    download.request.id
                }
            } catch (e: Exception) {
                "Downloaded Video"
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "SZ_${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}_$timestamp"
            val mimeType = download.request.mimeType ?: "video/mp4"

            saveUrlToMediaStore(download.request.uri.toString(), fileName, mimeType, download.request.id, title)
            
            cancelDownload(download.request.id)
        }
    }

    private suspend fun saveUrlToMediaStore(
        url: String,
        fileName: String,
        mimeType: String,
        originalId: String,
        title: String
    ) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return
                
                val body = response.body ?: return
                val detectedMimeType = body.contentType()?.let { "${it.type}/${it.subtype}" } ?: mimeType
                
                val tempFile = File(context.cacheDir, "temp_download_${System.currentTimeMillis()}.mp4")
                tempFile.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }

                val result = videoStorageRepository.saveVideoToMediaStore(
                    sourceUri = Uri.fromFile(tempFile),
                    fileName = fileName,
                    mimeType = detectedMimeType,
                    relativePath = "Movies/SZ Player/"
                )

                result.onSuccess { uri ->
                    val video = Video(
                        id = uri.toString(),
                        title = title,
                        url = uri.toString(),
                        type = VideoType.DOWNLOADED,
                        localUri = uri.toString(),
                        size = tempFile.length(),
                        dateAdded = System.currentTimeMillis()
                    )
                    videoRepository.addVideo(video)
                    tempFile.delete()
                }.onFailure {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDownload(id: String, url: String, title: String, mimeType: String?) {
        val uri = Uri.parse(url)
        val downloadRequest = DownloadRequest.Builder(id, uri)
            .setMimeType(mimeType)
            .setData(title.toByteArray(Charsets.UTF_8))
            .build()

        DownloadService.sendAddDownload(
            context,
            SZDownloadService::class.java,
            downloadRequest,
            /* foreground = */ true
        )
    }

    fun pauseDownload(id: String) {
        DownloadService.sendSetStopReason(
            context,
            SZDownloadService::class.java,
            id,
            Download.STOP_REASON_NONE + 1,
            /* foreground = */ false
        )
    }

    fun resumeDownload(id: String) {
        DownloadService.sendSetStopReason(
            context,
            SZDownloadService::class.java,
            id,
            Download.STOP_REASON_NONE,
            /* foreground = */ false
        )
    }

    fun cancelDownload(id: String) {
        DownloadService.sendRemoveDownload(
            context,
            SZDownloadService::class.java,
            id,
            /* foreground = */ false
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SZDownloadManager? = null

        fun getInstance(
            context: Context,
            videoRepository: VideoRepository,
            videoStorageRepository: VideoStorageRepository,
            okHttpClient: OkHttpClient
        ): SZDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SZDownloadManager(
                    context.applicationContext,
                    videoRepository,
                    videoStorageRepository,
                    okHttpClient
                ).also { INSTANCE = it }
            }
        }

        fun getInstance(context: Context): SZDownloadManager {
            val app = context.applicationContext as SZPlayerApplication
            return getInstance(context, app.videoRepository, app.videoStorageRepository, app.httpClient)
        }
    }
}
