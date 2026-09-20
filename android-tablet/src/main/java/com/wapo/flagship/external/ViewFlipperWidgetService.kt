package com.wapo.flagship.external

import android.content.Intent
import android.widget.RemoteViewsService

class ViewFlipperWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ViewFlipperWidgetRemoteViewsFactory(applicationContext, intent)
    }
}
