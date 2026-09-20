package com.wapo.flagship.util.coroutines

import kotlinx.coroutines.CoroutineScope

class CoroutineScopeProviderImpl(
    private val dispatcherProvider: DispatcherProvider,
) : CoroutineScopeProvider {
    override val sync: CoroutineScope
        get() = CoroutineScope(dispatcherProvider.io)
}
