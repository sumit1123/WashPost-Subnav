package com.wapo.kmpshared.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class ScopeModule {
    @Single
    @Named(Qualifiers.APPLICATION_SCOPE)
    fun provideExternalScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
