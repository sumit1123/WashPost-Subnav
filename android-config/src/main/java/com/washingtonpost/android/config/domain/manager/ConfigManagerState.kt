package com.washingtonpost.android.config.domain.manager

import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.config.Config

data class ConfigManagerState(
    val isUpdatingEnv: Boolean,
    val overrides: List<ConfigOverride>,
    val config: Config,
)