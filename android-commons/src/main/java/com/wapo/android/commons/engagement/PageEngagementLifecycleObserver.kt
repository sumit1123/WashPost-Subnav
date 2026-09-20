package com.wapo.android.commons.engagement

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.UUID

class PageEngagementLifecycleObserver(private var pageName: String, private val tabName: String, private var contentType: String) : DefaultLifecycleObserver {

    private val engagementTracker = EngagementTracker.getInstance()
    private var engagementTrace: PageEngagementTrace? = null

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        startEngagementTrace()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        stopAndTrackEngagementTrace()
    }

    private fun startEngagementTrace() {
        if (engagementTrace != null) return

        engagementTrace = PageEngagementTrace(UUID.randomUUID().toString(), null, null)
        engagementTrace?.startTrace()
    }

    private fun stopAndTrackEngagementTrace() {
        engagementTrace?.let { trace ->
            trace.stopTrace()
            trace.updateTrackingInfo(
                pageName = pageName,
                contentType = contentType,
                tabName = tabName
            )
            engagementTracker.trackEngagement(trace)
            engagementTrace = null
        }
    }
}