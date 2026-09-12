package com.shaaztechno.videoplayer.data.remote

import com.shaaztechno.videoplayer.data.local.entity.VideoEntity
import com.shaaztechno.videoplayer.domain.model.VideoType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

class GoogleSheetParser(private val client: OkHttpClient) {

    suspend fun fetchCatalog(url: String): List<VideoEntity> {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return emptyList()

        val videos = mutableListOf<VideoEntity>()
        val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
        
        // Skip header
        val header = reader.readLine() ?: return emptyList()
        val columns = header.split(",").map { it.trim().lowercase() }
        
        val idIdx = columns.indexOf("id")
        val titleIdx = columns.indexOf("title")
        val descIdx = columns.indexOf("description")
        val urlIdx = columns.indexOf("videourl")
        val thumbIdx = columns.indexOf("thumbnailurl")
        val categoryIdx = columns.indexOf("category")
        val durationIdx = columns.indexOf("duration")

        reader.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(",")
                if (parts.size > urlIdx && urlIdx != -1) {
                    val id = if (idIdx != -1 && idIdx < parts.size) parts[idIdx] else parts[urlIdx].hashCode().toString()
                    val title = if (titleIdx != -1 && titleIdx < parts.size) parts[titleIdx] else "Unknown Title"
                    val videoUrl = parts[urlIdx]
                    
                    if (videoUrl.isNotBlank()) {
                        videos.add(
                            VideoEntity(
                                id = id,
                                title = title,
                                url = videoUrl,
                                thumbnailUrl = if (thumbIdx != -1 && thumbIdx < parts.size) parts[thumbIdx] else null,
                                description = if (descIdx != -1 && descIdx < parts.size) parts[descIdx] else null,
                                category = if (categoryIdx != -1 && categoryIdx < parts.size) parts[categoryIdx] else null,
                                duration = if (durationIdx != -1 && durationIdx < parts.size) parts[durationIdx].toLongOrNull() ?: 0L else 0L,
                                type = VideoType.ONLINE.name,
                                localUri = null,
                                size = 0,
                                folder = null,
                                dateAdded = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
        return videos
    }
}
