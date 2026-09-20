package com.wapo.flagship.util.tracking

import com.wapo.flagship.json.TrackingInfo

/**
 * Interface to implement any analytics provider.
 * [Measurement] class maintains those providers and calls these methods when
 * it performs similar events.
 */
interface Provider {
    fun trackState(
        name: String,
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    )

    fun trackEvent(
        eventName: String,
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    )

    fun pause()

    fun resume()

    fun stop()

    fun release()
}
