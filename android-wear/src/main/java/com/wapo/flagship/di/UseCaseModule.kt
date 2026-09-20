/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.articles2.use_cases.Articles2UseCases
import com.wapo.flagship.features.articles2.use_cases.GetArticles2
import com.wapo.flagship.features.section.repo.SectionRepository
import com.wapo.flagship.features.section.use_cases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @ViewModelScoped
    @Provides
    fun provideSectionUseCases(repository: SectionRepository): SectionUseCases {
        return SectionUseCases(
            getFusionSection = GetFusionSection(repository),
            getPageBuilderSection = GetPageBuilderSection(repository)
        )
    }

    @ViewModelScoped
    @Provides
    fun provideArticles2UseCases(repository: Articles2Repository): Articles2UseCases {
        return Articles2UseCases(
            getArticles2 = GetArticles2(repository)
        )
    }

}