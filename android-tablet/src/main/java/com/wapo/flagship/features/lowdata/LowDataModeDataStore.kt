package com.wapo.flagship.features.lowdata

interface LowDataModeDataStore {
    suspend fun saveConfiguration(newValue: String)
    suspend fun getLowDataModeConfig(): LowDataModeNotificationConfigurationImpl
}