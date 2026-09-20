package com.wapo.flagship.features.subscribebanner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.grid.model.GlobalBannerMessage
import com.wapo.flagship.features.grid.model.GlobalBannerState
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject

/**
 * View Model that drives the state of the Global CTA Banner and also facilitates the communication (through Click Events)
 * between the GridAdapter and MainActivity.
 */
@HiltViewModel
class GlobalBannerViewModel
@Inject
constructor() : ViewModel() {
    private val _bannerEvent: MutableLiveData<BannerEvent> = MutableLiveData()
    val bannerEvent: LiveData<BannerEvent> = _bannerEvent

    /**
     * Sub Status LiveData that drives the type of banner and content shown on it.
     */
    private val _globalBannerState: MutableLiveData<GlobalBannerState> =
        MutableLiveData(GlobalBannerState.Unknown)
    val globalBannerState: LiveData<GlobalBannerState> = _globalBannerState

    /**
     * Set the appropriate [GlobalBannerState] given:
     * - configHideBanner = hide banner based on frontSubscriptionBanner from config
     * - hasSub = user has ordinary Active Sub
     * - isInGracePeriod = IAP Sub is suspended
     * - isOnHold = IAP Sub is Inactive and On Hold
     * - isTerminated = Determines if user should be treated as new Subscriber or Terminated Sub
     * - offerTitle = Header text for GSB from the offer provided by message or GlobalBannerConfig
     * - offerSubtitle = Subheader text for GSB from the offer provided by messages or GlobalBannerConfig
     * - offerText = Offer details (e.g. Resubscribe for $2.99) for GSB CTA from the offer provided by messages or GlobalBannerConfig
     * - isPaused = IAP Sub is Paused
     * - isPauseScheduled = IAP Sub is Active but scheduled to Pause
     * - autoResumeTime = time when a Paused sub is scheduled to automatically resume
     * - pauseTime = time when an Active sub is scheduled to Pause
     * - isMobileFreeTrial - user is on a time based free trial only for apps
     * - mobileFreeTrialTime - time when mobile free trial ends
     */
    fun setGlobalBannerState(
        hasSub: Boolean,
        isInGracePeriod: Boolean,
        isOnHold: Boolean,
        isTerminated: Boolean,
        isSignedIn: Boolean,
        globalBannerMessage: GlobalBannerMessage,
        isPlayStorePaused: Boolean,
        isPlayStorePauseScheduled: Boolean,
        isSitePaused: Boolean,
        isSitePauseScheduled: Boolean,
        autoResumeTime: Long,
        pauseTime: Long,
        isMobileFreeTrial: Boolean,
        mobileFreeTrialTime: Date?,
        configHideBanner: Boolean,
    ) {
        _globalBannerState.value =
            when {
                hasSub -> GlobalBannerState.Sub(globalBannerMessage)
                configHideBanner -> GlobalBannerState.ConfigHideBanner
                else ->
                    GlobalBannerState.NoSub(
                        isSignedIn,
                        isTerminated,
                        globalBannerMessage
                    )
            }
    }

    fun setBannerEvent(bannerEvent: BannerEvent) {
        _bannerEvent.value = bannerEvent
    }
}
