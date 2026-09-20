package com.wapo.flagship.external

import android.appwidget.AppWidgetManager
import com.wapo.flagship.external.storage.WidgetType

class WidgetConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val tag = "WidgetConfiguration"

    override fun onItemSelected(bundleName: String) {
        Widget.updateAppWidget(
            applicationContext,
            AppWidgetManager.getInstance(applicationContext),
            appWidgetId,
        )
    }

    override fun getWidgetType(): WidgetType = WidgetType.WIDGET
}
