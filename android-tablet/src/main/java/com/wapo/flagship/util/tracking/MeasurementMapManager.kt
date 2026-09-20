package com.wapo.flagship.util.tracking

import android.content.Context
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class MeasurementMapManager(
    context: Context,
) {
    private val eventParamsJson: JSONObject

    init {
        val eventParamsInputStream: InputStream =
            context.resources.openRawResource(
                R.raw.event_parameters,
            )
        eventParamsJson = JSONObject(eventParamsInputStream.bufferedReader().use { it.readText() })
    }

    private val eventTypes: JSONObject by lazy { eventParamsJson["eventType"] as JSONObject }
    private val requiredEventParams: JSONObject by lazy { eventParamsJson["params"] as JSONObject }

    fun handleEventMetrics(
        event: String,
        measurementMap: MeasurementMap,
    ): MeasurementMap {
        val formattedEventName = formatEventName(event, measurementMap["content_type"].toString())
        val standardParams = getStandardParams(formattedEventName, event)
        val requiredParams = getRequiredParams(formattedEventName, event)

        val newMap = createNewMapWithParams(standardParams, requiredParams, measurementMap)
        if (AppContextUtils.isDebuggableBuild())
            validateNewMap(event, formattedEventName, standardParams, requiredParams, newMap)
        return newMap
    }

    private fun formatEventName(
        event: String,
        contentType: String?,
    ): String =
        when {
            event == "page_view" && contentType == "front" -> "${event}_section"
            event == "page_view" -> "${event}_article"
            else -> event
        }

    private fun getStandardParams(
        formattedEventName: String,
        event: String,
    ): JSONArray {
        return if (eventTypes.optInt(event.uppercase(), 0) != 0) {
            requiredEventParams.optJSONArray("${formattedEventName}_standard")
                ?: requiredEventParams.getJSONArray("standard")
        } else {
            JSONArray()
        }
    }

    /**
     * Returns an event's required parameters as defined in [event_parameters.json]
     */
    private fun getRequiredParams(
        formattedEventName: String,
        event: String,
    ): JSONArray {
        return if (eventTypes.optInt(event.uppercase(), 0) != 0) {
            requiredEventParams.getJSONArray(formattedEventName)
        } else {
            JSONArray()
        }
    }

    private fun createNewMapWithParams(
        standardParams: JSONArray,
        requiredParams: JSONArray,
        measurementMap: MeasurementMap,
    ): MeasurementMap {
        val processedMap = MeasurementMap()

        // Add AB param first to make sure all events have it
        measurementMap[Evars.AB_TESTING_VARIANT.variable]
            ?.also { processedMap[Evars.AB_TESTING_VARIANT.variable] = it }

        // Add standard params for this event
        for (count in 0 until standardParams.length()) {
            val key = standardParams[count].toString()
            val value = measurementMap[key]
            if (value != null && value.toString().isNotBlank()) { // skip param if empty value
                processedMap.setEvar(key, value.toString())
            }
        }

        // Add required params for this event
        for (count in 0 until requiredParams.length()) {
            val key = requiredParams[count].toString()
            val value = measurementMap[key]
            if (value != null && value.toString().isNotBlank()) { // skip param if empty value
                processedMap.setEvar(key, value.toString())
            }
        }

        for (param in measurementMap) {
            val value = measurementMap[param.key]
            if (value != null && value.toString().isNotBlank()) { // skip param if empty value
                processedMap.setEvar(param.key, value.toString())
            }
        }
        return processedMap
    }

    private fun validateNewMap(
        event: String,
        formattedEventName: String,
        standardParams: JSONArray,
        requiredParams: JSONArray,
        measurementMap: MeasurementMap,
    ) {
        // Verify required params defined or not in json
        val paramsDefinedInJson = (eventTypes.optInt(event.uppercase(), 0) != 0) &&
                requiredEventParams.optJSONArray(formattedEventName) != null

        // Verify standard params
        val missingStandardParamsList = mutableSetOf<String>()
        for (index in 0 until standardParams.length()) {
            val key = standardParams[index].toString()
            val value = measurementMap[key]
            if (value == null || value.toString().isEmpty()) {
                missingStandardParamsList.add(key)
            }
        }

        // Verify required params
        val missingRequiredParamsList = mutableSetOf<String>()
        for (index in 0 until requiredParams.length()) {
            val key = requiredParams[index].toString()
            val value = measurementMap[key]
            if (value == null || value.toString().isEmpty()) {
                missingRequiredParamsList.add(key)
            }
        }

        val tag = "FirebaseTrackingManager"
        val missingAnyParams = !paramsDefinedInJson ||
                missingStandardParamsList.size > 0 || missingRequiredParamsList.size > 0
        val msg =
            "Validation event=$event, page_name=${measurementMap[Evars.PAGE_NAME.variable]},\n" +
                    " params_count=${standardParams.length() + requiredParams.length()}, \n" +
                    " standard_params=$standardParams, \n" +
                    " required_params=$requiredParams, \n" +
                    " missing_standard_params=${missingStandardParamsList},\n" +
                    " missing_required_params=$missingRequiredParamsList\n"
        if (missingAnyParams) Logger.w(tag, msg) else Logger.i(tag, msg)
    }
}
