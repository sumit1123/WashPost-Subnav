package com.wapo.flagship.features.amazonunification.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wapo.flagship.data.CacheMetadataDb
import com.wapo.flagship.features.amazonunification.MigrationHelper
import com.wapo.flagship.features.amazonunification.database.dao.UserPreferenceEntryDao
import com.wapo.flagship.features.amazonunification.database.model.UserPreferenceEntry

/**
 * RoomDatabase class for Rainbow's [CacheMetadataDb.Name] to migrate data to Classic.
 */
@Database(entities = [UserPreferenceEntry::class], version = 30, exportSchema = false)
abstract class RainbowAppDatabase : RoomDatabase() {
    abstract fun userPreferenceEntryDao(): UserPreferenceEntryDao

    companion object {
        @Volatile
        private var INSTANCE: RainbowAppDatabase? = null

        // Manual migration is required to make [UserPreferenceEntry] Entity's Primary key(fullUrl) as notNull.
        // Rainbow's SQLiteOpenHelper is not having a notNull constraint on the Primary Key that is not
        // allowing automatic data migration.
        private val MIGRATION_29_30 =
            object : Migration(29, 30) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.apply {
                        execSQL(
                            "CREATE TABLE `UserPreferenceEntryTemp` (`fullUrl` TEXT NOT NULL PRIMARY KEY, `hash` INTEGER NOT NULL DEFAULT 0, `localFilePath` TEXT, `binary` BLOB, `type` INTEGER, `savedType` INTEGER, `popularity` INTEGER DEFAULT 0, `last_touched` INTEGER)",
                        )
                        execSQL(
                            "INSERT OR IGNORE INTO UserPreferenceEntryTemp SELECT * FROM UserPreferenceEntry",
                        )
                        execSQL("DROP TABLE UserPreferenceEntry")
                        execSQL("ALTER TABLE UserPreferenceEntryTemp RENAME TO UserPreferenceEntry")
                    }
                }
            }

        fun getInstance(context: Context): RainbowAppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    RainbowAppDatabase::class.java,
                    MigrationHelper.rainbowMetadataDbNewName,
                ).addMigrations(MIGRATION_29_30)
                .build()
    }
}
