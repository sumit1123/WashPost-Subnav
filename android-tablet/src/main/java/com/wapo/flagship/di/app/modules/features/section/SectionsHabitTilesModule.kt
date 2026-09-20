// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.section

import android.content.Context
import android.content.SharedPreferences
import com.wapo.flagship.data.repository.SectionHabitTilesRepositoryImpl
import com.wapo.flagship.features.grid.domain.repository.SectionHabitTilesRepository
import com.wapo.flagship.features.sections.data.SectionRibbonRepoImpl
import com.wapo.flagship.features.sections.data.SectionRibbonRepoImpl.Companion.PREFS_NAME
import com.wapo.flagship.features.sections.domein.SectionRibbonRepo
import com.washingtonpost.foryou.domain.HabitTilesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SectionsHabitTilesModule {


    @Singleton
    @Provides
    fun provideSectionHabitTilesRepository(
        habitTilesRepository: HabitTilesRepository
    ): SectionHabitTilesRepository = SectionHabitTilesRepositoryImpl(habitTilesRepository)

    @Singleton
    @Provides
    fun provideSectionRibbonRepo(
        @ApplicationContext applicationContext: Context,
    ): SectionRibbonRepo = SectionRibbonRepoImpl(
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )
}
