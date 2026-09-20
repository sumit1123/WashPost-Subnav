package com.wapo.flagship.util.tracking.states

enum class AcquisitionEntranceType(
    val value: String,
) {
    GIFT_PAYWALL("apps_paywall_gift"),
    SAVE_REGWALL("apps_regwall_save_content"),
    SAVE_RECIPE_REGWALL("apps_regwall_save_recipe"),
    AUDIO_CAROUSEL_REGWALL("apps_regwall_audio_carousel"),
    PAUSE_WALL("apps_wall_pause"),
    NONE("none"),
    UNKNOWN("unknown")
}

enum class PaywallAnalyticsEntryPrefix(val value: String) {
    PAYWALL("apps_paywall"),
    REGWALL("apps_regwall"),
    REGWALL_SOFT("apps_regwall_soft")
}

enum class PaywallAnalyticsEntry(val value: String) {
    MAIN("main"),
    REGWALL("regwall"),
}

/**
 * Builds acquisition entrance type strings from Iterable banner config.
 *
 * Format: apps_<bannerPlacement>_<bannerCampaign>
 *
 * Examples:
 * - apps_banner_summer_sale
 * - apps_section_newsletter_promo
 * - apps_askThePost_welcome_offer
 */
object AcquisitionEntranceTypeBuilder {
    private const val PREFIX = "apps"

    /**
     * Constructs an acquisition entrance type string from banner placement and campaign.
     *
     * @param bannerPlacement The placement type (e.g., "banner", "askThePost")
     * @param campaignName The campaign name from Iterable JSON tracking object
     * @param campaignId The numerical identifier as a fallback (from AttributionInfo)
     * @param isLocalConfig True if the banner was loaded from the config file instead of Iterable
     * @return Formatted string: apps_<bannerPlacement>_<bannerCampaign>
     */
    @JvmStatic
    fun build(
        bannerPlacement: String,
        campaignName: String?,
        campaignId: Int?,
        isLocalConfig: Boolean = false
    ): String {

        val resolvedCampaign = when {
            isLocalConfig -> "config"
            !campaignName.isNullOrBlank() -> campaignName
            campaignId != null -> campaignId.toString()
            else -> "unknown"
        }

        val normalizedCampaign = resolvedCampaign.lowercase().replace("-", "_").replace(" ", "_")
        return "${PREFIX}_${bannerPlacement}_${normalizedCampaign}"
    }
}
