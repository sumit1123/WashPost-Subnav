package com.wapo.flagship.di.app.modules

import com.wapo.android.commons.util.CommonsTrackingProvider
import com.wapo.flagship.util.CommonsTrackingProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * This module provides the [CommonsTrackingProvider] in order to make tracking calls from android-commons.
 */
@InstallIn(SingletonComponent::class)
@Module
object CommonsTrackingModule {
    @Singleton
    @Provides
    fun provideCommonsTracking(): CommonsTrackingProvider = CommonsTrackingProviderImpl()
}
