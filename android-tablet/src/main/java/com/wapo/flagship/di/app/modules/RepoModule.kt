package com.wapo.flagship.di.app.modules

import android.content.Context
import android.preference.PreferenceManager
import com.wapo.android.commons.data.repository.AppContextUtilsRepoImpl
import com.wapo.android.commons.domain.AppContextUtilsRepo
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.android.data.repository.KMPRemoteLogRepoImpl
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.data.repository.ContentManagerDataRepoImpl
import com.wapo.flagship.data.repository.HealthStatusRepoImpl
import com.wapo.flagship.data.repository.MenuSectionRepoImpl
import com.wapo.flagship.data.repository.NavBarRepoImpl
import com.wapo.flagship.data.repository.OnboardingRepoImpl
import com.wapo.flagship.data.repository.PrefUtilsRepoImpl
import com.wapo.flagship.domain.repository.ContentManagerDataRepo
import com.wapo.flagship.domain.repository.HealthStatusRepo
import com.wapo.flagship.domain.repository.MenuSectionRepo
import com.wapo.flagship.domain.repository.NavBarRepo
import com.wapo.flagship.domain.repository.OnboardingRepo
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.foryou.UserHistoryMetaProviderImpl
import com.wapo.flagship.features.search2.remote.OkHttpSseService
import com.wapo.flagship.features.search2.remote.PostAnswersService
import com.wapo.flagship.features.search2.remote.Search2Service
import com.wapo.flagship.features.search2.remote.SearchRecipeService
import com.wapo.flagship.features.search2.repo.SearchRepoImpl
import com.washingtonpost.userhistory.repo.UserHistoryMetaProvider
import com.wapo.flagship.features.sections.repo.SectionTrackingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RepoModule {

    @Provides
    @Singleton
    fun provideAppContextUtilsRepo(): AppContextUtilsRepo = AppContextUtilsRepoImpl()

    @Provides
    @Singleton
    fun provideNavBarRepo(
        deviceUtilRepo: DeviceUtilRepo,
        prefUtilsRepo: PrefUtilsRepo
    ): NavBarRepo =
        NavBarRepoImpl(deviceUtilRepo, prefUtilsRepo)

    @Provides
    @Singleton
    fun providePrefUtilsRepo(@ApplicationContext context: Context): PrefUtilsRepo =
        PrefUtilsRepoImpl(PreferenceManager.getDefaultSharedPreferences(context))

    @Provides
    @Singleton
    fun provideContentManagerDataRepo(contentManager: ContentManager): ContentManagerDataRepo =
        ContentManagerDataRepoImpl(contentManager)

    @Provides
    @Singleton
    fun provideMenuSectionRepo(contentManagerDataRepo: ContentManagerDataRepo): MenuSectionRepo =
        MenuSectionRepoImpl(contentManagerDataRepo)

    @Provides
    @Singleton
    fun provideSearchRepo(
        @ApplicationContext context: Context,
        search2Service: Search2Service,
        searchRecipeService: SearchRecipeService,
        postAnswersService: PostAnswersService,
        okHttpSseService: OkHttpSseService,
        remoteLogRepo: RemoteLogRepo
    ): SearchRepo =
        SearchRepoImpl(
            context,
            search2Service,
            searchRecipeService,
            postAnswersService,
            okHttpSseService,
            remoteLogRepo
        )

    @Provides
    @Singleton
    fun provideRemoteLogRepo(kmpRepo: KMPRemoteLogRepoImpl): RemoteLogRepo = kmpRepo

    @Provides
    @Singleton
    fun provideUserHistoryMetaProvider(
        deviceUtilRepo: DeviceUtilRepo,
        utilsRepo: UtilsRepo
    ): UserHistoryMetaProvider =
        UserHistoryMetaProviderImpl(deviceUtilRepo, utilsRepo)

    @Provides
    @Singleton
    fun provideOnboardingRepo(
        @ApplicationContext context: Context,
        prefUtilsRepo: PrefUtilsRepo
    ): OnboardingRepo =
        OnboardingRepoImpl(context, prefUtilsRepo)

    @Provides
    @Singleton
    fun provideHealthStatusRepoProvider(
        remoteLogRepo: RemoteLogRepo,
    ): HealthStatusRepo =
        HealthStatusRepoImpl(remoteLogRepo)

    @Provides
    @Singleton
    fun provideSectionTrackingRepository(): SectionTrackingRepository =
        SectionTrackingRepository()
}
