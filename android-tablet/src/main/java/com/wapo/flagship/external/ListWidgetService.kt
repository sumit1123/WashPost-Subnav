package com.wapo.flagship.external

import android.content.Intent
import android.widget.RemoteViewsService

class ListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ListWidgetRemoteViewsFactory(applicationContext, intent)
    }
}
