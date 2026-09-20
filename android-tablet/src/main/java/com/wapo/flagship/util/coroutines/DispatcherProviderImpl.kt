package com.wapo.flagship.util.coroutines

import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * This class is the implementation of real [DispatcherProvider] that provides real [kotlinx.coroutines.CoroutineDispatcher]
 * to launch coroutines.
 */
class DispatcherProviderImpl @Inject constructor() : DispatcherProvider {
    override val io: CoroutineContext = Dispatchers.IO
    override val main: CoroutineContext = Dispatchers.Main
    override val default: CoroutineContext = Dispatchers.Default
}
