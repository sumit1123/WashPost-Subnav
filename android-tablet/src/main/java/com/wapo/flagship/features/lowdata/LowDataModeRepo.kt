package com.wapo.flagship.features.lowdata

import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.LowDataModeNotificationState

import kotlinx.coroutines.flow.Flow

interface LowDataModeRepo {

    val config: Flow<LowDataModeNotificationConfigurationImpl>

    suspend fun dismissNotification(): LowDataModeNotificationConfigurationImpl

    suspend fun setLowDataModeNotificationState(newState: LowDataModeNotificationState): LowDataModeNotificationConfigurationImpl

}