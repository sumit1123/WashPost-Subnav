package com.wapo.flagship.di.app.modules.features.articles2

import com.wapo.flagship.features.articles2.interfaces.DisclaimerInfoRepo
import com.wapo.flagship.features.articles2.repo.DisclaimerInfoRepoImpl
import com.wapo.flagship.features.articles2.services.DisclaimerInfoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DisclaimerInfoRepositoryModule {

    @Singleton
    @Provides
    fun provideDisclaimerInfoRepository(
        disclaimerInfoService: DisclaimerInfoService
    ): DisclaimerInfoRepo = DisclaimerInfoRepoImpl(disclaimerInfoService)
}