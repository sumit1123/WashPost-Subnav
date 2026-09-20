package com.washingtonpost.android.config.domain.models

import com.washingtonpost.android.config.domain.models.Constants.OVERRIDES_PATH

enum class ConfigOverride(
    val filename: String,
    val group: OverrideGroup,
    val isVisible: Boolean = true,
) {
    ADS_CONTEXTUAL_TARGETING_STAGING("ads_contextual_targeting_staging.json", group = OverrideGroup.ADS),
    APS_VIA_GOOGLE("aps_via_google.json", group = OverrideGroup.ADS),
    ADS_NIMBUS_ALWAYS("ads_nimbus_always.json", group = OverrideGroup.ADS),
    ADS_NIMBUS_EVENSPLIT("ads_nimbus_evensplit.json", group = OverrideGroup.ADS),
    APS_VIA_NIMBUS("aps_via_nimbus.json", group = OverrideGroup.ADS),
    ADMOB_VIA_NIMBUS("admob_via_nimbus.json", group = OverrideGroup.ADS),
    ADS_GAM_FIRST_NIMBUS_FALLBACK("ads_gam_first_nimbus_fallback.json", group = OverrideGroup.ADS),

    ADS_NIMBUS_TEST_MODE("ads_nimbus_test_mode.json", group = OverrideGroup.ADS_BY_NIMBUS),


    ARC_BETA("arc_beta.json", group = OverrideGroup.ARC),
    ARTICLE_FEEDS_ARC_DEV("article_feeds_arc_dev.json", group = OverrideGroup.ARC),

    AUDIO_ADS_ALWAYS("ads_audio_always.json", group = OverrideGroup.AUDIO),
    AUDIO_ADS_FREQUENT("ads_audio_frequent.json", group = OverrideGroup.AUDIO),
    AUDIO_ADS_VAST_DISABLED("ads_audio_vast_disabled.json", group = OverrideGroup.AUDIO),
    AUDIO_TRITON_ADS_OD_SPY("triton_test_ads.json", group = OverrideGroup.AUDIO),
    AUDIO_TRITON_ADS_CMOD("triton_prod_ads.json", group = OverrideGroup.AUDIO),

    FAILOVER_STAGE("failover_stage.json", group = OverrideGroup.BACKEND_HEALTH),
    BACKEND_HEALTH_CONTROL("backend_health_control.json", group = OverrideGroup.BACKEND_HEALTH),
    FAILOVER_ALWAYS("failover_always.json", group = OverrideGroup.BACKEND_HEALTH),
    FAILOVER_TEST("failover_test.json", group = OverrideGroup.BACKEND_HEALTH),

    CHARTBEAT_DEV("chartbeat_dev.json", group = OverrideGroup.CHARTBEAT),

    COMICS_STAGING("comics_staging.json", group = OverrideGroup.COMICS),

    COMMENTS_DEV("comments_dev.json", group = OverrideGroup.COMMENTS),

    FEEDS_DEV("feeds_dev.json", group = OverrideGroup.FEEDS),
    FEEDS_STAGE("feeds_stage.json", group = OverrideGroup.FEEDS),

    FOR_YOU_FLEX_STAGING("for_you_flex_staging.json", group = OverrideGroup.FOR_YOU_FLEX),

    FUSION_DEV("fusion_dev.json", group = OverrideGroup.FUSION),

    LOGGER_DEV_PLAYSTORE("logger_dev_playstore.json", group = OverrideGroup.LOGGER, isVisible = false),
    LOGGER_DEV_AMAZON("logger_dev_amazon.json", group = OverrideGroup.LOGGER, isVisible = false),


    PAYWALL_STAGE("paywall_stage.json", group = OverrideGroup.PAYWALL_SUBS),

    PRINT_DEV("print_dev.json", group = OverrideGroup.PRINT_EDITION),


    RIPPLE_STAGE("ripple_stage.json", group = OverrideGroup.RIPPLE),

    SEARCH_STAGING("search_staging.json", group = OverrideGroup.SEARCH),

    // Note: this endpoint only works on Zscaler, so must be tested via an emulator
    SEARCH_STAGING_PRIVATE("search_staging_private.json", group = OverrideGroup.SEARCH),

    SEARCH_BETA("search_beta.json", group = OverrideGroup.SEARCH),

    SITE_MOCK("site_mock.json", group = OverrideGroup.SITE_SERVICE),
    SITE_ARC_SANDBOX("site_arc_sandbox.json", group = OverrideGroup.SITE_SERVICE),

    USER_HISTORY_SERVICE_STAGING("user_history_service_staging.json", group = OverrideGroup.USER_HISTORY_SERVICE),


    ZENDESK_DEV("zendesk_dev.json", group = OverrideGroup.ZENDESK),

    ITERABLE_SANDBOX("iterable_sandbox.json", group = OverrideGroup.ITERABLE),


    WEBVIEW_BETA("webview_beta.json", group = OverrideGroup.WEBVIEW);

    val filePath = "${group.path}/$filename"

    companion object {
        fun fromIdOrNull(id: String): ConfigOverride? =
            entries.firstOrNull { it.name.lowercase() == id.lowercase() }
    }
}

enum class OverrideGroup(
    folderName: String,
) {
    ADS("ads"),
    ADS_BY_NIMBUS("ads_by_nimbus"),
    ARC("arc"),
    BACKEND_HEALTH("backend_health"),
    CHARTBEAT("chartbeat"),
    COMICS("comics"),
    COMMENTS("comments"),
    FEEDS("feeds"),
    FOR_YOU_FLEX("for_you_flex"),
    FUSION("fusion"),
    LIVEBLOG("liveblog"),
    LOGGER("logger"),
    MDS("mds"),
    PAYWALL_SUBS("paywall_subs"),
    PRINT_EDITION("print_edition"),
    RIPPLE("ripple"),
    SEARCH("search"),
    SITE_SERVICE("site_service"),
    USER_HISTORY_SERVICE("user_history_service"),
    ZENDESK("zendesk"),
    ITERABLE("iterable"),
    AUDIO("audio"),
    WEBVIEW("webview");

    val path = "$OVERRIDES_PATH/$folderName"
    val title = folderName
        .trim()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    companion object {
        fun fromIdOrNull(id: String): OverrideGroup? =
            OverrideGroup.entries.firstOrNull { it.name.lowercase() == id.lowercase() }
    }
}

private object Constants {
    const val OVERRIDES_PATH = "config/overrides"
}
