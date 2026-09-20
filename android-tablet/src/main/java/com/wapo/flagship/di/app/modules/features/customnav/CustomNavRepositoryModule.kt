package com.wapo.flagship.di.app.modules.features.customnav

import android.content.Context
import com.wapo.flagship.features.customnav.CustomNavProviderImpl
import com.washingtonpost.customnav.repo.CustomNavRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object CustomNavRepositoryModule {
    @Singleton
    @Provides
    fun provideCustomNavRepository(@ApplicationContext applicationContext: Context): CustomNavRepository =
        CustomNavRepository(
            context = applicationContext,
            customNavProvider = CustomNavProviderImpl(),
        )
}
