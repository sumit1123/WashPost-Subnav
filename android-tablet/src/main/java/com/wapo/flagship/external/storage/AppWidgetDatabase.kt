package com.wapo.flagship.external.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AppWidget::class], version = 1, exportSchema = false)
@TypeConverters(AppWidgetTypeConverters::class)
abstract class AppWidgetDatabase : RoomDatabase() {
    abstract fun appWidgetDao(): AppWidgetDao
}
