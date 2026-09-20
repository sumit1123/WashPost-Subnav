/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.datasource.local

import com.wapo.flagship.features.articles2.models.Article2

interface Articles2LocalDataSource {

    suspend fun getArticle2(url: String): Article2?

    suspend fun insertArticle2(vararg article: Article2)

    fun cleanUp(ttl: Long)

}