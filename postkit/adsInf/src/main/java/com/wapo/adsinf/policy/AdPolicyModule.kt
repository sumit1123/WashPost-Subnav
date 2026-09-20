package com.wapo.adsinf.policy

import android.content.Context
import com.washingtonpost.android.paywall.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdPolicyModule {
    
    @Provides
    @Singleton
    fun provideAdsPolicyRepository(@ApplicationContext context: Context): AdsPolicyRepository = AdsPolicyRepository(
        subAttributeAdFreeKey = context.getString(R.string.sub_attribute_ad_free),
        subAttributeAdFreeEuKey = context.getString(R.string.sub_attribute_ad_free_eu)
    )

    @Provides
    @Singleton
    fun provideAdService(repository: AdsPolicyRepository): AdService = AdService(repository)
}
