package com.washingtonpost.android.config.domain.models.config

import com.wapo.kmpshared.core.config.AppConfig as KMPConfig
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub
import com.washingtonpost.android.config.domain.models.config.paywallconf.PaywallConf
import java.util.regex.Pattern

data class Config(
    val version: Int,
    val comicsConfigStub: ComicsConfigStub,
    private val shareBaseURL: String,
    val fusionBaseUrl: String,
    val singleNativeContentTemplate: String,
    private val singleNativeContentTemplateByUUID: String,
    private val akamaiImageServiceTemplate: String?,
    private val akamaiImageServiceTemplateAuto: String?,
    val wpVideosConfig: WPVideosConfig,
    val listenContentUrl: String,
    val gamesContentUrl: String,
    val mostReadFeedURL: String,
    val rateAppPromptFrequency: Long,
    val search2Config: Search2Config,
    val summariesConfig: SummariesConfig,
    val versionConfig: VersionConfig,
    val adsConfig: AdsConfig,
    val preferencesApiConfig: PreferencesApiConfig,
    val saveConfig: SaveConfig,
    val purchasedArticleConfig: PurchasedArticleConfig,
    val backendHealthConfig: BackendHealthConfig,
    val siteServiceConfig: SiteServiceConfigStub,
    val printConfig: PrintConfigStub,
    val airshipPushConfig: PushConfigStub,
    val paywallConfig: ServiceConfigStub,
    val loggerConfig: LoggerConfig,
    val isSplunkLoggingActive: Boolean,
    val isSplunkSamplingActive: Boolean,
    val splunkSamplingRate: Double,
    val splunkSamplingSegment: Int,
    val liveBlogServiceURL: String,
    val newslettersAndEmailAlertsUrl: String,
    val privacyPolicyUrl: String,
    val noticeOfCollectionUrl: String,
    val termsOfServiceUrl: String,
    val widgetSections: List<WidgetSection>,
    val audioConfig: AudioConfig,
    val savedArticleLogFrequencyDays: Int,
    val contentUpdateRulesConfig: ContentUpdateRulesConfig,
    val paramsToOmit: List<String>,
    val maxGAUploadEventsBatchSize: Int,
    val webArticleConfig: WebArticlesConfig,
    val chartbeatConfig: ChartbeatConfig,
    val followConfig: FollowConfig,
    val voterGuideUrl: String,
    val zendeskConfig: ZendeskConfig,
    val oneTrustConfig: OneTrustConfig,
    val forYouFlexConfig: ForYouFlexConfig,
    val forYouWidgetConfig: ForYouWidgetConfig,
    val userHistoryServiceConfig: UserHistoryServiceConfig,
    val readingHistoryServiceConfig: ReadingHistoryServiceConfig,
    val myPostConfig: MyPostConfig,
    val articleContentUpdateRulesConfig: ArticleContentUpdateRulesConfig,
    val paywallConf: PaywallConf,
    val inlineAlertToggleMappingConfigs: List<InlineAlertToggleMappingConfig>,
    val deepLinkConfig: DeepLinkConfig,
    val airshipConfig: AirshipConfig,
    val verticalVideosConfig: VerticalVideosConfig,
    val actionButtonsConfig: ActionButtonsConfig,
    val amazonMigrationConfig: AmazonMigrationConfig,
    val customNavConfig: CustomNavConfig,
    val recipesConfig: RecipesConfig,
    val electionConfig: ElectionConfig,
    val customURLs: List<String>,
    val featureOnboardingConfigs: List<FeatureOnboardingConfig>,
    val findHighlightItemsConfig: List<FindHighlightItemConfig>,
    val lowDataModeConfig: LowDataModeConfig,
    val commentsConfig: CommentsConfig,
    val privacyConsentConfig: PrivacyConsentConfig,
    val iterableConfig: IterableConfig,
    val personalizedPodcastConfig: PersonalizedPodcastConfig,
    val proxyApiConfig: ProxyApiConfig,
    val nextVideoConfig: NextVideoConfig,
    val feedbackConfig: FeedbackConfig,
    val ageRestriction: AgeRestrictionConfig,
    val videosConfig: VideosConfig,
    val oneLinkGenerationConfig: OneLinkGenerationConfig,
    val kmpConfig: KMPConfig?,
    val autoRecircConfig: AutoRecircConfig,
    val webviewBaseUrlOverride: String? = null,
    val gamesPaths: List<String>,
    val disclaimerBaseUrl: String
) {

    fun getShareUrl(url: String?): String {
        val pattern = Pattern.compile("https?://.*", Pattern.CASE_INSENSITIVE)
        if (!url.isNullOrEmpty() && pattern.matcher(url).matches()) {
            return url
        }

        val sb = StringBuilder()
        sb.append(shareBaseURL)
        if (url != null && !url.startsWith("/") && !shareBaseURL.endsWith("/")) {
            sb.append("/")
        }
        sb.append(url)
        return sb.toString()
    }

    fun getFusionUrl(page: String): String {
        return fusionBaseUrl.replace("{{{page}}}", page)
    }

    fun getLowDataModeUrl(): String {
        return fusionBaseUrl.replace("{{{page}}}", lowDataModeConfig.liteUrlPath)
    }

    fun getUrlTemplate(UUIDBased: Boolean): String {
        return if (UUIDBased) singleNativeContentTemplateByUUID else singleNativeContentTemplate
    }

    fun createSingleArticleUrl(article: String, UUIDBased: Boolean): String {
        return String.format(getUrlTemplate(UUIDBased), article)
    }

    fun createImageRequestUrl(imageSrc: String, imageServiceConfig: ImageServiceConfig): String {
        return if (akamaiImageServiceTemplate != null) {
            String.format(akamaiImageServiceTemplate, imageServiceConfig.imgWidth, imageServiceConfig.imgHeight, imageSrc)
        } else {
            imageSrc
        }
    }

    fun createImageRequestUrlForAuto(imageSrc: String, imageServiceConfig: ImageServiceConfig): String {
        return if (akamaiImageServiceTemplateAuto != null) {
            String.format(akamaiImageServiceTemplateAuto, imageServiceConfig.imgWidth, imageServiceConfig.imgHeight, imageSrc)
        } else {
            imageSrc
        }
    }

    fun getPlayLicense() = paywallConfig.playLicense

    fun isGiftedURL(url: String): Boolean {
        return deepLinkConfig.ungiftedURLs.none { it.toRegex().matches(url) }
    }
}
