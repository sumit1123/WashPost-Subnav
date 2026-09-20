package com.wapo.flagship.external

import android.appwidget.AppWidgetManager
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.external.storage.WidgetType

class ListWidgetConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val tag = "TabletWidgetConfiguration"

    override fun onItemSelected(bundleName: String) {
        EventLog
            .Builder()
            .apply {
                setMessage("Added List Widget")
                setModule(LogModules.WIDGET)
                set("bundle_name", bundleName)
            }.run {
                RemoteLog.d(applicationContext, build())
            }
        TabletWidget.updateAppWidget(
            applicationContext,
            AppWidgetManager.getInstance(applicationContext),
            appWidgetId,
        )
    }

    override fun getWidgetType(): WidgetType = WidgetType.TABLET_WIDGET
}
