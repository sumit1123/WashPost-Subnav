/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.datasource.remote

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.network.APIResult

class Articles2RemoteDataSourceImpl(
    private val articles2Service: Articles2Service,
) : Articles2RemoteDataSource {

    override suspend fun getArticle2(
        timeoutMs: Int,
        cache: Article2?,
        url: String
    ): APIResult<Article2> =
        articles2Service.getArticleContent(
            url = url,
            timeoutMs = timeoutMs,
            cache = cache
        )

}