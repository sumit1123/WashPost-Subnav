/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Kicker(
    @Json(name = "content")
    val content: String?,
    @Json(name = "coverageActive")
    val coverageActive: Boolean?,
    @Json(name = "displayLabel")
    val displayLabel: String?,
    @Json(name = "displayTransparency")
    val displayTransparency: String?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "liveText")
    val liveText: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "style")
    val style: String?,
) : Item(type = type) {

    enum class SubType(private val value: String) {
        LIVE("Live"),
        EXCLUSIVE("Exclusive"),
        DEFAULT("Default");

        companion object {
            fun getValue(input: String?): SubType {
                for (b in values()) {
                    if (b.value.equals(input, ignoreCase = true)) {
                        return b
                    }
                }
                return DEFAULT
            }
        }

    }

}