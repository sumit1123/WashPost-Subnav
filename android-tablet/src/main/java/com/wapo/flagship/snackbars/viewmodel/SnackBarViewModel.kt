package com.wapo.flagship.snackbars.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.LowDataModeNotificationState
import com.wapo.flagship.snackbars.model.SnackBarUIState
import com.wapo.flagship.snackbars.model.SnackBarType
import com.wapo.flagship.features.lowdata.LowDataModeRepo
import com.wapo.flagship.util.tracking.Measurement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnackBarViewModel @Inject constructor(
    private val lowDataModeRepo: LowDataModeRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(SnackBarUIState())
    val uiState: StateFlow<SnackBarUIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            lowDataModeRepo.config.collect { config ->
                _uiState.update {
                    it.copy(
                        lowDataModeNotificationConfig = config,
                    )
                }
            }
        }
    }

    fun setNoNetwork(state: Boolean) {
        _uiState.update {
            it.copy(
                noNetworkConnection = state,
                snackBarType = if (state) SnackBarType.NoNetworkConnection() else null
            )
        }
    }

    fun hideSnackBar(pageNameFromTracking: String?) {
        if (uiState.value.snackBarType is SnackBarType.LowDataConnection) {
            dismissLowDataModeNotification()
            trackLowDataModeNotificationDismiss(pageNameFromTracking)
        }
        _uiState.update {
            it.copy(
                snackBarType = null,
            )
        }
    }

    fun showSnackBar(snackBarType: SnackBarType?) {
        snackBarType?.let { snackBarType ->
            _uiState.update {
                it.copy(
                    snackBarType = snackBarType,
                )
            }
            return
        }
        if (uiState.value.noNetworkConnection) {
            _uiState.update {
                it.copy(
                    snackBarType = SnackBarType.NoNetworkConnection(),
                )
            }
        }
    }

    private fun trackLowDataModeNotificationDismiss(pageNameFromTracking: String?) {
        Measurement.trackLowDataModeNotificationDismiss(
            pageNameFromTracking,
        )
    }

    fun isLowDataModeNotificationEnable(isEnable: (enable: Boolean) -> Unit) {
        isEnable(uiState.value.lowDataModeNotificationConfig?.isLowDataModeNotificationEnable() ?: false)
    }

    fun dismissLowDataModeNotification() {
        viewModelScope.launch {
            lowDataModeRepo.dismissNotification()
        }
    }

    fun allowLowDataModeNotification() {
        setLowDataModeNotificationState(
            LowDataModeNotificationState.Allow,
        )
    }

    fun disableLowDataModeNotification() {
        setLowDataModeNotificationState(
            LowDataModeNotificationState.Disable,
        )
    }

    fun snoozeLowDataModeNotification() {
        setLowDataModeNotificationState(
            LowDataModeNotificationState.Snooze(),
        )
    }

    private fun setLowDataModeNotificationState(newState: LowDataModeNotificationState) {
        viewModelScope.launch {
            lowDataModeRepo.setLowDataModeNotificationState(newState)
        }
    }
}
