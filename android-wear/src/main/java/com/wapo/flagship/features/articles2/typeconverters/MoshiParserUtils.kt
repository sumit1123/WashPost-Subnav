/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

@file:JvmName("MoshiParserUtils")

package com.wapo.flagship.features.articles2.typeconverters

import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Article2JsonAdapter

fun parseArticle(jsonString: String, moshi: Moshi): Article2? {
    return try {
        Article2JsonAdapter(moshi).fromJson(jsonString)
    } catch (t: Throwable) {
        null
    }
}