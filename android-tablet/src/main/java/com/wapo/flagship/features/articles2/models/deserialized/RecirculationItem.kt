// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.models.deserialized

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Item

/**
 * Placeholder item to hold recirculation module at the end of article.
 */
data class RecirculationItem(
    val article: Article2,
    val recirculationType: RecirculationType,
) : Item(type = Types.RECIRC.type)

/**
 * Recirculation module data type with section display name.
 */
enum class RecirculationType(
    var sectionName: String,
) {
    MOST_READ("Most read"),
}
