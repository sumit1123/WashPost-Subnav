/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.complications

import android.content.ComponentName
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.wapo.flagship.receivers.ComplicationTapBroadcastReceiver
import com.washingtonpost.android.R

class AlertsDataSourceService : ComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "1").build(),
            contentDescription = PlainComplicationText.Builder(
                text = "ShortText version of Alerts."
            ).build()
        ).build()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return

        // Create Tap Action so that the user can trigger an update by tapping the complication.
        // We pass the complication id, so we can only update the specific complication tapped.
        val thisDataSource = ComponentName(this, javaClass)
        val complicationPendingIntent =
            ComplicationTapBroadcastReceiver.getToggleIntent(
                this,
                thisDataSource,
                request.complicationInstanceId
            )

        val complicationData =
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text = "1").build(),
                contentDescription = PlainComplicationText
                    .Builder(text = "ShortText version of Alerts.").build()
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        image = Icon.createWithResource(this, R.drawable.icon_alerts)
                    )
                        .build()
                )
                .setTapAction(complicationPendingIntent)
                .build()
        listener.onComplicationData(complicationData)
    }
}