package com.washingtonpost.android.paywall.util.di

import com.washingtonpost.android.paywall.api.NonceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthEntryPoint {
    fun nonceRepository(): NonceRepository
}