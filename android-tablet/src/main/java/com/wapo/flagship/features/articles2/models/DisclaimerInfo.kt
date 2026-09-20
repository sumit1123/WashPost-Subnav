package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class DisclaimerInfo(
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "sections")
    val sections: List<DisclaimerSection>? = null,
): Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DisclaimerSection(
    @Json(name = "heading")
    val heading: String? = null,
    @Json(name = "content")
    val content: String? = null,
): Parcelable