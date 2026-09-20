package com.wapo.flagship.di.app.modules.coroutines

import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.wapo.flagship.util.coroutines.CoroutineScopeProviderImpl
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.util.coroutines.PaywallCoroutineScopeProvider
import com.washingtonpost.android.paywall.util.coroutines.PaywallCoroutineScopeProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object CoroutineScopeModule {
    @Singleton
    @Provides
    fun provideCoroutineScope(dispatcherProvider: DispatcherProvider): CoroutineScopeProvider =
        CoroutineScopeProviderImpl(dispatcherProvider)

    @Singleton
    @Provides
    fun providePaywallCoroutineScope(): PaywallCoroutineScopeProvider =
        PaywallCoroutineScopeProviderImpl()

    @CoroutineScopeCommonsModule.IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
