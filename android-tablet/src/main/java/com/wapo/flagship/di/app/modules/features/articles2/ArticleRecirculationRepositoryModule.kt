package com.wapo.flagship.di.app.modules.features.articles2

import com.wapo.flagship.features.articles2.interfaces.ArticleRecirculationRepository
import com.wapo.flagship.features.articles2.repo.ArticleRecirculationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class ArticleRecirculationRepositoryModule {
    @Singleton
    @Binds
    abstract fun bindRecirculationRepository(
        recirculationRepositoryImpl: ArticleRecirculationRepositoryImpl
    ): ArticleRecirculationRepository
}