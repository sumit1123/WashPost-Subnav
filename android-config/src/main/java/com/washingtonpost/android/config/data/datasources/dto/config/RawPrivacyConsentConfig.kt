package com.washingtonpost.android.config.data.datasources.dto.config

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.PrivacyConsentConfig
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@JsonClass(generateAdapter = true)
data class RawPrivacyConsentConfig(
    @Json(name = "titleText") val titleText: String? = null,
    @Json(name = "bodyText") val bodyText: String? = null,
    @Json(name = "accountText") val accountText: String? = null,
    @Json(name = "switchTextOn") val switchTextOn: String? = null,
    @Json(name = "switchTextOff") val switchTextOff: String? = null,
    @Json(name = "bottomText") val bottomText: String? = null,
) : Parcelable {

    fun mapToDomain(params: MapConfigParams): PrivacyConsentConfig {
        return PrivacyConsentConfig(
            titleText = titleText.orEmpty(),
            bodyText = bodyText.orEmpty(),
            accountText = accountText.orEmpty(),
            switchTextOn = switchTextOn.orEmpty(),
            switchTextOff = switchTextOff.orEmpty(),
            bottomText = bottomText.orEmpty(),
        )
    }
}