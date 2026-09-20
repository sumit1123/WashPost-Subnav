/*
 * Copyright (c) 2019. The Washington Post
 */
package com.washingtonpost.android.paywall.reminder

import android.os.SystemClock
import androidx.annotation.NonNull
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.washingtonpost.android.config.domain.models.config.paywall.ReminderScreenConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.reminder.ReminderScreenFragment.ReminderType.*
import com.washingtonpost.android.paywall.reminder.ReminderScreenFragment.*

class ReminderScreenViewModel : ViewModel() {
    enum class Event {
        CLOSE, SIGN_IN
    }

    val events = LiveEvent<Event?>()

    companion object {
        fun areConditionsMet(reminderType: ReminderType,
                             @NonNull preferenceStorage: ReminderScreenStorage,
                             @NonNull reminderScreenConfig: ReminderScreenConfig,
                             @NonNull bypassElapsedTimeChecks: Boolean = false): Boolean {
            val isSubscriptionActive = PaywallService.getInstance().isSubActive
            val isUserSignedIn = PaywallService.getInstance().isWpUserLoggedIn
            return when (reminderType) {
                IAP_REGISTRATION_ASK -> {
                    // Show when firstTime && subscription purchased && not logged-in
                    val firstTime = preferenceStorage.iapRegistrationAskShownTime == -1L
                    firstTime && isSubscriptionActive && !isUserSignedIn
                }
                IAP_REGISTRATION_ASK_REMINDER -> {
                    // Show when subscription purchased && not logged-in && shownAskXHrsBefore && shownReminderXHrsBefore
                    val elapsedRealtime = SystemClock.elapsedRealtime()
                    val shownAskXHrsBefore = elapsedRealtime - preferenceStorage.iapRegistrationAskShownTime > reminderScreenConfig.frequency
                    val shownReminderXHrsBefore = elapsedRealtime - preferenceStorage.iapRegistrationAskReminderShownTime > reminderScreenConfig.frequency
                    isSubscriptionActive && !isUserSignedIn &&
                        (bypassElapsedTimeChecks || shownAskXHrsBefore) &&
                        (bypassElapsedTimeChecks || shownReminderXHrsBefore)
                }
            }
        }
    }
}