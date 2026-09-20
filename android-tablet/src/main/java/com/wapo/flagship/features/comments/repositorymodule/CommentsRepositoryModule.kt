package com.wapo.flagship.features.comments.repositorymodule

import com.wapo.flagship.features.comments.repo.CommentsRepository
import com.wapo.flagship.features.comments.service.CommentsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CommentsRepositoryModule {
    @Singleton
    @Provides
    fun provideCommentCountRepository(commentsService: CommentsService): CommentsRepository =
        CommentsRepository(commentsService)
}
