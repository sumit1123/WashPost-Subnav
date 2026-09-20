package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.TicketForm
import com.washingtonpost.android.config.domain.models.config.ZendeskConfig
import com.washingtonpost.android.config.domain.models.config.ZendeskCustomFields
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawZendeskConfig(
    @Json(name = "url") val url: String? = null,
    @Json(name = "customFields") val customFields: RawZendeskCustomFields? = null,
    @Json(name = "ticketForms") val ticketForms: List<RawTicketForm>? = null,
) {
    fun mapToDomain(params: MapConfigParams): ZendeskConfig {
        return ZendeskConfig(
            url = url ?: "https://washposthelp.zendesk.com",
            customFields = (customFields ?: RawZendeskCustomFields()).mapToDomain(params),
            ticketForms = ticketForms?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawZendeskCustomFields(
    @Json(name = "metadataId") val metadataId: Long? = null,
    @Json(name = "appType") val appType: String? = null,
    @Json(name = "appTypeId") val appTypeId: Long? = null,
    @Json(name = "mobileApp") val mobileApp: String? = null,
    @Json(name = "mobileAppId") val mobileAppId: Long? = null,
) {
    fun mapToDomain(params: MapConfigParams): ZendeskCustomFields {
        val appType = when (params.configProvider.storeType) {
            StoreType.AMAZON -> "Fire_Unified"
            else -> appType ?: "android_classic"
        }
        return ZendeskCustomFields(
            metadataId = metadataId ?: 48269648,
            appType = appType,
            appTypeId = appTypeId ?: 56804708,
            mobileApp = mobileApp ?: "washington_post_app",
            mobileAppId = mobileAppId ?: 48155107
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawTicketForm(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
) {
    fun mapToDomain(): TicketForm {
        return TicketForm(
            id = id ?: -1,
            name = name.orEmpty(),
        )
    }
}
