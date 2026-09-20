/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.onboarding2.models

sealed class OnboardingEvent {

    data class AcceptsPushNotifications(val accepted: Boolean) : OnboardingEvent()
}
