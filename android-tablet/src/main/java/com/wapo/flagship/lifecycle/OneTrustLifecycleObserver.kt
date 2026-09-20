package com.wapo.flagship.lifecycle

import androidx.lifecycle.Lifecycle
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.onetrust.OneTrustInitializationState
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.paywall.PaywallService

class OneTrustLifecycleObserver(
    lifecycle: Lifecycle,
) : FlagshipLifecycleObserver(lifecycle) {
    private val tag = OneTrustLifecycleObserver::class.java.simpleName

    /**
     * OneTrust init on application start to give as much time as possible for network call.
     * Purpose is to enable analytics as early as possible.
     */
    override fun onApplicationStart() {
        super.onApplicationStart()
        val context = FlagshipApplication.getInstance()
        Logger.d(
            tag,
            "OTDebug, onApplicationStart, isSdkStarted=${OneTrustHelper.initializationState.value?.name}," +
                " paywallInitialized=${PaywallService.initialized()}",
        )
        if (OneTrustHelper.initializationState.value == OneTrustInitializationState.UNINITIALIZED ||
            OneTrustHelper.initializationState.value == OneTrustInitializationState.FAILURE
        ) {
            OneTrustHelper.initSdk(context)
        }
    }
}
