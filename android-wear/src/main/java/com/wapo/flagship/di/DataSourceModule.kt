/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.wapo.flagship.features.articles2.datasource.local.Articles2LocalDataSource
import com.wapo.flagship.features.articles2.datasource.local.Articles2LocalDataSourceImpl
import com.wapo.flagship.features.articles2.datasource.remote.Articles2RemoteDataSource
import com.wapo.flagship.features.articles2.datasource.remote.Articles2RemoteDataSourceImpl
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.section.datasource.remote.SectionRemoteDataSource
import com.wapo.flagship.features.section.datasource.remote.SectionRemoteDataSourceImpl
import com.wapo.flagship.features.section.services.FusionSectionService
import com.wapo.flagship.features.section.services.PageBuilderSectionService
import com.wapo.flagship.roomdb.AppDatabase
import com.wapo.flagship.utils.coroutines.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideSectionRemoteDataSource(
        fusionSectionService: FusionSectionService,
        pageBuilderSectionService: PageBuilderSectionService,
        dispatcherProvider: DispatcherProvider
    ): SectionRemoteDataSource {
        return SectionRemoteDataSourceImpl(
            fusionSectionService,
            pageBuilderSectionService,
            dispatcherProvider
        )
    }

    @Provides
    @Singleton
    fun provideArticles2RemoteDataSource(
        articles2Service: Articles2Service
    ): Articles2RemoteDataSource {
        return Articles2RemoteDataSourceImpl(
            articles2Service
        )
    }

    @Provides
    @Singleton
    fun provideArticles2LocalDataSource(
        appDatabase: AppDatabase
    ): Articles2LocalDataSource {
        return Articles2LocalDataSourceImpl(
            appDatabase
        )
    }


}