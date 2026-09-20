package com.washingtonpost.android.follow.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.washingtonpost.android.follow.database.dao.AuthorDao
import com.washingtonpost.android.follow.database.dao.FollowDao
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.database.model.FollowEntity

@Database(entities = [AuthorEntity::class, FollowEntity::class], version = 2, exportSchema = false)
abstract class FollowDatabase : RoomDatabase() {
    abstract fun authorDao(): AuthorDao

    abstract fun followDao(): FollowDao

    companion object {
        @Volatile
        private var INSTANCE: FollowDatabase? = null

        fun getInstance(context: Context): FollowDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        FollowDatabase::class.java, "FollowDatabase.db")
                        .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                        .addMigrations(MIGRATION_1_2)
                        .build()
    }
}

/***
 * Adding Migration rules for the Follow database MIGRATION_1_2
 * As part of the new version = 2 , Added (isFollowing,isSynced & isAuthorMetaDataAvailable )
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("PRAGMA foreign_keys = OFF")
        database.execSQL("ALTER TABLE FOLLOWENTITY RENAME TO FOLLOWENTITY_OLD")
        database.execSQL("CREATE TABLE FOLLOWENTITY (`author_id` TEXT NOT NULL, `last_modified` INTEGER NOT NULL, `isFollowing` INTEGER DEFAULT 1, `isSynced` INTEGER DEFAULT 0, `isAuthorMetaDataAvailable` INTEGER DEFAULT 1,  PRIMARY KEY(`author_id`))")
        database.execSQL("INSERT INTO FOLLOWENTITY (author_id, last_modified, isFollowing, isSynced, isAuthorMetaDataAvailable)  SELECT A.author_id, A.last_modified, 1, 0, 1 FROM FOLLOWENTITY_OLD as A")
        database.execSQL("DROP TABLE FOLLOWENTITY_OLD")
        database.execSQL("CREATE INDEX index_FollowEntity_last_modified ON  FOLLOWENTITY(last_modified)")
        database.execSQL("PRAGMA foreign_keys = ON")
    }
}