package com.wapo.flagship.features.onboarding2.composable

sealed class PushAlertPromptEvent() {

    data class OnNotificationIconClick(
        val response: Boolean = false
    ) : PushAlertPromptEvent()
}
