package com.shaaztechno.videoplayer.data.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.domain.repository.VideoRepository
import java.util.UUID

class VideoDownloader(
    private val context: Context,
    private val repository: VideoRepository
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    suspend fun downloadVideo(video: Video) {
        val request = DownloadManager.Request(Uri.parse(video.url))
            .setTitle(video.title)
            .setDescription("Downloading video...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, "${video.title}.mp4")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        
        val downloadedVideo = video.copy(
            id = UUID.randomUUID().toString(),
            type = VideoType.DOWNLOADED,
            localUri = null,
            downloadId = downloadId
        )
        repository.addVideo(downloadedVideo)
    }
}
