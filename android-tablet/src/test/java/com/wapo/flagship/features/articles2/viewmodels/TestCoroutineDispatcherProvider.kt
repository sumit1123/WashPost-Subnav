package com.wapo.flagship.features.articles2.viewmodels

import com.wapo.flagship.util.coroutines.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * This class is the implementation of real [DispatcherProvider] that provides fake/test [kotlinx.coroutines.CoroutineDispatcher]
 * to launch coroutines in tests.
 */
class TestCoroutineDispatcherProvider
    @ExperimentalCoroutinesApi
    constructor(
        val testCoroutineDispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : DispatcherProvider {
        override val io: CoroutineContext = testCoroutineDispatcher
        override val main: CoroutineContext = testCoroutineDispatcher
        override val default: CoroutineContext = testCoroutineDispatcher
    }
