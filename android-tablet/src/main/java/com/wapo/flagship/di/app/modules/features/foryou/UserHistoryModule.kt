/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.di.app.modules.features.foryou

import com.washingtonpost.userhistory.domain.UserHistoryPrefRepo
import com.washingtonpost.userhistory.repo.UserHistoryPrefRepoImpl
import com.washingtonpost.userhistory.domain.UserHistoryManager
import com.washingtonpost.userhistory.repo.UserHistoryManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UserHistoryModule {

    @Binds
    abstract fun bindUserHistoryManager(
        userHistoryManagerImpl: UserHistoryManagerImpl
    ): UserHistoryManager

    @Binds
    abstract fun bindUserHistoryPrefRepo(
        userHistoryPrefRepoImpl: UserHistoryPrefRepoImpl
    ): UserHistoryPrefRepo
}
