package com.shaaztechno.videoplayer.data.downloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shaaztechno.videoplayer.SZPlayerApplication
import com.shaaztechno.videoplayer.domain.model.VideoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != -1L && context != null) {
                val pendingResult = goAsync()
                val app = context.applicationContext as SZPlayerApplication
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val query = DownloadManager.Query().setFilterById(id)
                        val cursor = downloadManager.query(query)
                        
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                    
                                    val videoEntity = app.database.videoDao().getVideoByDownloadId(id)
                                    if (videoEntity != null) {
                                        val updatedVideo = videoEntity.copy(
                                            localUri = localUri,
                                            type = VideoType.DOWNLOADED.name
                                        )
                                        app.database.videoDao().insertVideo(updatedVideo)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
