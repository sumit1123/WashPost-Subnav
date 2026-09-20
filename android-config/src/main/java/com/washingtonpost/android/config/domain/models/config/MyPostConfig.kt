package com.washingtonpost.android.config.domain.models.config

data class MyPostConfig(
    val banner: MyPostBannerConfig?,
    val birthdayFrontPageConfig: BirthdayFrontPageConfig,
)

data class MyPostBannerConfig(
    val enabled: Boolean,
    val sections: List<String>,
    val criteria: List<String>,
    val link: String,
    val title: String,
    val subtitle: String,
    val image: MyPostBannerImageConfig,
    val cta: MyPostBannerCtaConfig,
)

data class MyPostBannerImageConfig(
    val asset: String,
    val url: String,
    val urlDark: String,
    val assetHasPriority: Boolean,
)

data class MyPostBannerCtaConfig(
    val minimizeOnNarrowScreen: Boolean,
    val buttonText: String,
)

data class BirthdayFrontPageConfig(
    val enabled: Boolean,
    val birthdayFrontPageUrl: String,
)
