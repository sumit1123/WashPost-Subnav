package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.BirthdayFrontPageConfig
import com.washingtonpost.android.config.domain.models.config.MyPostBannerConfig
import com.washingtonpost.android.config.domain.models.config.MyPostBannerCtaConfig
import com.washingtonpost.android.config.domain.models.config.MyPostBannerImageConfig
import com.washingtonpost.android.config.domain.models.config.MyPostConfig

/**
 * Holds configurable values for My Post.
 */
@JsonClass(generateAdapter = true)
data class RawMyPostConfig(
    @Json(name = "banner") val banner: RawMyPostBannerConfig? = null,
    @Json(name = "birthdayFrontPage") val birthdayFrontPageConfig: RawBirthdayFrontPageConfig? = null,
) {
    fun mapToDomain(): MyPostConfig {
        return MyPostConfig(
            banner = banner?.mapToDomain(),
            birthdayFrontPageConfig = (birthdayFrontPageConfig ?: RawBirthdayFrontPageConfig())
                .mapToDomain(),
        )
    }
}

/**
 * Holds values for configurable banner in My Post.
 */
@JsonClass(generateAdapter = true)
data class RawMyPostBannerConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "sections") val sections: List<String>? = null,
    @Json(name = "criteria") val criteria: List<String>? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "image") val image: RawMyPostBannerImageConfig? = null,
    @Json(name = "cta") val cta: RawMyPostBannerCtaConfig? = null,
) {
    fun mapToDomain(): MyPostBannerConfig? {
        return if (!title.isNullOrBlank() && !link.isNullOrBlank() && image != null) {
            MyPostBannerConfig(
                enabled = enabled ?: false,
                sections = sections ?: listOf("ALL"),
                criteria = criteria ?: emptyList(),
                link = link,
                title = title,
                subtitle = subtitle
                    ?: "",
                image = image.mapToDomain(),
                cta = (cta ?: RawMyPostBannerCtaConfig(
                    minimizeOnNarrowScreen = true,
                    buttonText = ""
                )).mapToDomain(),
            )
        } else null
    }
}

/**
 * Holds values for image in configurable banner in My Post.
 */
@JsonClass(generateAdapter = true)
data class RawMyPostBannerImageConfig(
    @Json(name = "asset") val asset: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "urlDark") val urlDark: String? = null,
    @Json(name = "assetHasPriority") val assetHasPriority: Boolean? = null,
) {
    fun mapToDomain(): MyPostBannerImageConfig {
        return MyPostBannerImageConfig(
            asset = asset.orEmpty(),
            url = url.orEmpty(),
            urlDark = urlDark.orEmpty(),
            assetHasPriority = assetHasPriority ?: false,
        )
    }
}

/**
 * Holds values for CTA in configurable banner in My Post.
 */
@JsonClass(generateAdapter = true)
data class RawMyPostBannerCtaConfig(
    @Json(name = "minimizeOnNarrowScreen") val minimizeOnNarrowScreen: Boolean? = null,
    @Json(name = "buttonText") val buttonText: String? = null,
) {
    fun mapToDomain(): MyPostBannerCtaConfig {
        return MyPostBannerCtaConfig(
            minimizeOnNarrowScreen = minimizeOnNarrowScreen ?: false,
            buttonText = buttonText.orEmpty(),
        )
    }
}

/**
 * Holds configurable values for Birthday Front Page.
 */
@JsonClass(generateAdapter = true)
data class RawBirthdayFrontPageConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "url") val url: String? = null,
) {
    fun mapToDomain(): BirthdayFrontPageConfig {
        return BirthdayFrontPageConfig(
            enabled = enabled ?: false,
            birthdayFrontPageUrl = url ?: "https://washingtonpost.com/my-post/front-page",
        )
    }
}
