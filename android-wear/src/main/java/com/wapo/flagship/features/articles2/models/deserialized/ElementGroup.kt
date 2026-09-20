/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.typeconverters.ItemListTypeConverter

@JsonClass(generateAdapter = true)
data class ElementGroup(
    @Json(name = "additional_properties")
    val additionalProperties: AdditionalProperties?,
    @Json(name = "content_elements")
    @TypeConverters(ItemListTypeConverter::class)
    val contentElements: List<Item>?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "type")
    override val type: String?
) : Item(type)