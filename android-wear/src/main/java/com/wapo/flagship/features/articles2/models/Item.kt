/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class Item(
    @Json(name = "type")
    open val type: String? = null
) {
    override fun equals(other: Any?): Boolean {
        return if (other as? Item == null) {
            false
        } else {
            other == this
        }
    }

    override fun hashCode(): Int {
        var result = this.hashCode()
        result *= 31
        return result
    }
}