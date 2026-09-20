package com.wapo.flagship.features.lowdata

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class LowDataModeNotificationImpl @Inject constructor(
    private var lowDataModeDataStore: LowDataModeDataStore,
) : LowDataModeRepo {


    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _config = MutableStateFlow(
        LowDataModeNotificationConfigurationImpl(
            notificationState = LowDataModeNotificationState.Allow,
            dismissCounter = DISMISS_START_COUNTER,
        )
    )
    override val config = _config.asStateFlow()

    init {
        scope.launch {
            _config.value = lowDataModeDataStore.getLowDataModeConfig()
        }
    }

    override suspend fun dismissNotification(): LowDataModeNotificationConfigurationImpl {
        val newConfig = _config.value.let {
            val newState = if (it.willExhaustDismissCounterOnNextDismiss()) {
                LowDataModeNotificationState.Allow
            } else {
                it.dismissNotification()
                LowDataModeNotificationState.Snooze(snoozeTime = SNOOZE_HOURS_DISMISS)
            }
            it.copy(notificationState = newState)
        }
        _config.update { newConfig }
        lowDataModeDataStore.saveConfiguration(gson.toJson(newConfig))
        return newConfig
    }

    override suspend fun setLowDataModeNotificationState(newState: LowDataModeNotificationState): LowDataModeNotificationConfigurationImpl {
        var newConfig = _config.value.copy(
            notificationState = newState,
        )
        when (newConfig.notificationState) {
            LowDataModeNotificationState.Allow -> {
                newConfig.resetCounter()
                newConfig.setAllowState()
            }

            LowDataModeNotificationState.Disable -> {}
            LowDataModeNotificationState.Dialog -> {
                newConfig.resetCounter()
            }

            is LowDataModeNotificationState.Snooze -> {}
        }
        _config.update { newConfig }
        lowDataModeDataStore.saveConfiguration(gson.toJson(newConfig))
        return newConfig
    }

    sealed class LowDataModeNotificationState(
        val id: String,
    ) {
        data object Allow : LowDataModeNotificationState(ID_ALLOW)

        data object Disable : LowDataModeNotificationState(ID_DISABLE)

        data object Dialog : LowDataModeNotificationState(ID_DIALOG)

        class Snooze(
            val snoozeTime: Int = SNOOZE_HOURS,
            val timingDate: Date =
                getSnoozeTime(
                    snoozeTime,
                ),
        ) : LowDataModeNotificationState(ID_SNOOZE) {
            fun isTimingComplete() = Date().after(timingDate)
        }
    }

    companion object {
        const val ID_ALLOW = "allow"
        const val ID_DISABLE = "disable"
        const val ID_DIALOG = "dialog"
        const val ID_SNOOZE = "snooze"
        const val DISMISS_START_COUNTER = 2
        const val SNOOZE_HOURS = 24
        const val SNOOZE_HOURS_DISMISS = 12

        val gson =
            GsonBuilder()
                .registerTypeAdapter(
                    LowDataModeNotificationConfigurationImpl::class.java,
                    LDMCTypeAdapter(),
                ).create()


        private fun getSnoozeTime(snoozeTime: Int): Date {
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + snoozeTime)
            return calendar.time
        }
    }

}