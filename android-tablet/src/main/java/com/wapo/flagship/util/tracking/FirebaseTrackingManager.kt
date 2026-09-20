package com.wapo.flagship.util.tracking

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.wapo.android.commons.util.Logger
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.wapo.flagship.Utils
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.manager.ConfigManager

class FirebaseTrackingManager(
    val context: Context,
) {
    private val TAG: String = FirebaseTrackingManager::class.java.simpleName
    private val maxEventNameLength = 40
    private val maxUserPropertyNameLength = 24
    private val maxUserPropertyValueLength = 36
    private val reservedPrefixes: Array<String> = arrayOf("firebase_", "google_", "ga_")

    val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val measurementMapManager: MeasurementMapManager = MeasurementMapManager(context)
    private var eventDispatchCount: Int = 0

    fun setUserId(id: String) {
        firebaseAnalytics.setUserId(id)
    }

    fun trackAction(
        action: String?,
        measurementMap: MeasurementMap,
        isRawEvent: Boolean = false,
    ) {
        action ?: return

        val validatedMap = if (isRawEvent) {
            measurementMap
        } else {
            measurementMapManager.handleEventMetrics(action, measurementMap)
        }
        val eventName = processName(action, maxEventNameLength)
        if (eventName.isEmpty()) {
            return
        }

        val param = Bundle()

        val processedMap = MeasurementMap()

        validatedMap.keys.forEach {
            val key = processName(it) // truncate param key to char limit
            if (key.isNotEmpty()) {
                val value = processValue(validatedMap[it]) // truncate param value to char limit
                if (value?.isEmpty() == false) {
                    param.putString(key, value)
                    processedMap[key] = value
                }
            }
        }

        if (BuildConfig.DEBUG) {
            printValues(action, processedMap)
        }

        firebaseAnalytics.logEvent(eventName, param)
        dispatchEvents()
    }

    fun trackState(
        state: String,
        map: MeasurementMap,
        isRawEvent: Boolean = false,
    ) {
        trackAction(state, map, isRawEvent)
    }

    fun resumeCollection(activity: Activity) {
        firebaseAnalytics.setCurrentScreen(activity, activity.javaClass.simpleName, null)
    }

    /**
     * Set user properties to better filter Audiences in FireBase A/B Testing.
     */
    fun setUserProperties(properties: MeasurementMap) {
        properties.forEach {
            val value = processValue(it.value)
            firebaseAnalytics.setUserProperty(it.key, value)
        }
    }

    private fun processName(
        name: String,
        length: Int = Int.MAX_VALUE,
    ): String {
        if (name.isEmpty()) {
            return name
        }
        // https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.UserProperty
        // https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Param
        var nameVar = name
        reservedPrefixes.forEach {
            if (nameVar.startsWith(it)) {
                nameVar = nameVar.removePrefix(it)
                return@forEach
            }
        }

        nameVar = nameVar.replace("[^a-zA-Z0-9_]".toRegex(), "_").replace("[_]+".toRegex(), "_")

        if (!Character.isLetterOrDigit(nameVar[0])) {
            nameVar = nameVar.substring(1)
        }

        return nameVar
    }

    private fun processValue(
        obj: Any?,
        length: Int = Int.MAX_VALUE,
    ): String? {
        if (obj == null) {
            return null
        }
        val objValue = obj.toString()
        return when {
            objValue.isEmpty() -> null
            objValue.length > length -> objValue.substring(0, length)
            else -> objValue
        }
    }

    private fun dispatchEvents() {
        eventDispatchCount++
        val maxGAUploadEventsBatchSize = ConfigManager.getInstance().config.maxGAUploadEventsBatchSize
        if (eventDispatchCount >= maxGAUploadEventsBatchSize) {
            dispatchEventsNow()
            eventDispatchCount = 0
        }
    }

    fun dispatchEventsNow() {
        if (Utils.isProductFlavorAmazon() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GoogleAnalytics.getInstance(context).dispatchLocalHits()
        }
    }

    private fun printValues(
        action: String,
        measurementMap: MeasurementMap,
    ) {
        val builder = StringBuilder()
        builder.append("FA trackEvent =>> $action")
        var propertiesCount = 0
        var parametersCount = 0
        measurementMap.forEach {
            val name = processName(it.key)
            if (name.isNotEmpty()) {
                val value = processValue(it.value)
                if (value?.isEmpty() == false) {
                    builder.append("\n  $name =>> $value")
                    parametersCount++
                }
            }
        }
        builder.append("\n\n")
        Logger.d(TAG, builder.toString())
        Logger.d(
            TAG,
            "Total: ${propertiesCount + parametersCount}, propertiesCount: $propertiesCount, parametersCount: $parametersCount",
        )
    }
}
