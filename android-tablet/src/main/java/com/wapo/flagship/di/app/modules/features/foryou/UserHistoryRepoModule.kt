/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.di.app.modules.features.foryou

import android.content.Context
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.flagship.features.foryou.UserHistoryMetaProviderImpl
import com.washingtonpost.userhistory.remote.UserHistoryService
import com.washingtonpost.userhistory.repo.UserHistoryPrefRepoImpl
import com.washingtonpost.userhistory.repo.UserHistoryPrefRepoImpl.Companion.USER_HISTORY_PREFS_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object UserHistoryRepoModule {

    @Singleton
    @Provides
    fun provideUserHistoryPrefRepo(
        @ApplicationContext applicationContext: Context,
        userHistoryService: UserHistoryService,
        deviceUtilRepo: DeviceUtilRepo,
        utilRepo: UtilsRepo
    ): UserHistoryPrefRepoImpl {
        return UserHistoryPrefRepoImpl(
            applicationContext.getSharedPreferences(
                USER_HISTORY_PREFS_NAME,
                Context.MODE_PRIVATE
            ),
            userHistoryService,
            UserHistoryMetaProviderImpl(
                deviceUtilRepo,
                utilRepo
            )
        )
    }
}
