/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.remotelog.logger

import android.content.Context
import com.wapo.android.domain.repository.EventTimerLogRepo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EventTimerLogRepo that acts as a wrapper around the static EventTimerLog utility.
 * This allows for dependency injection and testability.
 */
@Singleton
class EventTimerLogRepoImpl @Inject constructor(
    private val context: Context
) : EventTimerLogRepo {

    override fun startTimingEvent(baseEvent: String, concreteEvent: String, extraMessage: String?) {
        EventTimerLog.startTimingEvent(baseEvent, concreteEvent, extraMessage)
    }

    override fun restartTimingEvent(baseEvent: String, concreteEvent: String) {
        EventTimerLog.restartTimingEvent(baseEvent, concreteEvent)
    }

    override fun appendNewMessageTimingEvent(baseEvent: String, concreteEvent: String, extraMessage: String) {
        EventTimerLog.appendNewMessageTimingEvent(baseEvent, concreteEvent, extraMessage)
    }

    override fun stopTimingEvent(baseEvent: String, concreteEvent: String) {
        EventTimerLog.stopTimingEvent(baseEvent, concreteEvent)
    }

    override fun stopTimingEventAndLog(
        baseEvent: String,
        stopEvent: String,
        includeCalculatedTotal: Boolean,
        additionalFields: Map<String, String>,
        message: String?
    ) {
        EventTimerLog.stopTimingEventAndLog(baseEvent, stopEvent, context, includeCalculatedTotal, additionalFields, message)
    }

    override fun logTimingEvents(
        baseEvent: String,
        includeCalculatedTotal: Boolean,
        additionalFields: Map<String, String>,
        message: String?
    ) {
        EventTimerLog.logTimingEvents(baseEvent, context, includeCalculatedTotal, additionalFields, message)
    }

    override fun getStopTime(baseEvent: String, concreteEvent: String): Double {
        return EventTimerLog.getStopTime(baseEvent, concreteEvent)
    }

    override fun dumpTimers(baseEvent: String) {
        EventTimerLog.dumpTimers(baseEvent)
    }
}
