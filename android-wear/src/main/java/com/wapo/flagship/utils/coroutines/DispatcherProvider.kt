/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils.coroutines

import kotlin.coroutines.CoroutineContext

/**
 * Provides relevant dispatchers. Allows us to change the dispatchers out for testing
 * if need be.
 */
interface DispatcherProvider {
    val io: CoroutineContext
    val main: CoroutineContext
    val default: CoroutineContext
}