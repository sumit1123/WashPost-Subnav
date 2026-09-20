/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.grid.model.Cell


@JsonClass(generateAdapter = true)
data class TableItem(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "header")
    var header: Array<Cell> = emptyArray(),
    @Json(name = "rows")
    var row: Array<Array<Cell>>

) : Item(type = type) {

    @JsonClass(generateAdapter = true)
    data class Cell(@Json(name = "content") val content: String?)
}