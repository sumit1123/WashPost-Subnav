package com.wapo.flagship.external.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppWidgetDao {
    @Query("SELECT * FROM appwidget")
    fun getAll(): List<AppWidget>

    @Query("SELECT * FROM appwidget WHERE appWidgetId = :id")
    fun findById(id: String): AppWidget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(appWidget: AppWidget)

    @Query("DELETE FROM appwidget WHERE appWidgetId = :id")
    fun deleteById(id: String)
}
