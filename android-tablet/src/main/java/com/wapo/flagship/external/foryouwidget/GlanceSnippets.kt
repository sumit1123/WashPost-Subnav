package com.wapo.flagship.external.foryouwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.external.foryouwidget.workers.ForYouWidgetUpdateWorker
import com.wapo.flagship.external.foryouwidget.ui.ForYouWidget

private const val TAG = "ForYouWidget"

class ForYouWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ForYouWidget()

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        val app = context?.applicationContext as FlagshipApplication
        app.runWidgetInit(context)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        ForYouWidgetUpdateWorker.cancelAllUpdates(context)
    }
}