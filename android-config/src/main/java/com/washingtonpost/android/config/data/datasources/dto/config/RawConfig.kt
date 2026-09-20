package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.kmpshared.core.config.AppConfig as KMPConfig
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.washingtonpost.android.config.data.datasources.dto.config.paywall.RawServiceConfigStub
import com.washingtonpost.android.config.data.datasources.dto.config.paywallconf.RawPaywallConf
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters.mapAdapter
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters.rawConfigAdapter
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.utils.MapUtils

@JsonClass(generateAdapter = true)
data class RawConfig(
    @Json(name = "version") val version: Int? = null,
    @Json(name = "comicsConfigNew") val comicsConfigStub: RawComicsConfigStub? = null,
    @Json(name = "shareBaseURL") val shareBaseURL: String? = null,
    @Json(name = "fusionBaseUrl") val fusionBaseUrl: String? = null,
    @Json(name = "singleNativeContentRainbowTemplate") val singleNativeContentTemplate: String? = null,
    @Json(name = "singleNativeContentTemplateByUUID") val singleNativeContentTemplateByUUID: String? = null,
    @Json(name = "akamaiImageServiceTemplate") val akamaiImageServiceTemplate: String? = null,
    @Json(name = "akamaiImageServiceTemplateForAuto") val akamaiImageServiceTemplateAuto: String? = null,
    @Json(name = "wpVideosConfig") val wpVideosConfig: RawWPVideosConfig? = null,
    @Json(name = "listenContentUrl") val listenContentUrl: String? = null,
    @Json(name = "gamesContentUrl") val gamesContentUrl: String? = null,
    @Json(name = "mostReadFeedURL") val mostReadFeedURL: String? = null,
    @Json(name = "rateAppPromptFrequency") val rateAppPromptFrequency: Long? = null,
    @Json(name = "search2Config") val search2Config: RawSearch2Config? = null,
    @Json(name = "summariesConfig") val summariesConfig: RawSummariesConfig? = null,
    @Json(name = "versionConfig") val versionConfig: RawVersionConfig? = null,
    @Json(name = "ads") val adsConfig: RawAdsConfig? = null,
    @Json(name = "preferencesApiConfig") val preferencesApiConfig: RawPreferencesApiConfig? = null,
    @Json(name = "saveConfig") val saveConfig: RawSaveConfig? = null,
    @Json(name = "purchasedArticleConfig") val purchasedArticleConfig: RawPurchasedArticleConfig? = null,
    @Json(name = "backendHealthConfig") val backendHealthConfig: RawBackendHealthConfig? = null,
    @Json(name = "siteServiceConfig") val siteServiceConfig: RawSiteServiceConfigStub? = null,
    @Json(name = "printConfig") val printConfig: RawPrintConfigStub? = null,
    @Json(name = "pushConfigV3") val airshipPushConfig: RawPushConfigStub? = null,
    @Json(name = "paywallConfigV3") val paywallConfig: RawServiceConfigStub? = null,
    @Json(name = "loggerConfig") val loggerConfig: RawLoggerConfig? = null,
    @Json(name = "isSplunkLoggingActive") val isSplunkLoggingActive: Boolean? = null,
    @Json(name = "isSplunkSamplingActive") val isSplunkSamplingActive: Boolean? = null,
    @Json(name = "splunkSamplingPercentageRate") val splunkSamplingRate: Double? = null,
    @Json(name = "liveBlogServiceURL") val liveBlogServiceURL: String? = null,
    @Json(name = "newslettersAndEmailAlertsUrl") val newslettersAndEmailAlertsUrl: String? = null,
    @Json(name = "privacyPolicyUrl") val privacyPolicyUrl: String? = null,
    @Json(name = "noticeOfCollectionUrl") val noticeOfCollectionUrl: String? = null,
    @Json(name = "termsOfServiceUrl") val termsOfServiceUrl: String? = null,
    @Json(name = "widgetSections") val widgetSections: List<RawWidgetSection>? = null,
    @Json(name = "audioConfig") val audioConfig: RawAudioConfig? = null,
    @Json(name = "savedArticleLogFrequencyDays") val savedArticleLogFrequencyDays: Int? = null,
    @Json(name = "contentUpdateRules") val contentUpdateRulesConfig: RawContentUpdateRulesConfig? = null,
    @Json(name = "paramsToOmit") val paramsToOmit: List<String>? = null,
    @Json(name = "maxGAUploadEventsBatchSize") val maxGAUploadEventsBatchSize: Int? = null,
    @Json(name = "webArticleConfig") val webArticleConfig: RawWebArticlesConfig? = null,
    @Json(name = "chartbeatConfig") val chartbeatConfig: RawChartbeatConfig? = null,
    @Json(name = "followConfig") val followConfig: RawFollowConfig? = null,
    @Json(name = "voterGuideUrl") val voterGuideUrl: String? = null,
    @Json(name = "zendeskConfig") val zendeskConfig: RawZendeskConfig? = null,
    @Json(name = "oneTrustConfig") val oneTrustConfig: RawOneTrustConfig? = null,
    @Json(name = "forYouFlexConfig") val forYouFlexConfig: RawForYouFlexConfig? = null,
    @Json(name = "forYouWidgetConfig") val forYouWidgetConfig: RawForYouWidgetConfig? = null,
    @Json(name = "userHistoryServiceConfig") val userHistoryServiceConfig: RawUserHistoryServiceConfig? = null,
    @Json(name = "myPostConfig") val myPostConfig: RawMyPostConfig? = null,
    @Json(name = "articleContentUpdateRulesConfig") val articleContentUpdateRulesConfig: RawArticleContentUpdateRulesConfig? = null,
    @Json(name = "paywallConf") val paywallConf: RawPaywallConf? = null,
    @Json(name = "inlineAlertToggleMappingConfigs") val inlineAlertToggleMappingConfigs: List<RawInlineAlertToggleMappingConfig>? = null,
    @Json(name = "deepLinkConfig") val deepLinkConfig: RawDeepLinkConfig? = null,
    @Json(name = "airshipConfig") val airshipConfig: RawAirshipConfig? = null,
    @Json(name = "verticalVideosConfig") val verticalVideosConfig: RawVerticalVideosConfig? = null,
    @Json(name = "actionButtonsConfig") val actionButtonsConfig: RawActionButtonsConfig? = null,
    @Json(name = "amazonMigrationConfig") val amazonMigrationConfig: RawAmazonMigrationConfig? = null,
    @Json(name = "customNavConfig") val customNavConfig: RawCustomNavConfig? = null,
    @Json(name = "recipesConfig") val recipesConfig: RawRecipesConfig? = null,
    @Json(name = "electionConfig") val electionConfig: RawElectionConfig? = null,
    @Json(name = "customURLs") val customURLs: List<String>? = null,
    @Json(name = "featureOnboarding") val featureOnboardingConfigs: List<RawFeatureOnboardingConfig>? = null,
    @Json(name = "findHighlightItemsConfig") val findHighlightItemsConfig: List<RawFindHighlightItemConfig>? = null,
    @Json(name = "lowDataModeConfig") val lowDataModeConfig: RawLowDataModeConfig? = null,
    @Json(name = "commentsConfig") val commentsConfig: RawCommentsConfig? = null,
    @Json(name = "privacyConfig") val privacyConsentConfig: RawPrivacyConsentConfig? = null,
    @Json(name = "iterable") val iterableConfig: RawIterableConfig? = null,
    @Json(name = "readingHistoryServiceConfig") val readingHistoryServiceConfig: RawReadingHistoryServiceConfig? = null,
    @Json(name = "personalizedPodcastConfig") val personalizedPodcastConfig: RawPersonalizedPodcastConfig? = null,
    @Json(name = "nextVideoConfig") val nextVideoConfig: RawNextVideoConfig? = null,
    @Json(name = "proxyApiConfig") val proxyApiConfig: RawProxyApiConfig? = null,
    @Json(name = "feedbackConfig") val feedbackConfig: RawFeedbackConfig? = null,
    @Json(name = "ageRestriction") val ageRestriction: RawAgeRestrictionConfig? = null,
    @Json(name = "videos") val videosConfig: RawVideosConfig? = null,
    @Json(name = "oneLinkGenerationConfig") val oneLinkGenerationConfig: RawOneLinkGenerationConfig? = null,
    @Json(name = "kmp") val kmpConfig: Map<String, Any?>? = null,
    @Json(name = "autoRecircConfig") val autoRecircConfig: RawAutoRecircConfig? = null,
    @Json(name = "webviewBaseUrlOverride") val webviewBaseUrlOverride: String? = null,
    @Json(name = "gamesPaths") val gamesPaths: List<String>? = null,
    @Json(name = "disclaimerBaseUrl") val disclaimerBaseUrl: String? = null,
) {
    fun override(overrides: List<Map<String, Any?>>): RawConfig {
        if (overrides.isEmpty()) return this
        val srcJson = rawConfigAdapter.toJson(this)
        val srcMap = mapAdapter.fromJson(srcJson).orEmpty()
        val merged = MapUtils.mergeMaps(srcMap, overrides)
        val mergedJson = mapAdapter.toJson(merged)
        return rawConfigAdapter.fromJson(mergedJson) ?: this
    }

    fun mapToDomain(params: MapConfigParams): Config {
        val splunkSamplingSegment = params.deviceUniqueId.let {
            (if(it.hashCode() < 0) it.hashCode() * -1 else it.hashCode()) % 999
        }

        val configVersion = version ?: -1

        return Config(
            version = configVersion,
            comicsConfigStub = (comicsConfigStub ?: RawComicsConfigStub()).mapToDomain(),
            shareBaseURL = shareBaseURL ?: "http://www.washingtonpost.com",
            fusionBaseUrl = (fusionBaseUrl
                ?: "https://jsonapp1.washingtonpost.com/fusion_prod/v2/{{{page}}}")
                .replace("%s", "fusion_prod"),
            singleNativeContentTemplate = (
                    singleNativeContentTemplate
                        ?: "https://rainbowapi-a.wpdigital.net/rainbow-data-service/rainbow/content-by-url.json?followLinks=false&platform=iphoneclassic&url=%s"
                    ).replace("%@", "%s"),
            singleNativeContentTemplateByUUID = singleNativeContentTemplateByUUID
                ?.replace("%@", "%s")
                ?: "http://tabletapi.washingtonpost.com/apps-data-service/content-by-identifier.json?identifier=%s",
            akamaiImageServiceTemplate = akamaiImageServiceTemplate,
            akamaiImageServiceTemplateAuto = akamaiImageServiceTemplateAuto,
            wpVideosConfig = (wpVideosConfig ?: RawWPVideosConfig()).mapToDomain(),
            listenContentUrl = listenContentUrl
                ?: "https://jsonapp1.washingtonpost.com/fusion_prod/v2/tablet/listen-to-the-post",
            gamesContentUrl = gamesContentUrl
                ?: "https://jsonapp1.washingtonpost.com/fusion_prod/v2/games",
            mostReadFeedURL = mostReadFeedURL
                ?: "https://www.washingtonpost.com/arcio/most-read/?size=10&website=washpost",
            rateAppPromptFrequency = rateAppPromptFrequency
                ?: 1209600000,   // rateAppPromptFrequency from config or default to 14 days
            search2Config = (search2Config ?: RawSearch2Config()).mapToDomain(),
            summariesConfig = (summariesConfig ?: RawSummariesConfig()).mapToDomain(),
            versionConfig = (versionConfig ?: RawVersionConfig()).mapToDomain(params),
            adsConfig = (adsConfig ?: RawAdsConfig()).mapToDomain(params),
            preferencesApiConfig = (preferencesApiConfig
                ?: RawPreferencesApiConfig()).mapToDomain(),
            saveConfig = (saveConfig ?: RawSaveConfig()).mapToDomain(),
            purchasedArticleConfig = (purchasedArticleConfig ?: RawPurchasedArticleConfig()).mapToDomain(),
            backendHealthConfig = (backendHealthConfig ?: RawBackendHealthConfig()).mapToDomain(),
            siteServiceConfig = (siteServiceConfig ?: RawSiteServiceConfigStub()).mapToDomain(),
            printConfig = (printConfig ?: RawPrintConfigStub()).mapToDomain(params),
            airshipPushConfig = (
                    airshipPushConfig
                        ?: params.getDefaultConfigFromResource<RawConfig>(
                            DEFAULT_PUSH_CONFIG_STUB_FILENAME
                        )?.airshipPushConfig
                        ?: RawPushConfigStub()
                    ).mapToDomain(params),
            paywallConfig = (
                    paywallConfig
                        ?: params.getDefaultConfigFromResource<RawConfig>(
                            DEFAULT_PAYWALL_CONFIG_FILENAME
                        )?.paywallConfig
                        ?: RawServiceConfigStub()
                    ).mapToDomain(params),
            loggerConfig = (loggerConfig ?: RawLoggerConfig()).mapToDomain(params),
            isSplunkLoggingActive = isSplunkLoggingActive ?: true,
            isSplunkSamplingActive = isSplunkSamplingActive ?: false,
            splunkSamplingRate = splunkSamplingRate ?: 10.0,
            splunkSamplingSegment = splunkSamplingSegment,
            liveBlogServiceURL = liveBlogServiceURL
                ?: "https://www.washingtonpost.com/arcio/fact-checker/item/?select=cms_date,cms_title,cms_modified,transformed_content.slug&limit=LIVE_BLOG_MAX_ENTRIES&src_url=",
            newslettersAndEmailAlertsUrl = "https://www.washingtonpost.com/newsletters/",
            privacyPolicyUrl = "https://www.washingtonpost.com/privacy-policy/2011/11/18/gIQASIiaiN_story.html",
            noticeOfCollectionUrl = "https://www.washingtonpost.com/privacy-policy/2011/11/18/gIQASIiaiN_story.html#CALIFORNIA",
            termsOfServiceUrl = "https://www.washingtonpost.com/terms-of-service/2011/11/18/gIQAldiYiN_story.html",
            widgetSections = widgetSections?.map { it.mapToDomain() }.orEmpty(),
            audioConfig = (audioConfig ?: RawAudioConfig()).mapToDomain(),
            savedArticleLogFrequencyDays = savedArticleLogFrequencyDays ?: 14,
            contentUpdateRulesConfig = (contentUpdateRulesConfig ?: RawContentUpdateRulesConfig())
                .mapToDomain(),
            paramsToOmit = paramsToOmit.orEmpty(),
            maxGAUploadEventsBatchSize = maxGAUploadEventsBatchSize ?: 10,
            webArticleConfig = (webArticleConfig ?: RawWebArticlesConfig()).mapToDomain(),
            chartbeatConfig = (chartbeatConfig ?: RawChartbeatConfig()).mapToDomain(),
            followConfig = (followConfig ?: RawFollowConfig()).mapToDomain(),
            voterGuideUrl = voterGuideUrl
                ?: "https://elex-page-data-prod.elections.aws.wapo.pub/elex-components-data/voting-guide-50-states.json",
            zendeskConfig = (zendeskConfig ?: RawZendeskConfig()).mapToDomain(params),
            oneTrustConfig = (oneTrustConfig ?: RawOneTrustConfig()).mapToDomain(params),
            forYouFlexConfig = (forYouFlexConfig ?: RawForYouFlexConfig()).mapToDomain(),
            forYouWidgetConfig = (forYouWidgetConfig ?: RawForYouWidgetConfig()).mapToDomain(),
            userHistoryServiceConfig = (userHistoryServiceConfig ?: RawUserHistoryServiceConfig())
                .mapToDomain(),
            myPostConfig = (myPostConfig ?: RawMyPostConfig()).mapToDomain(),
            articleContentUpdateRulesConfig = (
                    articleContentUpdateRulesConfig ?: RawArticleContentUpdateRulesConfig()
                    ).mapToDomain(),
            paywallConf = (paywallConf
                ?: params.getDefaultConfigFromResource<RawConfig>(
                    DEFAULT_PAYWALL_CONF
                )?.paywallConf
                ?: RawPaywallConf()).mapToDomain(),
            inlineAlertToggleMappingConfigs = inlineAlertToggleMappingConfigs
                ?.map { it.mapToDomain() }
                .orEmpty(),
            deepLinkConfig = (deepLinkConfig ?: RawDeepLinkConfig()).mapToDomain(),
            airshipConfig = (airshipConfig ?: RawAirshipConfig()).mapToDomain(),
            verticalVideosConfig = (verticalVideosConfig ?: RawVerticalVideosConfig())
                .mapToDomain(),
            actionButtonsConfig = (actionButtonsConfig ?: RawActionButtonsConfig()).mapToDomain(),
            amazonMigrationConfig = (amazonMigrationConfig ?: RawAmazonMigrationConfig())
                .mapToDomain(),
            customNavConfig = (customNavConfig ?: RawCustomNavConfig()).mapToDomain(),
            recipesConfig = (recipesConfig ?: RawRecipesConfig()).mapToDomain(),
            electionConfig = (electionConfig ?: RawElectionConfig()).mapToDomain(),
            customURLs = customURLs.orEmpty(),
            featureOnboardingConfigs = featureOnboardingConfigs?.map { it.mapToDomain() }.orEmpty(),
            findHighlightItemsConfig = findHighlightItemsConfig?.map { it.mapToDomain() }.orEmpty(),
            lowDataModeConfig = (lowDataModeConfig ?: RawLowDataModeConfig()).mapToDomain(),
            commentsConfig = (commentsConfig ?: RawCommentsConfig()).mapToDomain(),
            privacyConsentConfig = (privacyConsentConfig ?: RawPrivacyConsentConfig())
                .mapToDomain(params),
            iterableConfig = (iterableConfig ?: RawIterableConfig()).mapToDomain(),
            readingHistoryServiceConfig = (readingHistoryServiceConfig ?: RawReadingHistoryServiceConfig()).mapToDomain(),
            personalizedPodcastConfig =  (personalizedPodcastConfig ?: RawPersonalizedPodcastConfig()).mapToDomain(params),
            nextVideoConfig = (nextVideoConfig ?: RawNextVideoConfig()).mapToDomain(),
            proxyApiConfig = (proxyApiConfig ?: RawProxyApiConfig()).mapToDomain(),
            feedbackConfig = (feedbackConfig ?: RawFeedbackConfig()).mapToDomain(),
            ageRestriction = (ageRestriction ?: RawAgeRestrictionConfig()).mapToDomain(),
            videosConfig = (videosConfig ?: RawVideosConfig()).mapToDomain(),
            oneLinkGenerationConfig = (oneLinkGenerationConfig ?: RawOneLinkGenerationConfig()).mapToDomain(),
            kmpConfig = kmpConfig?.let {
                KMPConfig(configVersion, it.withSecureLoggerToken())
            },
            autoRecircConfig = (autoRecircConfig ?: RawAutoRecircConfig()).mapToDomain(),
            webviewBaseUrlOverride = webviewBaseUrlOverride,
            gamesPaths = gamesPaths ?: listOf(),
            disclaimerBaseUrl = disclaimerBaseUrl ?: "https://rainbowdatanet-a.wpdigital.net/native/app-article-disclaimer/"
        )
    }

    private fun Map<String, Any?>.withSecureLoggerToken(): Map<String, Any?> {
        val logger = this["logger"] as? Map<*, *> ?: return this
        val loggerWithToken = logger.entries.associate { (key, value) ->
            key.toString() to value
        } + ("uploaderToken" to WapoSecDataProvider.splunkToken)

        return this + ("logger" to loggerWithToken)
    }

    companion object {
        private const val DEFAULTS_PATH = "config/defaults"
        private const val DEFAULT_PUSH_CONFIG_STUB_FILENAME = "${DEFAULTS_PATH}/default_push_config_stub.json"
        private const val DEFAULT_PAYWALL_CONFIG_FILENAME = "${DEFAULTS_PATH}/default_service_config_stub.json"
        private const val DEFAULT_PAYWALL_CONF = "${DEFAULTS_PATH}/paywall_config_fallback.json"
    }
}
