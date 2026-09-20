package com.washingtonpost.android.config.domain.repositories

import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.Source

interface ConfigRepository {
    suspend fun saveConfigOverrides(overrides: List<ConfigOverride>): Boolean
    suspend fun getConfigOverrides(): List<ConfigOverride>?
    suspend fun getConfig(overrides: List<ConfigOverride>, source: Source = Source.DEFAULT): Config
}