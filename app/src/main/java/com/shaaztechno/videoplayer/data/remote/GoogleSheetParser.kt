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
        
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                
                val body = response.body ?: return emptyList()
                val videos = mutableListOf<VideoEntity>()
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                
                // Skip header
                val header = reader.readLine() ?: return emptyList()
                val columns = parseCsvLine(header).map { it.trim().lowercase() }
                
                val idIdx = columns.indexOf("id")
                val titleIdx = columns.indexOf("title")
                val descIdx = columns.indexOf("description")
                val urlIdx = columns.indexOf("videourl")
                val thumbIdx = columns.indexOf("thumbnailurl")
                val categoryIdx = columns.indexOf("category")
                val durationIdx = columns.indexOf("duration")

                reader.forEachLine { line ->
                    val parts = parseCsvLine(line)
                    if (parts.size > urlIdx && urlIdx != -1) {
                        val videoUrl = parts[urlIdx]
                        if (videoUrl.isNotBlank()) {
                            val id = if (idIdx != -1 && idIdx < parts.size && parts[idIdx].isNotBlank()) {
                                parts[idIdx]
                            } else {
                                videoUrl.hashCode().toString()
                            }
                            val title = if (titleIdx != -1 && titleIdx < parts.size) parts[titleIdx] else "Unknown Title"
                            
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
                                    folder = "REMOTE_CATALOG", // Use folder field to distinguish from user-added ONLINE videos
                                    dateAdded = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                videos
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        cur.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cur.append(c)
                }
            } else {
                if (c == '\"') {
                    inQuotes = true
                } else if (c == ',') {
                    result.add(cur.toString())
                    cur = StringBuilder()
                } else {
                    cur.append(c)
                }
            }
            i++
        }
        result.add(cur.toString())
        return result
    }
}
