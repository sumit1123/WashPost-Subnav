package com.wapo.flagship.external.foryouwidget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.unit.ColorProvider
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.external.foryouwidget.data.*
import com.wapo.flagship.external.foryouwidget.ui.components.ForYouTitleBar
import com.wapo.flagship.external.foryouwidget.ui.components.ForYouWidgetBody
import com.wapo.flagship.external.foryouwidget.ui.components.ShowEmptyState

private const val FOR_YOU_TITLE = "For You"
private const val TAG = "ForYouWidget"

class ForYouWidget: GlanceAppWidget() {
    override val stateDefinition = WidgetGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            Content(context)
        }
    }
    @Composable
    private fun Content(context: Context){
        val recommendations = getRecommendationsForCurrentState(
            context = context
        )

        GlanceTheme {
            Scaffold(
                horizontalPadding = 8.dp,
                titleBar = {
                    ForYouTitleBar(
                        widgetTitle =  FOR_YOU_TITLE,
                    )
                },
                backgroundColor = ColorProvider(Color.White),
                content = {
                    when {
                        recommendations.isEmpty() -> {
                            ShowEmptyState()
                        }
                        else -> {
                            ForYouWidgetBody(
                                articles = recommendations,
                            )
                        }
                    }
                }
            )
        }
    }


    @Composable
    private fun getRecommendationsForCurrentState(context: Context): List<ForYouWidgetItem> {
        val state = currentState<Preferences>()
        val currentPageIndex = state[CURRENT_PAGE_INDEX_KEY] ?: 0
        if (currentPageIndex > 2){
            return emptyList()
        }
        val widgetItemsJson = state[CURRENT_WIDGET_ITEMS_KEY] ?: "[]"

        val allItems = deserializeWidgetItems(widgetItemsJson)

        if (allItems.isEmpty()) {
            Logger.d(TAG, "No cached widget items available")
            return emptyList()
        }

        val unconsumedItems = allItems.filter { !it.isConsumed }
        Logger.d(TAG, "Page $currentPageIndex: ${unconsumedItems.size} unconsumed from ${allItems.size} total")

        if (unconsumedItems.isNotEmpty()) {
            return unconsumedItems
        }

        Logger.d(TAG, "Current page empty, moving to next page")
        LaunchedEffect(currentPageIndex) {
            moveToNextPage(context, currentPageIndex)
        }

        return emptyList()
    }
}