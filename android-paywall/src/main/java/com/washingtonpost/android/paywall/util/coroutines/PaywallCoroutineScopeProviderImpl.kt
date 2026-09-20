package com.washingtonpost.android.paywall.util.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PaywallCoroutineScopeProviderImpl :
    PaywallCoroutineScopeProvider {
    override val sync: CoroutineScope
        get() = CoroutineScope(Dispatchers.IO)
}
