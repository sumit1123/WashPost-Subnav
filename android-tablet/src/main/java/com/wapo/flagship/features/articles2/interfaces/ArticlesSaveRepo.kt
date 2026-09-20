/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.articles2.interfaces

interface ArticlesSaveRepo {

    suspend fun saveArticles(widgetId: String, items: List<String>?)

    suspend fun getArticleUrlsById(widgetId: String): List<String>

    suspend fun removeArticleUrls(widgetId: String)
}
