/* Copyright (c) 2019 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.washingtonpost.android.save.database.dao.SavedArticleDao
import com.washingtonpost.android.save.database.model.ArticleListQueue
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.android.save.misc.ArticleListQueueType

@Database(entities = [SavedArticleModel::class, MetadataModel::class, ArticleListQueue::class], version = SavedArticleDB.DB_VERSION)
@TypeConverters(ArticleListQueueType::class)
abstract class SavedArticleDB : RoomDatabase() {
    abstract fun articleItemModel() : SavedArticleDao

    companion object {
        const val DB_NAME = "saved_article_db"
        const val DB_VERSION = 7
        private var INSTANCE : SavedArticleDB? = null

        @JvmStatic
        fun getInstance(context: Context): SavedArticleDB =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Room.databaseBuilder(context.applicationContext, SavedArticleDB::class.java, DB_NAME)
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .build().also {
                        INSTANCE = it
                    }
                }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE MetadataModel ADD COLUMN secondaryText TEXT")
        database.execSQL("ALTER TABLE MetadataModel ADD COLUMN displayLabel TEXT")
        database.execSQL("ALTER TABLE MetadataModel ADD COLUMN displayTransparency TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE NewMetadataModel (
                headline TEXT,
                byline TEXT,
                blurb TEXT,
                imageURL TEXT,
                canonicalURL TEXT,
                lastUpdated INTEGER,
                publishedTime INTEGER,
                secondaryText TEXT,
                displayLabel TEXT,
                displayTransparency TEXT,
                contentURL TEXT NOT NULL,
                syncLmt INTEGER NOT NULL,
                articleListType INTEGER NOT NULL,
                PRIMARY KEY(contentURL, articleListType)
            )
        """)

        database.execSQL("""
            INSERT INTO NewMetadataModel (
                headline,
                byline,
                blurb,
                imageURL,
                canonicalURL,
                lastUpdated,
                publishedTime,
                secondaryText,
                displayLabel,
                displayTransparency,
                contentURL,
                syncLmt,
                articleListType
            )
            SELECT headline, byline, blurb, imageURL, canonicalURL, lastUpdated, publishedTime, secondaryText, displayLabel, displayTransparency, sam.contentURL, syncLmt, sam.articleListType FROM MetadataModel JOIN SavedArticleModel sam USING(contentURL)
        """)

        database.execSQL("DROP TABLE MetadataModel")

        database.execSQL("CREATE INDEX index_MetadataModel_canonicalURL ON NewMetadataModel (canonicalURL)")

        database.execSQL("CREATE INDEX index_MetadataModel_syncLmt ON NewMetadataModel (syncLmt)")

        database.execSQL("ALTER TABLE NewMetadataModel RENAME TO MetadataModel")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE MetadataModel ADD COLUMN trackingString TEXT")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE MetadataModel ADD COLUMN headlinePrefix TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE SavedArticleModel ADD COLUMN isListened INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `NewMetadataModel` (
                `contentURL` TEXT NOT NULL,
                `syncLmt` INTEGER NOT NULL,
                `headline` TEXT,
                `byline` TEXT,
                `blurb` TEXT,
                `imageURL` TEXT,
                `canonicalURL` TEXT,
                `lastUpdated` INTEGER,
                `publishedTime` INTEGER,
                `secondaryText` TEXT,
                `displayLabel` TEXT,
                `displayTransparency` TEXT,
                `trackingString` TEXT,
                `headlinePrefix` TEXT,
                PRIMARY KEY(`contentURL`)
            )
        """
        )

        database.execSQL(
            """
            INSERT OR REPLACE INTO NewMetadataModel (
              contentURL, syncLmt, headline, byline, blurb, imageURL,
              canonicalURL, lastUpdated, publishedTime, secondaryText,
              displayLabel, displayTransparency, trackingString, headlinePrefix
            )
            SELECT m.contentURL, m.syncLmt, m.headline, m.byline, m.blurb, m.imageURL,
                   m.canonicalURL, m.lastUpdated, m.publishedTime, m.secondaryText,
                   m.displayLabel, m.displayTransparency, m.trackingString, m.headlinePrefix
            FROM MetadataModel m
            JOIN SavedArticleModel s
              ON m.contentURL = s.contentURL
            WHERE s.articleListType = 'SAVED_STORIES';
        """
        )

        database.execSQL("DROP TABLE `MetadataModel`")
        database.execSQL("ALTER TABLE `NewMetadataModel` RENAME TO `MetadataModel`")

        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_MetadataModel_syncLmt` ON `MetadataModel` (`syncLmt`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_MetadataModel_canonicalURL` ON `MetadataModel` (`canonicalURL`)""")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `NewSavedArticleModel` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contentURL` TEXT NOT NULL,
                `lmt` INTEGER NOT NULL,
                `isListened` INTEGER NOT NULL DEFAULT 0
            )
        """
        )

        database.execSQL("""
            INSERT OR REPLACE INTO NewSavedArticleModel (id, contentURL, lmt, isListened)
            SELECT sam.id, sam.contentURL, sam.lmt, sam.isListened
            FROM SavedArticleModel AS sam
            JOIN (
              SELECT contentURL, MAX(lmt) AS max_lmt
              FROM SavedArticleModel
              WHERE articleListType = 'SAVED_STORIES'
              GROUP BY contentURL
            ) AS latest
              ON sam.contentURL = latest.contentURL
             AND sam.lmt = latest.max_lmt
            WHERE sam.articleListType = 'SAVED_STORIES';
        """)

        database.execSQL("DROP TABLE `SavedArticleModel`")
        database.execSQL("ALTER TABLE `NewSavedArticleModel` RENAME TO `SavedArticleModel`")

        database.execSQL("""CREATE INDEX IF NOT EXISTS index_SavedArticleModel_lmt ON SavedArticleModel(lmt)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS index_SavedArticleModel_contentURL ON SavedArticleModel(contentURL)""")

        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `NewArticleListQueue` (
                    `contentURL` TEXT NOT NULL,
                    `lmt` INTEGER NOT NULL,
                    `articleListQueueType` INTEGER NOT NULL,
                    PRIMARY KEY(`contentURL`, `articleListQueueType`)
                )
            """
        )

        database.execSQL("""
            INSERT OR REPLACE INTO NewArticleListQueue (contentURL, lmt, articleListQueueType)
            SELECT contentURL, lmt, articleListQueueType
            FROM ArticleListQueue
        """)

        database.execSQL("DROP TABLE `ArticleListQueue`")
        database.execSQL("ALTER TABLE `NewArticleListQueue` RENAME TO `ArticleListQueue`")

        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_ArticleListQueue_lmt` ON `ArticleListQueue` (`lmt`)""")
    }
}