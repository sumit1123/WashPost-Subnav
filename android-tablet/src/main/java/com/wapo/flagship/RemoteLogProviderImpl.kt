// Copyright (c) 2019 The Washington Post. All rights reserved.

package com.wapo.flagship

import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.util.JUcidTracker

class RemoteLogProviderImpl : RemoteLog.RemoteLogProvider {
    override fun isApplicationInForeground(): Boolean = !FlagshipApplication.getInstance().isApplicationInBackground

    override fun getJUcid(): String = JUcidTracker.jUcid
}
