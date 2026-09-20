/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class OlympicsMedals(
    @Json(name = "type")
    override val type: String?,

    @Json(name = "title")
    val title: String?,
    @Json(name = "link")
    val linkText: String?,
    @Json(name = "linkURL")
    val linkURL: String?,
    @Json(name = "data")
    val data: MutableList<CountryMedalItem?>?,
) : Item(type = type)

@JsonClass(generateAdapter = true)
data class CountryMedalItem(
    @Json(name = "rank")
    val rank: Int?,
    @Json(name = "icon")
    val icon: String?,
    @Json(name = "label")
    val label: String?,
    @Json(name = "bronze")
    val bronze: Int?,
    @Json(name = "silver")
    val silver: Int?,
    @Json(name = "gold")
    val gold: Int?
)