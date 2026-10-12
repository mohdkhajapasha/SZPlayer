package com.shaaztechno.videoplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shaaztechno.videoplayer.data.local.dao.HistoryDao
import com.shaaztechno.videoplayer.data.local.dao.PlaylistDao
import com.shaaztechno.videoplayer.data.local.dao.VideoDao
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.data.local.entity.PlaylistEntity
import com.shaaztechno.videoplayer.data.local.entity.PlaylistItemEntity
import com.shaaztechno.videoplayer.data.local.entity.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        HistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SZPlayerDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: SZPlayerDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "playback_history", "mimeType", "TEXT")
                addColumnIfNotExists(db, "videos", "mimeType", "TEXT")
            }
        }

        private fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnType: String
        ) {
            val cursor = db.query("PRAGMA table_info(`$tableName`)")
            var exists = false
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (it.moveToNext()) {
                        if (it.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                            exists = true
                            break
                        }
                    }
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnType")
            }
        }

        fun getDatabase(context: Context): SZPlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SZPlayerDatabase::class.java,
                    "sz_player_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
