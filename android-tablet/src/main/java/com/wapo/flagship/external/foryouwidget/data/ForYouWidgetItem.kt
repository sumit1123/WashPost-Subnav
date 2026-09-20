package com.wapo.flagship.external.foryouwidget.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForYouWidgetItem(
    val arcId: String?,
    val headline: String?,
    val url: String?,
    val imageUrl: String?,
    val category: String?,
    val displayAge: String?,
    val isConsumed: Boolean = false
)