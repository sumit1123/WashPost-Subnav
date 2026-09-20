/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.domain.repository

/**
 * Repository interface for logging timed events.
 * This abstracts the static EventTimerLog utility for testability and dependency injection.
 */
interface EventTimerLogRepo {
    fun startTimingEvent(baseEvent: String, concreteEvent: String, extraMessage: String? = null)
    fun restartTimingEvent(baseEvent: String, concreteEvent: String)
    fun appendNewMessageTimingEvent(baseEvent: String, concreteEvent: String, extraMessage: String)
    fun stopTimingEvent(baseEvent: String, concreteEvent: String)
    fun stopTimingEventAndLog(
        baseEvent: String,
        stopEvent: String,
        includeCalculatedTotal: Boolean,
        additionalFields: Map<String, String>,
        message: String? = null
    )
    fun logTimingEvents(
        baseEvent: String,
        includeCalculatedTotal: Boolean,
        additionalFields: Map<String, String>,
        message: String? = null
    )
    fun getStopTime(baseEvent: String, concreteEvent: String): Double
    fun dumpTimers(baseEvent: String)
}
