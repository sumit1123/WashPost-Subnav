package com.wapo.flagship.snackbars.model

import com.wapo.flagship.features.lowdata.LowDataModeNotificationConfigurationImpl

data class SnackBarUIState(
    val snackBarType: SnackBarType? = null,
    val noNetworkConnection: Boolean = false,
    val lowDataModeNotificationConfig: LowDataModeNotificationConfigurationImpl? = null
)