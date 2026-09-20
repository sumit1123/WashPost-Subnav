/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.remotelog.logger

import com.wapo.android.domain.repository.EventTimerLogRepo
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import javax.inject.Inject

class LoadRenderMetricsImpl @Inject constructor (
    private val eventTimerLogRepo: EventTimerLogRepo
) : LoadRenderMetrics {

    override fun stopLoadRenderMetrics(loadRenderEvent: LoadRenderMetricsEvent) {
        stopLoadRenderMetrics(loadRenderEvent, emptyMap())
    }

    override fun startLoadRenderMetrics(loadRenderEvent: LoadRenderMetricsEvent) {
        eventTimerLogRepo.startTimingEvent(loadRenderEvent.event, EventTimerLog.FRONT_RENDER_LOAD)
    }

    override fun stopLoadRenderMetrics(
        loadRenderEvent: LoadRenderMetricsEvent,
        additionalFields: Map<String, String>
    ) {
        val eventAdditionalFields: MutableMap<String, String> = HashMap()
        eventAdditionalFields.putAll(additionalFields)
        eventAdditionalFields[EventTimerLog.SECTION_NAME_FIELD] = loadRenderEvent.event

        eventTimerLogRepo.stopTimingEventAndLog(
            loadRenderEvent.event,
            EventTimerLog.FRONT_RENDER_LOAD,
            true,
            eventAdditionalFields,
            EventTimerLog.SECTION_LOAD_METRICS_MESSAGE
        )
    }
}
