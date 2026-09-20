/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.wapo.flagship.network.CallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.CallAdapter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CallAdapterModule {

    @Provides
    @Singleton
    fun provideCallAdapterFactory(): CallAdapter.Factory =
        CallAdapterFactory()

}