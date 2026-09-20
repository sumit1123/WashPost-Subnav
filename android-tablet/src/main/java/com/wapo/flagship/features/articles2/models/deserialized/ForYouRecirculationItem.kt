/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.articles2.models.deserialized

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Item
import com.washingtonpost.foryou.data.RecommendationsItem

/**
 * Placeholder item to hold recirculation module at the end of article.
 */
data class ForYouRecirculationItem(
    val article: Article2,
    val list: List<RecommendationsItem>
) : Item(type = Types.RECIRC.type)
