/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils.coroutines

import kotlinx.coroutines.CoroutineScope

class CoroutineScopeProviderImpl(
    private val dispatcherProvider: DispatcherProvider
) : CoroutineScopeProvider {
    override val sync: CoroutineScope
        get() = CoroutineScope(dispatcherProvider.io)
}