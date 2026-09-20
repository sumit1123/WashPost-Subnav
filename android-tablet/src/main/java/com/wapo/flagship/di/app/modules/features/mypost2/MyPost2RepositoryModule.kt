package com.wapo.flagship.di.app.modules.features.mypost2

import android.content.Context
import com.wapo.flagship.features.mypost.SaveProviderImpl
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.repo.MyPost2Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object MyPost2RepositoryModule {
    @Singleton
    @Provides
    fun provideMyPost2Repository(
        @ApplicationContext applicationContext: Context,
        savedArticleManager: SavedArticleManager,
        coroutineScopeProvider: CoroutineScopeProvider
    ): MyPost2Repository =
        MyPost2Repository(
            applicationContext,
            savedArticleManager,
            coroutineScopeProvider.sync
        )

    @Singleton
    @Provides
    fun provideSavedArticleManager(saveProvider: SaveProviderImpl): SavedArticleManager = SavedArticleManager.getInstance(SavedArticleManager.Params(saveProvider))
}
