package com.shaaztechno.videoplayer.data.remote

import com.google.gson.Gson
import com.shaaztechno.videoplayer.domain.model.UpdateInfo
import com.shaaztechno.videoplayer.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GitHubUpdateDataSource(private val client: OkHttpClient) {

    suspend fun fetchUpdateInfo(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${Config.UPDATE_JSON_URL}?ts=${System.currentTimeMillis()}")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "SZPlayer-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch update info: ${response.code}"))
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                val updateInfo = try {
                    Gson().fromJson(body, UpdateInfo::class.java)
                } catch (e: Exception) {
                    null
                }
                
                if (updateInfo == null || updateInfo.apkUrl.isBlank()) {
                    return@withContext Result.failure(Exception("Invalid update information"))
                }
                
                Result.success(updateInfo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
