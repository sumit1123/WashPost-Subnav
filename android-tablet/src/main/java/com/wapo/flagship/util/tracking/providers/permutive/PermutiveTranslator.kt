package com.wapo.flagship.util.tracking.providers.permutive

import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.json.TrackingInfoPageType
import com.wapo.flagship.util.tracking.Evars
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.MeasurementMap
import com.wapo.flagship.util.tracking.Translator
import com.wapo.flagship.util.tracking.providers.permutive.PermutiveEventProperties.*

/**
 * Class to translate and prepare a map for Permutive page views and events
 */
class PermutiveTranslator : Translator {
    private val tag: String = PermutiveTranslator::class.java.simpleName

    /**
     * Method to translate and return the result map
     * @param measurementMap - analytics map from the Measurement class. It can have transformed values
     * @param trackingInfo - original values from the feed/app
     */
    override fun translate(
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    ): MeasurementMap {
        val resultMap: MeasurementMap = MeasurementMap()

        // map properties based on page type
        // mapping articles and section properties for now as permutive is tracking only for those.
        when (trackingInfo?.pageType) {
            // map section properties
            TrackingInfoPageType.FRONT -> {
                mapSectionProperties(resultMap, measurementMap, trackingInfo)
            }
            // map article properties
            TrackingInfoPageType.ARTICLE -> {
                // Process targetingDict if it is available otherwise fallback to trackingInfo
                if (trackingInfo?.targetingDict != null) {
                    mapArticlePropertiesFromDict(
                        resultMap,
                        measurementMap,
                        trackingInfo.targetingDict,
                    )
                }
            }
            else -> {}
        }

        // Custom Sections. Should we track?
        if (measurementMap[Evars.CONTENT_TYPE.variable] == Measurement.CONTENT_TYPE_COMICS ||
            trackingInfo?.primarySection == SECTION_BIO_PAGE
        ) {
            mapSectionProperties(resultMap, measurementMap, trackingInfo)
        }

        // map default properties
        if (resultMap.isNotEmpty()) {
            mapPlatformProperty(resultMap, measurementMap)
        }

        return resultMap
    }

    private fun mapSectionProperties(
        resultMap: MeasurementMap,
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    ) {
        resultMap.apply {
            // Property: Name:"Page Section", ID:"section"
            val section = trackingInfo?.primarySection
            setProperty(this, SECTION.propertyName, section)

            // Property: Name:"Page Subsection", ID:"subsection"
            val subSection = trackingInfo?.contentSubsection
            setProperty(this, SUBSECTION.propertyName, subSection)

            if (section.isNullOrEmpty()) {
                // Custom section (like Comics etc.,)? Should we track?
                logI("Missing section from source map. So tracking app_section value as section.")
                val appSection = measurementMap[Evars.APP_SECTION.variable]?.toString()
                setProperty(this, SECTION.propertyName, appSection)
            }

            // Property: Name:"Page Path", ID:"canonicalUrl"
            var contentUrl = measurementMap[Evars.CONTENT_URL.variable]?.toString()
            // Process path as canonical url.
            val canonicalUrl = trackingInfo?.pagePath ?: URLParser(contentUrl).getPath()
            setProperty(this, CANONICAL_URL.propertyName, canonicalUrl)
        }
    }

    /**
     * Process targetingDict object recursively to prepare the resultMap object.
     */
    private fun mapArticlePropertiesFromDict(
        resultMap: MeasurementMap,
        measurementMap: MeasurementMap,
        targetingDict: Any?,
    ) {
        val dict = targetingDict as? Map<*, *>? ?: return
        mapPropertiesFromMap(resultMap, dict)
    }

    private fun mapPropertiesFromMap(
        resultMap: MeasurementMap,
        map: Map<*, *>,
    ) {
        map.forEach { entry ->
            if (entry.value is Map<*, *>) {
                setProperty(
                    resultMap,
                    entry.key as String,
                    MeasurementMap().apply { mapPropertiesFromMap(this, entry.value as Map<*, *>) },
                )
            } else {
                setProperty(resultMap, entry.key as String, entry.value)
            }
        }
    }

    private fun mapPlatformProperty(
        resultMap: MeasurementMap,
        measurementMap: MeasurementMap,
    ) {
        resultMap.apply {
            // Property: Name:"Platform", ID:"platform"
            val platform =
                when (measurementMap[Evars.PROPERTY_NAME.variable]?.toString()) {
                    Measurement.CLASSIC_GOOGLE -> PLATFORM_ANDROID
                    Measurement.CLASSIC_AMAZON -> PLATFORM_AMAZON
                    else -> null
                }
            setProperty(this, PLATFORM.propertyName, platform)
        }
    }

    private fun setProperty(
        resultMap: MeasurementMap,
        name: String,
        value: String?,
    ) {
        if (!value.isNullOrEmpty()) {
            resultMap[name] = value
        } else {
            logW("$name value is missing!")
        }
    }

    private fun setProperty(
        resultMap: MeasurementMap,
        name: String,
        value: List<Any?>?,
    ) {
        if (!value.isNullOrEmpty()) {
            resultMap[name] = value.filterNotNull()
        } else {
            logW("$name value is missing!")
        }
    }

    private fun setProperty(
        resultMap: MeasurementMap,
        name: String,
        value: Any?,
    ) {
        if (value != null) {
            resultMap[name] = value
        } else {
            logW("$name value is missing!")
        }
    }

    private fun logW(msg: String) {
        Logger.w(tag, "Permutive, $msg")
    }

    private fun logI(msg: String) {
        Logger.i(tag, "Permutive, $msg")
    }

    companion object {
        const val PLATFORM_ANDROID = Measurement.CLASSIC_GOOGLE
        const val PLATFORM_AMAZON = Measurement.CLASSIC_AMAZON
        const val SECTION_BIO_PAGE = "bioPage"
    }
}
