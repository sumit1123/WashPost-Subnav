/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.repo

import com.wapo.flagship.features.articles2.datasource.local.Articles2LocalDataSource
import com.wapo.flagship.features.articles2.datasource.remote.Articles2RemoteDataSource
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.models.Status
import com.wapo.flagship.network.APIResult
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.features.articles2.utils.UrlUtils
import com.wapo.flagship.utils.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.*
import java.net.URISyntaxException

class Articles2RepositoryImpl(
    private val articles2RemoteDataSource: Articles2RemoteDataSource,
    private val articles2LocalDataSource: Articles2LocalDataSource,
    private val dispatcherProvider: DispatcherProvider
) : Articles2Repository {

    override fun getArticle2(query: Query<Article2>): Flow<Status<out Article2>> = flow {
        val url = getUrl(query)
        val articleCache = articles2LocalDataSource.getArticle2(url)
        if (!query.queryPolicy.needUpdate(articleCache)) {
            if (articleCache != null) {
                emit(Status.Cache(articleCache))
            } else {
                emit(Status.Error("cache is missing"))
            }
        } else {
            val data = articles2RemoteDataSource.getArticle2(
                timeoutMs = query.queryPolicy.timeOut(),
                cache = articleCache,
                url = url
            )
            if (data is APIResult.Success && data.data != null) {
                articles2LocalDataSource.insertArticle2(data.data)
            }
            emit(query.queryPolicy.onResponse(data))
        }
    }
        .catch { t ->
            emit(Status.Error(t.message ?: t.toString()))
        }
        .flowOn(dispatcherProvider.io)

    fun cleanUp() {
        articles2LocalDataSource.cleanUp(System.currentTimeMillis())
    }

    private fun getUrl(query: Query<Article2>): String {
        return try {
            UrlUtils.getUrlWithoutParameters(query.url)
        } catch (ex: URISyntaxException) {
            query.url
        }
    }

}