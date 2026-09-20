package com.wapo.flagship.external

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.WorkerThread
import androidx.room.Room
import com.wapo.flagship.external.storage.AppWidget
import com.wapo.flagship.external.storage.AppWidgetDao
import com.wapo.flagship.external.storage.AppWidgetDatabase
import com.wapo.android.commons.util.Logger

interface WidgetStorage {
    var appWidgetDatabase: AppWidgetDatabase

    fun getAll(): List<AppWidget>

    fun getById(
        @NonNull appWidgetId: Int,
    ): AppWidget?

    fun insert(
        @NonNull appWidget: AppWidget,
    ): Boolean

    fun deleteById(
        @NonNull appWidgetId: Int,
    ): Boolean

    fun print()
}

@WorkerThread
class WidgetDBStorage private constructor(
    val context: Context,
) : WidgetStorage {
    override var appWidgetDatabase =
        Room
            .databaseBuilder(
                context,
                AppWidgetDatabase::class.java,
                DATABASE_NAME,
            ).build()

    override fun getAll(): List<AppWidget> = getAppWidgetDao().getAll()

    override fun getById(
        @NonNull appWidgetId: Int,
    ): AppWidget? = getAppWidgetDao().findById(appWidgetId.toString())

    override fun insert(
        @NonNull appWidget: AppWidget,
    ): Boolean {
        getAppWidgetDao().insert(appWidget)
        return false
    }

    override fun deleteById(
        @NonNull appWidgetId: Int,
    ): Boolean {
        getAppWidgetDao().deleteById(appWidgetId.toString())
        return false
    }

    override fun print() {
        val records = getAppWidgetDao().getAll()
        Logger.d(TAG, "Widget - print - widgetsCount=${records.size}")
        records.forEachIndexed { index, it ->
            Logger.d(
                TAG,
                "Widget ListStorage - print - index=$index -> appWidgetId=${it.appWidgetId}, name=${it.sectionName}, bundleName=${it.bundleName}, type=${it.widgetType}",
            )
        }
    }

    private fun getAppWidgetDao(): AppWidgetDao = appWidgetDatabase.appWidgetDao()

    companion object {
        private const val TAG = "WidgetDBStorage"
        private const val DATABASE_NAME = "app_widget_storage.db"

        @Volatile
        private var INSTANCE: WidgetStorage? = null

        @JvmStatic
        fun getInstance(context: Context): WidgetStorage =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetDBStorage(context).also { INSTANCE = it }
            }
    }
}
