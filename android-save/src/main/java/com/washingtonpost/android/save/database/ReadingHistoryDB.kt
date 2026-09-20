package com.washingtonpost.android.save.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.washingtonpost.android.save.database.dao.ReadingHistoryDao
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryModel

@Database(
    entities = [ReadingHistoryModel::class, MetadataModel::class],
    version = ReadingHistoryDB.DB_VERSION
)
abstract class ReadingHistoryDB : RoomDatabase() {
    abstract fun readingHistoryItemModel(): ReadingHistoryDao

    companion object {
        const val DB_NAME = "reading_history_db"
        const val DB_VERSION = 1
        private var INSTANCE: ReadingHistoryDB? = null

        @JvmStatic
        fun getInstance(context: Context): ReadingHistoryDB =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReadingHistoryDB::class.java,
                    DB_NAME
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build().also {
                        INSTANCE = it
                    }
            }
    }
}