package com.wapo.flagship.sdk.iterable.models

import com.washingtonpost.android.config.domain.models.config.IterableConfig

enum class IamMessageType(val type: String) {
    BANNER("banner"),
    ARTICLE("article"),
    SECTION("section"),
    MY_POST_BANNER("myPostBanner"),
    ASK_THE_POST_BANNER("askThePostBanner"),
    MY_POST("myPost"),
    ASK_THE_POST("askThePost"),
    SETTINGS_PLAN("settingsPlan"),
    SETTINGS_TOP("settingsTop"),
    FRONT_HOME_SCROLL("frontHomeScroll"),
    REGULAR_IN_APP_MESSAGE("iam");

    fun embeddedPlacementId(config: IterableConfig?): Long? {
        val iamAndEmbeddedPlacementsIdsMap = config?.run {
            mapOf(
                BANNER to banner,
                ARTICLE to article,
                SECTION to section,
                MY_POST_BANNER to myPostBanner,
                ASK_THE_POST_BANNER to askThePostBanner,
                MY_POST to myPost,
                ASK_THE_POST to askThePost,
                SETTINGS_PLAN to settingsPlan,
                SETTINGS_TOP to settingsTop,
                FRONT_HOME_SCROLL to frontHomeScroll,
                REGULAR_IN_APP_MESSAGE to -1L
            )
        } ?: emptyMap()

        return iamAndEmbeddedPlacementsIdsMap[this]
    }

    companion object {
        fun fromType(type: String): IamMessageType? = entries.find { it.type == type }
    }
}
