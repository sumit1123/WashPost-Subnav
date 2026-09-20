package com.wapo.flagship.features.lowdata

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.Companion.DISMISS_START_COUNTER
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.Companion.gson
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.LowDataModeNotificationState
import com.wapo.flagship.util.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LowDataModeDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
): LowDataModeDataStore {


    private val lowDataModeNotificationHelperConfig: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[LOW_DATA_MODE_NOTIFICATION_CONFIG_KEY] ?: ""
        }

    override suspend fun saveConfiguration(newValue: String) {
        context.dataStore.edit { preferences ->
            preferences[LOW_DATA_MODE_NOTIFICATION_CONFIG_KEY] = newValue
        }
    }

    override suspend fun getLowDataModeConfig(): LowDataModeNotificationConfigurationImpl {
        val preferenceValue = lowDataModeNotificationHelperConfig.first()
        return if (preferenceValue.isEmpty()) {
            LowDataModeNotificationConfigurationImpl(
                notificationState = LowDataModeNotificationState.Allow,
                dismissCounter = DISMISS_START_COUNTER,
            )
        } else {
            val savedValue =
                gson.fromJson(
                    preferenceValue,
                    LowDataModeNotificationConfigurationImpl::class.java,
                )
            if (savedValue.isLowDataModeDialogVisible()) {
                val savedValueCopy = savedValue.copy(
                    notificationState = LowDataModeNotificationState.Allow,
                )
                LowDataModeNotificationConfigurationImpl(
                    savedValueCopy.notificationState, savedValueCopy.dismissCounter
                )
            } else {
                LowDataModeNotificationConfigurationImpl(
                    savedValue.notificationState,
                    savedValue.dismissCounter
                )
            }
        }.also {
            saveConfiguration(gson.toJson(it))
        }
    }

    companion object {
        // LOW_DATA_MODE_NOTIFICATION
        private val LOW_DATA_MODE_NOTIFICATION_CONFIG = "lowdatamode_notification_config"
        private val LOW_DATA_MODE_NOTIFICATION_CONFIG_KEY = stringPreferencesKey(LOW_DATA_MODE_NOTIFICATION_CONFIG)
    }
}