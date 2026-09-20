package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawServiceConfigStub(
    @Json(name = "tetroSyncFrequency") val tetroSyncFrequency: Int? = null,
    @Json(name = "on") val pwTurnedOn: Boolean? = null,
    @Json(name = "tetroOn") val tetroTurnedOn: Boolean? = null,
    @Json(name = "tetroBaseUrl") val tetroBaseUrl: String? = null,
    @Json(name = "meteringProxyBaseUrl") val meteringProxyBaseUrl: String? = null,
    @Json(name = "useMeteringProxy") val useMeteringProxy: Boolean? = null,
    @Json(name = "tetroWeightArticleTTL") val tetroWeightArticleTTL: Long? = null,
    @Json(name = "limit") val limit: Int? = null,
    @Json(name = "ftcDialogPlayStoreVisibility") val ftcDialogPlayStoreVisibility: Boolean? = null,
    @Json(name = "ftcDialogAmazonVisibility") val ftcDialogAmazonVisibility: Boolean? = null,
    @Json(name = "freeThresholdSec") val thresholdSec: Int? = null,
    @Json(name = "validSKUs") val validSkuList: Set<String>? = null,
    @Json(name = "validSixMonthSKUs") val validSixMonthsSkuList: Set<String>? = null,
    @Json(name = "adFreeSKUs") val adFreeSKUs: Set<String>? = null,
    @Json(name = "baseSubscriptionProducts") val baseSubscriptionProducts: List<String>? = null,
    @Json(name = "SKU") val sku: String? = null,
    @Json(name = "samsungSku") val samsungSku: String? = null,
    @Json(name = "playStoreSku") val playStoreSku: String? = null,
    @Json(name = "amazonSku") val amazonSku: String? = null,
    @Json(name = "playLicense") val playLicense: String? = null,
    @Json(name = "editEmailPasswordUrl") val editEmailPasswordUrl: String? = null,
    @Json(name = "editNamePhotoUrl") val editNamePhotoUrl: String? = null,
    @Json(name = "manageSubUrl") val manageSubUrl: String? = null,
    @Json(name = "manageSubResumeUrl") val manageSubResumeUrl: String? = null,
    @Json(name = "paywallBaseUrl") val paywallBaseURL: String? = null,
    @Json(name = "subsBaseUrl") val subsBaseUrl: String? = null,
    @Json(name = "newsLettersBaseUrl") val newsLettersBaseUrl: String? = null,
    @Json(name = "subBenefitsUrl") val subBenefitsUrl: String? = null,
    @Json(name = "aboutMeUrl") val aboutMeUrl: String? = null,
    @Json(name = "oAuthConfigStub") val oAuthConfigStub: RawOAuthConfigStub? = null,
    @Json(name = "reminderScreen") val reminderScreenConfig: RawReminderScreenConfig? = null,
    @Json(name = "bottomCta") val bottomCta: RawBottomCtaModel? = null,
    @Json(name = "bottomCtaAmazon") val bottomCtaAmazon: RawBottomCtaModel? = null,
    @Json(name = "onboardingReminder") val onboardingReminder: RawOnboardingReminderModel? = null,
    @Json(name = "acquisitionReminder") val acquisitionReminder: RawAcquisitionReminderModel? = null,
    @Json(name = "acquisitionReminderAmazon") val acquisitionReminderAmazon: RawAcquisitionReminderModel? = null,
    @Json(name = "paywallSheets") val paywallSheets: RawPaywallSheetModels? = null,
    @Json(name = "cookies") val cookieConfig: RawCookieConfig? = null,
    @Json(name = "globalBannerConfig") val globalBannerConfig: RawGlobalBannerConfig? = null,
) {
    fun mapToDomain(params: MapConfigParams): ServiceConfigStub {
        return ServiceConfigStub(
            tetroSyncFrequency = tetroSyncFrequency ?: 3,
            pwTurnedOn = pwTurnedOn ?: false,
            tetroTurnedOn = tetroTurnedOn ?: false,
            tetroBaseUrl = tetroBaseUrl ?: "https://www.washingtonpost.com/tetro/metering/",
            meteringProxyBaseUrl = meteringProxyBaseUrl
                ?: "https://subscribe.washingtonpost.com/nativeservice/proxy/metering/apps/",
            useMeteringProxy = useMeteringProxy ?: false,
            tetroWeightArticleTTL = tetroWeightArticleTTL ?: 86400000,
            limit = limit ?: 0,
            ftcDialogVisibility = when (params.configProvider.storeType) {
                StoreType.AMAZON -> ftcDialogAmazonVisibility ?: false
                else -> ftcDialogPlayStoreVisibility ?: false
            },
            thresholdSec = thresholdSec ?: 5,
            validSkuList = validSkuList.orEmpty().ifEmpty { setOf("monthly_all_access") },
            validSixMonthsSkuList = validSixMonthsSkuList.orEmpty(),
            adFreeSKUs = adFreeSKUs.orEmpty(),
            baseSubscriptionProducts = baseSubscriptionProducts ?: emptyList(),
            sku = when (params.configProvider.storeType) {
                StoreType.AMAZON -> amazonSku ?: sku ?: "wp.unified.basic"
                StoreType.GOOGLE -> playStoreSku ?: sku ?: "monthly_all_access"
                StoreType.SAMSUNG -> samsungSku ?: sku ?: "monthly_all_access"
            },
            playLicense = playLicense ?: "",
            editEmailPasswordUrl = editEmailPasswordUrl
                ?: "https://subscribe.washingtonpost.com/profile/#!/profile/access?destination=https:%2F%2Fwww.washingtonpost.com%2F%3Frefresh%3Dtrue&tid=nav_acctmgnt_menu&itid=app_settings",   //  Used in SettingsViewModel,
            editNamePhotoUrl = editNamePhotoUrl
                ?: "https://subscribe.washingtonpost.com/profile/#!/profile/details?destination=https:%2F%2Fwww.washingtonpost.com%2F%3Frefresh%3Dtrue&itid=app_settings",   //  Used in SettingsViewModel,
            manageSubUrl = manageSubUrl
                ?: "https://www.washingtonpost.com/my-post/account/subscription?itid=app_settings",
            manageSubResumeUrl = manageSubResumeUrl
                ?: "https://www.washingtonpost.com/my-post/account/subscription/resume?itid=app_settings",
            paywallBaseURL = String.format(
                paywallBaseURL ?: "https://subscribe.washingtonpost.com/%s/rest/",
                if (params.configProvider.storeType == StoreType.AMAZON) "amazonservice" else "googleservice"
            ),
            subsBaseUrl = subsBaseUrl ?: "https://subscribe.washingtonpost.com/",
            newsLettersBaseUrl = newsLettersBaseUrl ?: "https://newsletters.washingtonpost.com/",
            subBenefitsUrl = subBenefitsUrl
                ?: "https://www.washingtonpost.com/my-post/my-benefits/",
            aboutMeUrl = aboutMeUrl ?: "https://www.washingtonpost.com/my-post/account/about-me",
            oAuthConfigStub = (oAuthConfigStub ?: RawOAuthConfigStub()).mapToDomain(),
            reminderScreenConfig = (reminderScreenConfig
                ?: RawReminderScreenConfig()).mapToDomain(),
            bottomCtaModel = (when (params.configProvider.storeType) {
                StoreType.AMAZON -> bottomCtaAmazon
                else -> bottomCta
            } ?: RawBottomCtaModel()).mapToDomain(params),
            onboardingReminder = (onboardingReminder
                ?: RawOnboardingReminderModel()).mapToDomain(params),
            acquisitionReminder = (when (params.configProvider.storeType) {
                StoreType.AMAZON -> acquisitionReminderAmazon
                else -> acquisitionReminder
            } ?: RawAcquisitionReminderModel()).mapToDomain(params),
            paywallSheets = (paywallSheets ?: RawPaywallSheetModels()).mapToDomain(),
            cookieConfig = (cookieConfig ?: RawCookieConfig()).mapToDomain(),
            globalBannerConfig = (globalBannerConfig ?: RawGlobalBannerConfig()).mapToDomain(),
        )
    }
}
