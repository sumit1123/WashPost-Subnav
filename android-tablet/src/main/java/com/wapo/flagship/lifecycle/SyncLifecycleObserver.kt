package com.wapo.flagship.lifecycle

import android.os.Build
import androidx.lifecycle.Lifecycle
import com.wapo.android.remotelog.logger.EventTimerLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.content.ContentUpdateRulesManager
import com.wapo.flagship.sync.SyncManager
import com.wapo.flagship.util.tracking.Measurement

class SyncLifecycleObserver(
    lifecycle: Lifecycle,
) : FlagshipLifecycleObserver(lifecycle) {
    override fun onApplicationStart() {
        super.onApplicationStart()
        AppContext.incrementAppResumeCount()
        AppContext.incrementAppResumeDayCount()
        FlagshipApplication.getInstance().apply {
            SyncManager.reschedule(true, this)
            contentUpdateRulesManager.setTime(
                ContentUpdateRulesManager.TimeType.APP_RESUME_TIME,
                null,
            )
        }
    }

    override fun onApplicationStop() {
        super.onApplicationStop()
        FlagshipApplication.getInstance().apply {
            SyncManager.reschedule(false, this)
            contentUpdateRulesManager.setTime(
                ContentUpdateRulesManager.TimeType.APP_PAUSE_TIME,
                null,
            )
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Firebase Dispatch local hits is powered by a background service
            // and so we don't to make a dispatch call for Android O(8, SDK 26) or above to prevent a crash
            Measurement.dispatchEventsNow()
        }
    }

    override fun onApplicationPause() {
        super.onApplicationPause()
        EventTimerLog.dumpTimers(EventTimerLog.SYNC_EVENT)
    }
}
