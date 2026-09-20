/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.android.commons.logs

/**
 * Helper Class to construct a comma separated key values string for RemoteLog.
 * Also provides common setters that the app can use to build the dataString faster.
 * Note: There is a default data that all logs have that is constructed by the RemoteLog module.
 * This class is for constructing app/module/feature/metric specific data.
 */
class EventLog private constructor(
    val dataString: String,
    private val eventData: LinkedHashMap<String, Any?>
) {

    fun isForceUpload(): Boolean = eventData[LogKeys.FORCE_UPLOAD.keyName] as? Boolean ?: false
    fun getSampling(): String? = eventData[LogKeys.SAMPLING_RATE.keyName]?.toString()
    fun getEventData(): Map<String, Any?> = eventData.toMap()

    class Builder {
        private val eventData: LinkedHashMap<String, Any?> = LinkedHashMap()

        fun get(key: String?): Any? {
            return key?.run { eventData[this] }
        }

        fun set(key: String?, value: Any?): Builder {
            if (!key.isNullOrEmpty()) eventData[key] = value
            return this
        }

        fun setModule(value: LogModules): Builder = set(LogKeys.MODULE.keyName, value.name)

        fun setMessage(value: Any?): Builder = set(LogKeys.MESSAGE.keyName, value)

        fun setErrorMessage(value: String?): Builder = set(LogKeys.ERROR_MESSAGE.keyName, value)

        fun setErrorCode(value: Int?): Builder = set(LogKeys.ERROR_CODE.keyName, value)

        fun setUUID(value: String?): Builder = set(LogKeys.UUID.keyName, value)

        fun setContentUrl(value: String?): Builder = set(LogKeys.CONTENT_URL.keyName, value)

        fun setForceUpload(): Builder = set(LogKeys.FORCE_UPLOAD.keyName, true)

        fun setSampling(sampling: Sampling): Builder = set(LogKeys.SAMPLING_RATE.keyName, sampling.key)

        fun build(): EventLog {
            val sb = StringBuilder()
            val itr: Iterator<Map.Entry<String, Any?>> = eventData.entries.iterator()
            while (itr.hasNext()) {
                val (key, value) = itr.next()
                sb.append("$key=\"$value\"")
                if (itr.hasNext()) sb.append(", ")
            }
            return EventLog(sb.toString(), eventData)
        }

        override fun toString(): String {
            return eventData.toString()
        }
    }
}

