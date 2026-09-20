package com.washingtonpost.android.paywall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.washingtonpost.android.paywall.bottomsheet.SubState
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil

open class BaseSubViewModel : ViewModel() {

    protected val subStateLiveData = MutableLiveData<SubState>()

    val isIapTerminated
        get() = PaywallService.getInstance() != null && PaywallService.getConnector().iapSubscriptionStatus == PaywallConstants.TERMINATED

    open fun update() {
        subStateLiveData.value = getSubState()
    }

    fun updateCTAText(
        productId: String,
        subState: SubState,
        ctaType: String
    ): String {
        val iapSubItem = PaywallService.getConnector().iapSubItems.getItem(productId)
        return PaywallUtil.getCTAText(
            ctaType = ctaType,
            iapSubItem = iapSubItem,
            subState = subState,
            isIapTerminated = isIapTerminated,
            wallName = null
        )
    }

    fun getSubStateLiveData(): LiveData<SubState> {
        return subStateLiveData.distinctUntilChanged()
    }

    /**
     * Used to customize View with appropriate pricing based on User Sub Status.
     */
    private fun getSubState(): SubState {
        return when {
            PaywallService.getInstance() != null && (PaywallService.getInstance()
                .isFreeArticlesUser || PaywallService.getInstance()
                .isFreeDaysUser || PaywallService.getInstance().isMobileFreeDaysUser) -> SubState.FreeTrialSub
            PaywallService.getInstance() != null && PaywallService.getInstance().isPremiumUser -> SubState.ActiveSub
            PaywallService.getInstance() != null && PaywallService.getInstance().isSubscriptionTerminated -> SubState.TerminatedSub
            PaywallService.getInstance() != null && PaywallService.getInstance().isSubscriptionPaused -> SubState.PausedSub
            else -> SubState.NoSub
        }
    }
}