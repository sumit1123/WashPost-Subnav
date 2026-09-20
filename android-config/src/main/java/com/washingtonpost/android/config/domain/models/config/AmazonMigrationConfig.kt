package com.washingtonpost.android.config.domain.models.config

import com.squareup.moshi.JsonClass

data class AmazonMigrationConfig(
    val offboarding: ClassicOffboardingConfig?,
    val onboarding: UnifiedOnboardingConfig?
)

@JsonClass(generateAdapter = true)
data class ClassicOffboardingConfig(
    val enabled: Boolean,
    val tombstone: Boolean
)

@JsonClass(generateAdapter = true)
data class UnifiedOnboardingConfig(
    val enabled: Boolean
)
