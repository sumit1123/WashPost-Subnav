/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.features.section.activities.SectionActivity
import com.washingtonpost.android.R


class HeadlinesDataSourceService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(
                text = "The Washington Post: Breaking News, World, US, DC News and Analysis"
            ).build(),
            contentDescription = PlainComplicationText.Builder(
                text = "LongText version of Headlines."
            ).build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val topHeadline = WearAppContext.topHeadline ?: "Unable to load content."
        val topHeadlineUrl = WearAppContext.topHeadlineUrl

        val articleIntent = Intent(this, SectionActivity::class.java).apply {
            putExtra(SectionActivity.SECTION, "Top Stories")
        }
        val pendingArticleIntent = PendingIntent.getActivity(
            this,
            request.complicationInstanceId,
            articleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return when (request.complicationType) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text = topHeadline).build(),
                contentDescription = PlainComplicationText
                    .Builder(text = "LongText version of Headlines.").build()
            )
                .setSmallImage(
                    SmallImage.Builder(
                        image = Icon.createWithResource(this, R.drawable.ic_wapo_logo_white),
                        type = SmallImageType.ICON
                    ).build()
                )
                .setTapAction(pendingArticleIntent)
                .build()
            else -> null
        }
    }

}