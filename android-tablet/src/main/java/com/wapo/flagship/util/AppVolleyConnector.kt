// Copyright (c) 2023 The Washington Post. All rights reserved.
package com.wapo.flagship.util

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.volley.VolleyConnector

class AppVolleyConnector : VolleyConnector {
    override fun logD(eventLogBuilder: EventLog.Builder) = Unit

    override fun logE(eventLogBuilder: EventLog.Builder) {
        eventLogBuilder
            .setModule(LogModules.VOLLEY)
            .run {
                RemoteLog.e(AppContextUtils.appContext, this.build())
            }
    }
}
