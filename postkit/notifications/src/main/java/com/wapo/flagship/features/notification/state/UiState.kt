package com.wapo.flagship.features.notification.state

sealed class UiState {
    object NotificationsScreen: UiState()
    object AlertSettingsScreen: UiState()
}