package com.wapo.flagship.features.lowdata

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.Companion.DISMISS_START_COUNTER
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class LowDataModeNotificationConfigurationImpl(
    val notificationState: LowDataModeNotificationImpl.LowDataModeNotificationState,
    val dismissCounter: Int,
) {
    fun setAllowState(): LowDataModeNotificationConfigurationImpl =
        copy(notificationState = LowDataModeNotificationImpl.LowDataModeNotificationState.Allow)

    fun isLowDataModeNotificationEnable(): Boolean =
        when (val state = notificationState) {
            LowDataModeNotificationImpl.LowDataModeNotificationState.Allow -> true
            LowDataModeNotificationImpl.LowDataModeNotificationState.Disable -> false
            is LowDataModeNotificationImpl.LowDataModeNotificationState.Snooze -> {
                setAllowState()
                state.isTimingComplete()
            }
            LowDataModeNotificationImpl.LowDataModeNotificationState.Dialog -> false
        }

    fun isLowDataModeDialogVisible() = notificationState is LowDataModeNotificationImpl.LowDataModeNotificationState.Dialog

    fun dismissNotification(): LowDataModeNotificationConfigurationImpl =
        copy(dismissCounter = dismissCounter - 1)

    fun willExhaustDismissCounterOnNextDismiss(): Boolean = dismissCounter - 1 == 0

    fun resetCounter(): LowDataModeNotificationConfigurationImpl =
        copy(dismissCounter = DISMISS_START_COUNTER)

}

class LDMCTypeAdapter : TypeAdapter<LowDataModeNotificationConfigurationImpl>() {
    private val gson: Gson = Gson()

    override fun write(
        out: JsonWriter,
        value: LowDataModeNotificationConfigurationImpl?,
    ) {
        out.beginObject()
        out.name(FIELD_DISMISS_COUNTER)
        gson.getAdapter(Int::class.java).write(out, value?.dismissCounter)
        out.name(FIELD_NOTIFICATION_STATE)
        value?.notificationState?.let { state ->
            gson.getAdapter(String::class.java).write(out, state.id)
            out.name(FIELD_SNOOZE_DATE)
            val snoozeValue =
                if (state is LowDataModeNotificationImpl.LowDataModeNotificationState.Snooze) {
                    state.timingDate.toString()
                } else {
                    ""
                }
            gson.getAdapter(String::class.java).write(out, snoozeValue)
            out.name(FIELD_SNOOZE_TIME)
            val snoozeTime =
                if (state is LowDataModeNotificationImpl.LowDataModeNotificationState.Snooze) {
                    state.snoozeTime.toString()
                } else {
                    ""
                }
            gson.getAdapter(String::class.java).write(out, snoozeTime)
        }
        out.endObject()
    }

    override fun read(reader: JsonReader): LowDataModeNotificationConfigurationImpl {
        var dismissCounterValue = 2
        var notificationStateId = LowDataModeNotificationImpl.Companion.ID_ALLOW
        var snoozeDate = ""
        var snoozeTime = ""
        var notificationState: LowDataModeNotificationImpl.LowDataModeNotificationState = LowDataModeNotificationImpl.LowDataModeNotificationState.Allow

        reader.beginObject()
        while (reader.hasNext()) {
            val fieldName = reader.nextName()

            when (fieldName) {
                FIELD_DISMISS_COUNTER -> {
                    dismissCounterValue = gson.getAdapter(Int::class.java).read(reader)
                }
                FIELD_NOTIFICATION_STATE -> {
                    notificationStateId = gson.getAdapter(String::class.java).read(reader)
                }
                FIELD_SNOOZE_DATE -> {
                    snoozeDate = gson.getAdapter(String::class.java).read(reader)
                }
                FIELD_SNOOZE_TIME -> {
                    val value = gson.getAdapter(String::class.java).read(reader)
                    snoozeTime =
                        if (value.isNullOrEmpty()) {
                            LowDataModeNotificationImpl.Companion.SNOOZE_HOURS.toString()
                        } else {
                            value
                        }
                }
                else ->
                    reader.skipValue()
            }
        }

        notificationState =
            when (notificationStateId) {
                LowDataModeNotificationImpl.Companion.ID_ALLOW -> LowDataModeNotificationImpl.LowDataModeNotificationState.Allow
                LowDataModeNotificationImpl.Companion.ID_DISABLE -> LowDataModeNotificationImpl.LowDataModeNotificationState.Disable
                LowDataModeNotificationImpl.Companion.ID_DIALOG -> LowDataModeNotificationImpl.LowDataModeNotificationState.Dialog
                LowDataModeNotificationImpl.Companion.ID_SNOOZE -> {
                    val calendar = Calendar.getInstance()
                    val timingDate =
                        SimpleDateFormat(
                            DATE_FORMAT,
                            Locale.US,
                        ).parse(snoozeDate)

                    timingDate?.let {
                        calendar.time = it
                    }

                    LowDataModeNotificationImpl.LowDataModeNotificationState.Snooze(
                        snoozeTime = snoozeTime.toInt(),
                        timingDate = calendar.time,
                    )
                }
                else -> {
                    LowDataModeNotificationImpl.LowDataModeNotificationState.Allow
                }
            }

        reader.endObject()

        return LowDataModeNotificationConfigurationImpl(notificationState, dismissCounterValue)
    }

    companion object {
        private const val FIELD_DISMISS_COUNTER = "dismissCounter"
        private const val FIELD_NOTIFICATION_STATE = "notificationState"
        private const val FIELD_SNOOZE_DATE = "snoozeDate"
        private const val FIELD_SNOOZE_TIME = "snoozeTime"
        private const val DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy"
    }
}