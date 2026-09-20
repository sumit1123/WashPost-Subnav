/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.datasource.local

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.roomdb.AppDatabase

class Articles2LocalDataSourceImpl(
    private val appDatabase: AppDatabase
): Articles2LocalDataSource {

    override suspend fun getArticle2(url: String): Article2? =
        appDatabase.articlesDao().getArticle(url)

    override suspend fun insertArticle2(vararg article: Article2) {
        appDatabase.articlesDao().insertArticle(*article)
    }

    override fun cleanUp(ttl: Long) {
        appDatabase.articlesDao().cleanUp(ttl)
    }

}