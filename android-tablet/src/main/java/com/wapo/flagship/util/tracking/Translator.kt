package com.wapo.flagship.util.tracking

import com.wapo.flagship.json.TrackingInfo

/**
 * Interface to implement a translator for any analytics [Provider].
 *
 */
interface Translator {
    fun translate(
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    ): MeasurementMap
}
