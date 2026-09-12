package com.shaaztechno.videoplayer.data.local.dao

import androidx.room.*
import com.shaaztechno.videoplayer.data.local.entity.PlaylistEntity
import com.shaaztechno.videoplayer.data.local.entity.PlaylistItemEntity
import com.shaaztechno.videoplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String)

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN playlist_items pi ON v.id = pi.videoId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.orderIndex ASC
    """)
    fun getVideosInPlaylist(playlistId: Long): Flow<List<VideoEntity>>
}
