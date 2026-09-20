package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.AmazonMigrationConfig
import com.washingtonpost.android.config.domain.models.config.ClassicOffboardingConfig
import com.washingtonpost.android.config.domain.models.config.UnifiedOnboardingConfig

@JsonClass(generateAdapter = true)
data class RawAmazonMigrationConfig(
    @Json(name = "offboarding") val offboarding: RawClassicOffboardingConfig? = null,
    @Json(name = "onboarding") val onboarding: RawUnifiedOnboardingConfig? = null
) {
    fun mapToDomain(): AmazonMigrationConfig {
        return AmazonMigrationConfig(
            offboarding = offboarding?.mapToDomain(),
            onboarding = onboarding?.mapToDomain()
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawClassicOffboardingConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "tombstone") val tombstone: Boolean? = null
) {
    fun mapToDomain(): ClassicOffboardingConfig {
        return ClassicOffboardingConfig(
            enabled = enabled ?: false,
            tombstone = tombstone ?: false,
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawUnifiedOnboardingConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
) {
    fun mapToDomain(): UnifiedOnboardingConfig {
        return UnifiedOnboardingConfig(
            enabled = enabled ?: false,
        )
    }
}
