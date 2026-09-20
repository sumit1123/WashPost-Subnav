package com.wapo.flagship.external.foryouwidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.GlanceStateDefinition
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.external.foryouwidget.ui.ForYouWidget
import java.io.File

private const val TAG = "ForYouWidget"
private const val WIDGET_DATASTORE_FILE = "for_you_widget_state"

// Keys for DataStore
val CURRENT_PAGE_INDEX_KEY = intPreferencesKey("current_page_index")
val CURRENT_ARTICLE_INDEX_KEY = intPreferencesKey("current_article_index")
val LAST_UPDATE_TIME_KEY = longPreferencesKey("last_update_time")
val CURRENT_WIDGET_ITEMS_KEY = stringPreferencesKey("current_widget_items")

object WidgetGlanceStateDefinition : GlanceStateDefinition<Preferences> {

    // Using Glance's Datastore
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = WIDGET_DATASTORE_FILE
    )

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.applicationContext.filesDir, "datastore/$WIDGET_DATASTORE_FILE")
    }
}

//------------ Below this are helper functions dealing in Widget state -------------
// --------------------- that are being used in other modules ----------------------


fun serializeWidgetItems(items: List<ForYouWidgetItem>): String {
    return try {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(List::class.java, ForYouWidgetItem::class.java)
        val adapter = moshi.adapter<List<ForYouWidgetItem>>(type)
        adapter.toJson(items)
    } catch (e: Exception) {
        Logger.e("WidgetState", "Failed to serialize widget items", e)
        "[]"
    }
}

fun deserializeWidgetItems(json: String): List<ForYouWidgetItem> {
    return try {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(List::class.java, ForYouWidgetItem::class.java)
        val adapter = moshi.adapter<List<ForYouWidgetItem>>(type)
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        Logger.e("WidgetState", "Failed to deserialize widget items", e)
        emptyList()
    }
}


/**
 * Sets a new state for the widget and recomposes all the widget instances.
 */
suspend fun updateWidgetRecommendations(
    context: Context,
    recommendations: List<ForYouWidgetItem>,
    pageIndex: Int,
    articleIndex: Int = 0
) {
    try {
        val widgetItemsJson = serializeWidgetItems(recommendations)

        GlanceAppWidgetManager(context).getGlanceIds(ForYouWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[CURRENT_PAGE_INDEX_KEY] = pageIndex
                prefs[CURRENT_ARTICLE_INDEX_KEY] = articleIndex
                prefs[CURRENT_WIDGET_ITEMS_KEY] = widgetItemsJson
                prefs[LAST_UPDATE_TIME_KEY] = System.currentTimeMillis()
            }
        }

        ForYouWidget().updateAll(context)
        Logger.d("WidgetState", "Updated widget with ${recommendations.size} items (with cached images), page: $pageIndex, article: $articleIndex")

    } catch (e: Exception) {
        Logger.e("WidgetState", "Failed to update widget state", e)
    }
}


/**
 * Used to move to the next page when the article recommendations are exhausted by
 * opening all articles in a page
 */
suspend fun moveToNextPage(context: Context, currentPage: Int) {
    try {
        val app = context.applicationContext as FlagshipApplication
        val repository = app.forYouFeedRepo
        val nextPage = (currentPage + 1)

        Logger.d(TAG, "Loading next page: $nextPage")

        val pageRecommendations = repository.getCachedArticlesPage(nextPage)
        val pageItems = ForYouWidgetMapper.mapToWidgetItems(pageRecommendations)

        // Update widget state to next page
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ForYouWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[CURRENT_PAGE_INDEX_KEY] = nextPage
                prefs[CURRENT_ARTICLE_INDEX_KEY] = 0
                prefs[CURRENT_WIDGET_ITEMS_KEY] = serializeWidgetItems(pageItems)
            }
        }

        ForYouWidget().updateAll(context)

    } catch (e: Exception) {
        Logger.e(TAG, "Failed to move to next page", e)
    }
}