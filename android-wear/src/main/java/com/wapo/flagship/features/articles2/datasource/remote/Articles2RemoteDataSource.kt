/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.datasource.remote

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.network.APIResult

interface Articles2RemoteDataSource {
    suspend fun getArticle2(timeoutMs: Int, cache: Article2?, url: String): APIResult<Article2>
}