package com.wapo.flagship.di.app.modules.features.articles2

import android.content.Context
import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.articles2.repo.ArticlesSaveRepoImpl
import com.wapo.flagship.features.articles2.repo.DebugArticleRepository
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.roomdb.AppDatabase
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * This module provides the [AppDatabase] required by room in order to carry out all the database related operations.
 */
@InstallIn(SingletonComponent::class)
@Module
object Article2RepositoryModule {
    @Singleton
    @Provides
    fun provideArticleRepository(
        articles2Service: Articles2Service,
        appDatabase: AppDatabase,
        coroutineScopeProvider: CoroutineScopeProvider,
    ): Articles2Repository = DebugArticleRepository(articles2Service, appDatabase, coroutineScopeProvider)

    @Singleton
    @Provides
    fun provideArticlesSaveRepo(
        @ApplicationContext applicationContext: Context
    ): ArticlesSaveRepo = ArticlesSaveRepoImpl(applicationContext.cacheDir)
}
