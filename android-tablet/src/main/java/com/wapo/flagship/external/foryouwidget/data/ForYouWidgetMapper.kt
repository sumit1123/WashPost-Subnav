package com.wapo.flagship.external.foryouwidget.data

import android.util.Log
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.timeAgo
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.getDate

private const val TAG = "ForYouWidgetMapper"

object ForYouWidgetMapper {

    fun mapToWidgetItems(
        recommendations: List<RecommendationsItem>,
    ): List<ForYouWidgetItem> {
        return recommendations.mapNotNull { article ->
            try {
                ForYouWidgetItem(
                    arcId = article.arcId ,
                    headline = article.headline,
                    url = article.url,
                    imageUrl = article.imageUrl,
                    category = article.labelDisplay?.basic?.text.orEmpty(),
                    displayAge = article.getDate()?.let { timeAgo(it) } ?: ""
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to map recommendation item: ${article.arcId}", e)
                null
            }
        }
    }

    fun mapToWidgetItem(
        recommendation: RecommendationsItem,
    ): ForYouWidgetItem? {
        return try {
            ForYouWidgetItem(
                arcId = recommendation.arcId,
                headline = recommendation.headline,
                url = recommendation.url,
                imageUrl = recommendation.imageUrl,
                category = recommendation.labelDisplay?.basic?.text.orEmpty(),
                displayAge = recommendation.getDate()?.let { timeAgo(it) } ?: ""
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to map single recommendation item: ${recommendation.arcId}", e)
            null
        }
    }
}