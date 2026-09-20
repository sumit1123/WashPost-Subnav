package com.wapo.flagship.di.app.modules

import com.wapo.flagship.features.conversations.di.CommentsStoreProvider
import com.wapo.flagship.features.feedback.domain.FeedbackRepositoryProvider
import com.wapo.flagship.kmp.core.DependencyRegistry
import com.wapo.kmpshared.features.conversations.presentation.CommentsStore
import com.wapo.kmpshared.features.feedback.domain.FeedbackRepository
import com.wapo.kmpshared.util.KMPURL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KMPRepositoryModule {

    @Provides
    @Singleton
    fun bindFeedbackRepositoryProvider(
        kmpRegistry: DependencyRegistry
    ): FeedbackRepositoryProvider =
        object : FeedbackRepositoryProvider {
            override val repository: FeedbackRepository?
                get() = kmpRegistry.feedbackRepository
        }

    @Provides
    @Singleton
    fun bindCommentsStoreProvider(
        kmpRegistry: DependencyRegistry
    ): CommentsStoreProvider =
        object : CommentsStoreProvider {
            override fun makeCommentsStore(storyURL: KMPURL, arcID: String?): CommentsStore? {
            return kmpRegistry.makeCommentsStore(storyURL, arcID)
        }
    }
}
