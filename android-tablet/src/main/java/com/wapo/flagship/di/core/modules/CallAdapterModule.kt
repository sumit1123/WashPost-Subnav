package com.wapo.flagship.di.core.modules

import com.wapo.flagship.network.retrofit.network.CallAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.CallAdapter
import javax.inject.Singleton

/**
 * This module provides the custom call adapter factory for retrofit module.
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class CallAdapterModule {
    @Singleton
    @Binds
    abstract fun provideCallAdapterFactory(factory: CallAdapterFactory): CallAdapter.Factory
}
