package com.shaaztechno.videoplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE videoId = :videoId")
    suspend fun getHistoryForVideo(videoId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun deleteHistory(videoId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    /**
     * Keeps only the [limit] most-recent records, deleting any excess in a single SQL statement
     * instead of N individual deletes.
     */
    @Query(
        "DELETE FROM playback_history WHERE videoId NOT IN " +
            "(SELECT videoId FROM playback_history ORDER BY timestamp DESC LIMIT :limit)"
    )
    suspend fun trimHistory(limit: Int)
}
