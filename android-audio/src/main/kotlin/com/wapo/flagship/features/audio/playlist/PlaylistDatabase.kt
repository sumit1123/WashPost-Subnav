package com.wapo.flagship.features.audio.playlist

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioMediaAdConfigTypeConverter
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioMediaSubscriptionLinksTypeConverter
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioTrackerTypeConverter
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioVoiceListTypeConverter

@Database(
    entities = [Playlist::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(
    AudioVoiceListTypeConverter::class,
    AudioTrackerTypeConverter::class,
    AudioMediaAdConfigTypeConverter::class,
    AudioMediaSubscriptionLinksTypeConverter::class,
)
abstract class PlaylistDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE playlist ADD COLUMN stream_url_no_ads TEXT")
                database.execSQL("ALTER TABLE playlist ADD COLUMN series TEXT")
                database.execSQL("ALTER TABLE playlist ADD COLUMN ad_config TEXT")
                database.execSQL("ALTER TABLE playlist ADD COLUMN series_slug TEXT")
                database.execSQL("ALTER TABLE playlist ADD COLUMN podcast_slug TEXT")
                database.execSQL("ALTER TABLE playlist ADD COLUMN subscription_links TEXT")
            }
        }
    }
}
