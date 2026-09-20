package com.wapo.android.commons.engagement

import com.wapo.android.commons.util.Logger

class EngagementTracker private constructor(
    private val measurement: Measurement,
) {
    fun trackEngagement(trace: EngagementTrace) {
        if (!trace.isValid()) {
            Logger.w(TAG, "Invalid engagement trace: ${getTraceLogInfo(trace)}")
            return
        }
        Logger.d(TAG, "trackEngagement: ${getTraceLogInfo(trace)}")
        measurement.trackEngagement(trace)
    }

    private fun getTraceLogInfo(trace: EngagementTrace): String = with(trace) {
        return "id=$id, pageName=${(this as? PageEngagementTrace)?.pageName}, startTimeMillis=$startTimeMillis, endTimeMillis=$endTimeMillis, engagedTimeMillis=$engagedTimeMillis"
    }

    fun interface Measurement {
        fun trackEngagement(trace: EngagementTrace)
    }

    companion object {
        private const val TAG = "EngagementTracker"

        @Volatile
        private lateinit var instance: EngagementTracker

        fun init(measurement: Measurement) {
            if (!::instance.isInitialized) {
                instance = EngagementTracker(measurement)
            }
        }

        fun getInstance(): EngagementTracker {
            return instance
        }
    }
}