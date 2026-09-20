package com.wapo.flagship.util.tracking.states

import android.net.Uri

data class TrafficSource(
    val source: String?,
    val medium: String?,
    val campaign: String?,
) {
    val isEmpty: Boolean
        get() = source.isNullOrEmpty() && medium.isNullOrEmpty() && campaign.isNullOrEmpty()

    companion object {
        private const val UTM_SOURCE = "utm_source"
        private const val UTM_MEDIUM = "utm_medium"
        private const val UTM_CAMPAIGN = "utm_campaign"

        fun fromDeepLinkUri(uri: Uri): TrafficSource {
            if (!uri.isHierarchical) {
                return TrafficSource(
                    source = null,
                    medium = null,
                    campaign = null,
                )
            }
            val utmSource = uri.getQueryParameter(UTM_SOURCE)
            val utmMedium = uri.getQueryParameter(UTM_MEDIUM)
            val utmCampaign = uri.getQueryParameter(UTM_CAMPAIGN)

            return TrafficSource(
                source = utmSource,
                medium = utmMedium,
                campaign = utmCampaign,
            )
        }
    }
}