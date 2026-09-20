package com.wapo.flagship.features.notification.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.notification.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AlertsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.NotificationsScreen)
    val uiState: StateFlow<UiState> = _uiState

    private val _customizeAlertsEvent = LiveEvent<Boolean>()
    val customizeAlertsEvent:LiveData<Boolean> = _customizeAlertsEvent

    var showAlertsOnly = false
        set(value) {
            if (value) showScreen(UiState.AlertSettingsScreen)
            field = value
        }

    fun showScreen(uiState: UiState) {
        _uiState.value = uiState
    }

    fun showCustomizeAlertsEvent() {
        _customizeAlertsEvent.value = true
    }

}