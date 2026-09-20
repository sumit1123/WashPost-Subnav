package com.wapo.flagship.di.core.modules.coroutines

import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.coroutines.DispatcherProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * This module is responsible for providing the right [DispatcherProvider] based on what application (real or test)
 * is being currently used. In most tests we do not use the [Dispatchers.IO], [Dispatchers.Main] or [Dispatchers.Default].
 * Instead, most tests use the [kotlinx.coroutines.test.TestCoroutineDispatcher]. This way we would be able to mock the real dispatchers with [kotlinx.coroutines.test.TestCoroutineDispatcher]
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class DispatcherModule {
    @Singleton
    @Binds
    abstract fun provideDispatcherProvider(impl: DispatcherProviderImpl): DispatcherProvider
}
