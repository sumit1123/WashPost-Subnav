package com.washingtonpost.android.paywall.util.coroutines

import kotlinx.coroutines.CoroutineScope

interface PaywallCoroutineScopeProvider {
    val sync: CoroutineScope
}
