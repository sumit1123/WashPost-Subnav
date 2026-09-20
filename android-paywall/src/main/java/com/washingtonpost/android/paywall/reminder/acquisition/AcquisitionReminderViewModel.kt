package com.washingtonpost.android.paywall.reminder.acquisition

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderCtaDestination
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderModel
import com.washingtonpost.android.paywall.BaseSubViewModel
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.bottomsheet.SubState
import com.washingtonpost.android.paywall.util.PaywallConstants
import java.lang.Exception

class AcquisitionReminderViewModel : BaseSubViewModel() {

    val signOnVisibleLiveData = MutableLiveData<Boolean>()
    val sku
    get() = PaywallService.getInstance().acquisitionReminderModel.productId

    override fun update() {
        super.update()
        signOnVisibleLiveData.value = !PaywallService.getInstance().isWpUserLoggedIn
    }

    fun getCTATextLiveData() : LiveData<String> {
        return subStateLiveData.map {
            var textValue = "Empty"
            PaywallService.getInstance().acquisitionReminderModel?.apply {
                textValue = updateCTAText(this.productId, it, PaywallConstants.CTA_ACQUISITION_REMINDER)
            } ?: PaywallService.getConnector().logHandledException(Exception(MODEL_EMPTY))
            textValue
        }
    }

    fun getHeader() : LiveData<String> {
        return subStateLiveData.map { screen:SubState->
            var textValue = "Empty"
            PaywallService.getInstance().acquisitionReminderModel?.apply {
                textValue = when (screen) {
                    SubState.NoSub -> this.newSubscriber.heading
                    SubState.TerminatedSub -> this.terminatedSubscriber.heading
                    SubState.FreeTrialSub -> {
                        if(isIapTerminated) {
                            this.terminatedSubscriber.heading
                        } else {
                            this.newSubscriber.heading
                        }
                    }
                    SubState.ActiveSub, SubState.PausedSub -> {
                        PaywallService.getConnector()
                            .logHandledException(Exception(ACTIVE_SUB_ERROR))
                        "User already has subscription"
                    }
                }
            } ?: PaywallService.getConnector().logHandledException(Exception(MODEL_EMPTY))
            textValue
        }
    }

    fun getMessage() : LiveData<String> {
        return subStateLiveData.map { screen:SubState->
            var textValue = "Empty"
            PaywallService.getInstance().acquisitionReminderModel?.apply {
                textValue = when (screen) {
                    SubState.NoSub -> this.newSubscriber.message
                    SubState.TerminatedSub -> this.terminatedSubscriber.message
                    SubState.FreeTrialSub -> {
                        if (isIapTerminated) {
                            this.terminatedSubscriber.message
                        } else {
                            this.newSubscriber.message
                        }
                    }
                    SubState.ActiveSub, SubState.PausedSub -> {
                        PaywallService.getConnector()
                            .logHandledException(Exception(ACTIVE_SUB_ERROR))
                        "User already has subscription"
                    }
                }
            } ?: PaywallService.getConnector().logHandledException(Exception(MODEL_EMPTY))
            textValue
        }
    }

    fun getCTASubscribeActionType() : AcquisitionReminderCtaDestination {
        return PaywallService.getInstance().acquisitionReminderModel.ctaDestination
    }

    companion object {
        /**
         * Conditions required to show this screen:
         * 1. Only display for non-subscribers
         * 2. Only display on direct app launch
         * 3. Do not display if user saw first install onboarding or “whats new” screens that session
         * 4. Do not display to users with Paused subscriptions
         */
        @JvmStatic
        fun areConditionsMet(acquisitionReminderModel: AcquisitionReminderModel, acquisitionReminderStorage: AcquisitionReminderStorage, bypassElapsedTimeChecks:Boolean): Boolean {
            val isNonSubscriber = !PaywallService.getInstance().isPremiumUser
            val isPaused = PaywallService.getInstance().isSubscriptionPaused
            // The first time the onboarding acquisition screen will be shown so updated stored state and don't show reminder.
            if (acquisitionReminderStorage.iapRegistrationAskReminderShownTime == -1L) {
                acquisitionReminderStorage.iapRegistrationAskReminderShownTime = SystemClock.elapsedRealtime()
            }
            val elapsedRealtime = SystemClock.elapsedRealtime()
            val shownReminderXHrsBefore = elapsedRealtime - acquisitionReminderStorage.iapRegistrationAskReminderShownTime > acquisitionReminderModel.frequency

            return isNonSubscriber && !isPaused
                    && (bypassElapsedTimeChecks || shownReminderXHrsBefore)
        }

        const val ACTIVE_SUB_ERROR = "Error : User has subscription. Should not see this message."
        const val MODEL_EMPTY = "Error : AcquisitionReminderModel is empty."
    }
}

