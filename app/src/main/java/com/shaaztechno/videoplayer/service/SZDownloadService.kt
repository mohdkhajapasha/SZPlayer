package com.shaaztechno.videoplayer.service

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.shaaztechno.videoplayer.R
import com.shaaztechno.videoplayer.data.downloader.SZDownloadManager

@OptIn(UnstableApi::class)
class SZDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.download_channel_name,
    0
) {

    private val notificationHelper by lazy {
        DownloadNotificationHelper(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return SZDownloadManager.getInstance(this).downloadManager
    }

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): Notification {
        return notificationHelper.buildProgressNotification(
            this,
            R.drawable.sz_player_icon,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "sz_player_downloads_channel"
    }
}
