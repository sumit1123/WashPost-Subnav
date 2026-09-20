package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.search2.ui.WidthFactor

@JsonClass(generateAdapter = true)
data class PostAnswerDivider(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "width_factor")
    val widthFactor: String? = WidthFactor.DEFAULT.value,
) : PostAnswerItem(
        type = type,
    )
