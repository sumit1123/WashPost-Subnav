/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.wapo.flagship.features.articles2.datasource.local.Articles2LocalDataSource
import com.wapo.flagship.features.articles2.datasource.remote.Articles2RemoteDataSource
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.articles2.repo.Articles2RepositoryImpl
import com.wapo.flagship.features.section.datasource.remote.SectionRemoteDataSource
import com.wapo.flagship.features.section.repo.SectionRepository
import com.wapo.flagship.features.section.repo.SectionRepositoryImpl
import com.wapo.flagship.utils.coroutines.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSectionRepository(
        sectionRemoteDataSource: SectionRemoteDataSource
    ): SectionRepository {
        return SectionRepositoryImpl(sectionRemoteDataSource)
    }

    @Provides
    @Singleton
    fun provideArticles2Repository(
        articles2RemoteDataSource: Articles2RemoteDataSource,
        articles2LocalDataSource: Articles2LocalDataSource,
        dispatcherProvider: DispatcherProvider
    ): Articles2Repository {
        return Articles2RepositoryImpl(
            articles2RemoteDataSource,
            articles2LocalDataSource,
            dispatcherProvider
        )
    }


}