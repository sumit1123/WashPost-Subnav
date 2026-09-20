package com.wapo.flagship.external.foryouwidget.actions

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.external.WidgetData
import com.wapo.flagship.external.foryouwidget.data.CURRENT_WIDGET_ITEMS_KEY
import com.wapo.flagship.external.foryouwidget.data.deserializeWidgetItems
import com.wapo.flagship.external.foryouwidget.data.serializeWidgetItems
import com.wapo.flagship.external.foryouwidget.ui.ForYouWidget
import com.wapo.flagship.external.storage.WidgetType
import com.wapo.flagship.features.articles2.activities.WIDGET_ORIGINATED
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import androidx.core.net.toUri


/**
 * Actions to be carried out when a card is clicked
 * 1. The article is marked as consumed
 * 2. The article is then opened in the app
 */
class ArticleClickAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val articleId = parameters[articleIdKey] ?: return
        val articleUrl = parameters[articleUrlKey] ?: return

        // Use full washingtonpost.com URL
        val fullArticleUrl = DeepLinksProcessor.pathToFullUrl(articleUrl)
        val bundle = Bundle().apply {
            putBoolean(WIDGET_ORIGINATED, true)
            putString(WidgetData.EXTRAS_WIDGET_TYPE, WidgetType.FOR_YOU_WIDGET.name)
        }

        val processed = DeepLinksProcessor.process(
            link = fullArticleUrl,
            activityContext = context,
            isExternalOrigin = false,
            sourceType = DeepLinksProcessor.SourceType.WIDGET,
            referrer = null,
            bundle = bundle
        )
        if (!processed) Logger.e("ArticleClickAction","Deeplink processor was not able to process $fullArticleUrl")
        else markAsConsumed(context, glanceId, articleId, articleUrl)
    }

    private suspend fun markAsConsumed(context: Context, glanceId: GlanceId, articleId: String, articleUrl: String) {
        try {
            updateAppWidgetState(context, glanceId) { prefs ->
                val currentItemsJson = prefs[CURRENT_WIDGET_ITEMS_KEY] ?: "[]"
                val currentItems = deserializeWidgetItems(currentItemsJson)

                val updatedItems = currentItems.map { item ->
                    if (item.arcId == articleId || item.url == articleUrl) {
                        item.copy(isConsumed = true)
                    } else {
                        item
                    }
                }

                prefs[CURRENT_WIDGET_ITEMS_KEY] = serializeWidgetItems(updatedItems)
            }

            ForYouWidget().updateAll(context)

        } catch (e: Exception) {
            Logger.e("ArticleClick", "Failed to mark as consumed", e)
        }
    }

    companion object {
        const val FOR_YOU_WIDGET_UPDATE = "for_you_widget_update"
        val articleIdKey = ActionParameters.Key<String>("article_id")
        val articleUrlKey = ActionParameters.Key<String>("article_url")
        val positionKey = ActionParameters.Key<Int>("position")
        val widgetUpdateKey = ActionParameters.Key<Boolean>(FOR_YOU_WIDGET_UPDATE)
    }
}