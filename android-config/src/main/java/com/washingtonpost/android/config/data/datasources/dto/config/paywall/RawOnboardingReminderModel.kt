package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.android.commons.util.Utils
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.paywall.OnboardingReminderModel
import com.washingtonpost.android.config.domain.models.config.paywall.SubscriberDataModel

@JsonClass(generateAdapter = true)
data class RawOnboardingReminderModel(
    @Json(name = "newSubscriber") val newSubscriber: RawSubscriberDataModel? = null,
    @Json(name = "terminatedSubscriber") val terminatedSubscriber: RawSubscriberDataModel? = null,
    @Json(name = "productId") val productId: String? = null,
    @Json(name = "ctaDestination") val ctaDestination: String? = null,
) {
    fun mapToDomain(params: MapConfigParams): OnboardingReminderModel {
        val defaultProductId = if(params.configProvider.storeType == StoreType.AMAZON) {
            "wp.unified.basic"
        } else {
            "wp.classic.basic"
        }
        return OnboardingReminderModel(
            newSubscriber = (newSubscriber ?: RawSubscriberDataModel(
                "Plus, you will be able to save stories to read later, and read offline.",
                "All our journalism, free for 30 days."
            )).mapToDomain(),
            terminatedSubscriber = (terminatedSubscriber ?: RawSubscriberDataModel(
                "Resubscribe and get unlimited access to trusted journalism, save stories to read later, and read offline.",
                "Come back and get the full experience."
            )).mapToDomain(),
            productId = productId ?: defaultProductId,
            ctaDestination = ctaDestination ?: "paywall",
        )
    }
}