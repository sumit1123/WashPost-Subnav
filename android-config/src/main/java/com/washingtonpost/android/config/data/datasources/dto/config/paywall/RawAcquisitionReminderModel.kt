package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderModel
import com.washingtonpost.android.config.domain.models.config.paywall.SubscriberDataModel
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderCtaDestination

@JsonClass(generateAdapter = true)
data class RawAcquisitionReminderModel(
    @Json(name = "newSubscriber") val newSubscriber: RawSubscriberDataModel? = null,
    @Json(name = "terminatedSubscriber") val terminatedSubscriber: RawSubscriberDataModel? = null,
    @Json(name = "sku") val productId: String? = null,
    @Json(name = "ctaDestination") val ctaDestination: RawAcquisitionReminderCtaDestination? = null,
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "frequency") val frequency: Long? = null,
) {
    fun mapToDomain(params: MapConfigParams): AcquisitionReminderModel {
        val defaultProductId = if (params.configProvider.storeType == StoreType.AMAZON) {
            "wp.unified.basic"
        } else {
            "wp.classic.basic"
        }
        return AcquisitionReminderModel(
            newSubscriber = (this.newSubscriber ?: RawSubscriberDataModel(
                "Plus, you will be able to save stories to read later, and read offline.",
                "All our journalism, free for 30 days."
            )).mapToDomain(),
            terminatedSubscriber = (terminatedSubscriber ?: RawSubscriberDataModel(
                "Resubscribe and get unlimited access to trusted journalism, save stories to read later, and read offline.",
                "Come back and get the full experience."
            )).mapToDomain(),
            productId = productId ?: defaultProductId,
            ctaDestination = ctaDestination?.mapToDomain() ?: AcquisitionReminderCtaDestination.PAYWALL,
            enabled = enabled ?: true,
            frequency = frequency ?: 86400000,
        )
    }
}

enum class RawAcquisitionReminderCtaDestination {
    @Json(name = "purchase") PURCHASE,
    @Json(name = "paywall") PAYWALL;

    fun mapToDomain(): AcquisitionReminderCtaDestination {
        return when(this) {
            PURCHASE -> AcquisitionReminderCtaDestination.PURCHASE
            PAYWALL -> AcquisitionReminderCtaDestination.PAYWALL
        }
    }
}
