package com.wapo.flagship.di.app.modules.features.purchasedarticles

import android.content.Context
import com.wapo.flagship.features.purchasedarticles.db.PurchasedArticleDao
import com.wapo.flagship.features.purchasedarticles.repo.PurchasedArticleManager
import com.wapo.flagship.features.purchasedarticles.repo.PurchasedArticlesRepository
import com.wapo.flagship.features.purchasedarticles.service.PurchasedArticlesService
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.save.network.SavedRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PurchasedArticlesModule {

    @Singleton
    @Provides
    fun providePurchasedArticleManager(
        @ApplicationContext context: Context,
        service: PurchasedArticlesService,
        metadataNetwork: SavedRetrofit.MetadataNetwork,
        purchasedArticleDao: PurchasedArticleDao,
        coroutineScopeProvider: CoroutineScopeProvider,
    ): PurchasedArticleManager = PurchasedArticleManager(
        context, service, metadataNetwork, purchasedArticleDao,
        coroutineScopeProvider
    )

    @Singleton
    @Provides
    fun providePurchasedArticleRepository(
        purchasedArticleManager: PurchasedArticleManager
    ): PurchasedArticlesRepository = PurchasedArticlesRepository(purchasedArticleManager)
}