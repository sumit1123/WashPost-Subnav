package com.wapo.flagship.features.newsletter.repo.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wapo.flagship.features.newsletter.repo.local.dao.NewslettersDao
import com.wapo.flagship.features.newsletter.repo.local.model.NewslettersEntity

@Database(entities = [NewslettersEntity::class], version = 2)
abstract class NewslettersDatabase : RoomDatabase() {
    abstract fun newslettersDao(): NewslettersDao

    companion object {
        /**
         * Adding Migration rules for the NewslettersDatabase MIGRATION_1_2
         * As part of the new version = 2, Added key & list, and made id optional in NewslettersEntity
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE NEWSLETTERSENTITY RENAME TO NEWSLETTERSENTITY_OLD")
                db.execSQL("""
                    CREATE TABLE NEWSLETTERSENTITY (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        `id` TEXT,
                        `list` TEXT,
                        `last_modified` INTEGER NOT NULL,
                        `isEnrolled` INTEGER DEFAULT 1,
                        `isSynced` INTEGER DEFAULT 0
                    ) 
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO NEWSLETTERSENTITY (key, id, list, last_modified, isEnrolled, isSynced)
                    SELECT id AS key, id, NULL AS list, last_modified, isEnrolled, isSynced
                    FROM NEWSLETTERSENTITY_OLD
                """.trimIndent())
                db.execSQL("DROP TABLE NEWSLETTERSENTITY_OLD")
                db.execSQL("CREATE INDEX index_NewslettersEntity_last_modified ON  NEWSLETTERSENTITY(last_modified)")
            }
        }
    }
}
