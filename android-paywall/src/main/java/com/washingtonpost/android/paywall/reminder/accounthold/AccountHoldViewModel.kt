package com.washingtonpost.android.paywall.reminder.accounthold

import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.wapo.android.commons.logs.EventLog
import com.washingtonpost.android.paywall.BaseSubViewModel
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallUtil

class AccountHoldViewModel : BaseSubViewModel() {

    val signOnVisibleLiveData = MutableLiveData<Boolean>()
    val accountHoldStatus = MutableLiveData<Boolean>()
    val productId
        get() = PaywallService.getInstance().acquisitionReminderModel.productId

    override fun update() {
        super.update()
        signOnVisibleLiveData.value = !PaywallService.getInstance().isWpUserLoggedIn
        accountHoldStatus.value = !PaywallService.getInstance().isSubOnHold
    }

    fun getPrice(): String? {
        var subscriptionPrice = "$11.99/month"

        val subscription = PaywallService.getBillingHelper().lastActiveSubscription
        subscription?.let {
            val iapSubItem = PaywallService.getConnector().iapSubItems.getItem(it.storeProductId)
            iapSubItem?.apply {
                iapSubItem.productId?.let { productId ->
                    iapSubItem.basePrice?.let { price ->
                        subscriptionPrice = PaywallUtil.getFormattedPricePerPeriod(price, this.subscriptionPeriod, productId) ?: "$11.99/month"
                    } ?: PaywallService.getConnector().logE(EventLog.Builder().setErrorMessage("$TAG: price is null for $productId"))
                } ?: PaywallService.getConnector().logE(EventLog.Builder().setErrorMessage("$TAG: cannot set subscriptionPrice because productId is null"))
            }
        }
        return subscriptionPrice
    }

    fun getTitle(): String {
        val subscription = PaywallService.getBillingHelper().lastActiveSubscription

        return getPaywallTitle(subscription?.storeProductId)
    }

    private fun getPaywallTitle(storeProductId: String?): String {
        var title: String = "Core"
        when (storeProductId) {
            "wp.classic.basic", "wp.classic.basic.annual" -> title = "Core"
            "monthly_all_access", "wp.classic.premium.annual" -> title = "Premium"
        }
        return title
    }

    companion object {
        private val TAG = AccountHoldViewModel::class.java.name
        /**
         * Conditions required to show this screen:
         * 1. only display for non-subscribers
         * 2. Only display on direct app launch
         * 3. Do not display if user saw first install onboarding or “whats new” screens that session
         */
        @JvmStatic
        fun areConditionsMet(accountHoldReminderStorage: AccountHoldReminderStorage, bypassElapsedTimeChecks: Boolean, frequency:Long): Boolean {
            val isSubHold = PaywallService.getInstance().isSubOnHold
            // The first time the onboarding acquisition screen will be shown so updated stored state and don't show reminder.
            if (accountHoldReminderStorage.iapRegistrationAskReminderShownTime == -1L) {
                accountHoldReminderStorage.iapRegistrationAskReminderShownTime = SystemClock.elapsedRealtime()
            }
            val elapsedRealtime = SystemClock.elapsedRealtime()
            val shownReminderXHrsBefore = elapsedRealtime - accountHoldReminderStorage.iapRegistrationAskReminderShownTime > frequency

            return isSubHold && (bypassElapsedTimeChecks || shownReminderXHrsBefore)
        }
    }

}

