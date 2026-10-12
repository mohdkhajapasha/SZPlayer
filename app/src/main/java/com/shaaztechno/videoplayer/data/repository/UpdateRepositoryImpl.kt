package com.shaaztechno.videoplayer.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.shaaztechno.videoplayer.data.remote.GitHubUpdateDataSource
import com.shaaztechno.videoplayer.domain.repository.UpdateRepository
import com.shaaztechno.videoplayer.domain.repository.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class UpdateRepositoryImpl(
    private val context: Context,
    private val dataSource: GitHubUpdateDataSource
) : UpdateRepository {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _downloadProgress = MutableStateFlow(0)

    override suspend fun checkForUpdates(): Result<UpdateStatus> = withContext(Dispatchers.IO) {
        dataSource.fetchUpdateInfo().map { updateInfo ->
            val currentVersionCode = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }
            } catch (e: Exception) {
                0
            }

            if (updateInfo.versionCode > currentVersionCode) {
                UpdateStatus.UpdateAvailable(updateInfo)
            } else {
                UpdateStatus.UpToDate
            }
        }
    }

    override fun getDownloadProgress(): Flow<Int> = _downloadProgress

    override suspend fun downloadApk(url: String, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            _downloadProgress.value = 0
            
            if (url.isBlank()) return@withContext Result.failure(Exception("Download URL is empty"))

            // Sanitize filename to avoid filesystem issues
            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").let {
                if (it.endsWith(".apk")) it else "$it.apk"
            }
            
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return@withContext Result.failure(Exception("Cannot access app download directory"))
            
            if (!dir.exists()) dir.mkdirs()
            
            val destinationFile = File(dir, safeFileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = try {
                DownloadManager.Request(Uri.parse(url))
                    .setTitle("SZ Player Update")
                    .setDescription("Downloading latest version...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, safeFileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    .setMimeType("application/vnd.android.package-archive")
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Invalid download URL format: ${e.message}"))
            }

            val downloadId = try {
                downloadManager.enqueue(request)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Could not start download. Please check if Download Manager is enabled in settings."))
            }

            var downloaded = false
            var resultFile: File? = null
            var errorMessage: String? = null

            var lastProgress = 0
            var lastUpdate = System.currentTimeMillis()
            val timeout = 5 * 60 * 1000L // 5 minutes of no status change before giving up

            while (!downloaded) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = try {
                    downloadManager.query(query)
                } catch (e: Exception) {
                    null
                }
                
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                    if (statusIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        
                        if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                            val bytesTotal = cursor.getLong(bytesTotalIndex)
                            
                            if (bytesTotal > 0) {
                                val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                                if (progress > lastProgress) {
                                    lastProgress = progress
                                    lastUpdate = System.currentTimeMillis()
                                }
                                _downloadProgress.value = progress.coerceIn(0, 100)
                            } else if (bytesDownloaded > 0) {
                                // Some data is coming through, reset timeout
                                lastUpdate = System.currentTimeMillis()
                            }
                        }

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloaded = true
                                resultFile = destinationFile
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloaded = true
                                val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                                errorMessage = getDetailedErrorMessage(reason)
                            }
                            DownloadManager.STATUS_PAUSED,
                            DownloadManager.STATUS_PENDING,
                            DownloadManager.STATUS_RUNNING -> {
                                // Reset timeout as long as the download is active in the system
                                lastUpdate = System.currentTimeMillis()
                            }
                        }
                    }
                    cursor.close()
                } else {
                    cursor?.close()
                    downloaded = true
                    errorMessage = "Download task was cancelled or removed by system."
                }
                
                if (!downloaded) {
                    if (System.currentTimeMillis() - lastUpdate > timeout) {
                        downloaded = true
                        errorMessage = "Download timed out due to network inactivity."
                        downloadManager.remove(downloadId)
                    } else {
                        delay(1500)
                    }
                }
            }

            if (resultFile != null && resultFile.exists()) {
                Result.success(resultFile)
            } else {
                Result.failure(Exception(errorMessage ?: "Unknown error during download"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDetailedErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space to download the update."
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "SD Card or external storage is unavailable."
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network data error. Please check your connection."
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Server redirection loop. Please contact support."
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server returned an unexpected response."
            DownloadManager.ERROR_FILE_ERROR -> "Storage access error. Try clearing app cache."
            DownloadManager.ERROR_CANNOT_RESUME -> "Download cannot be resumed."
            else -> "Download failed (Error Code: $reason). Please check your internet connection."
        }
    }
}
