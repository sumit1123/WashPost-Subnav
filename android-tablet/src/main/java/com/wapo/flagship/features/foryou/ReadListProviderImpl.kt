package com.wapo.flagship.features.foryou

import com.wapo.android.commons.util.URLParser
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.foryou.ConsumedListProvider
import com.washingtonpost.foryou.data.ConsumedArticles

class ReadListProviderImpl(
    val savedArticleManager: SavedArticleManager,
) : ConsumedListProvider {
    override suspend fun getReadList(): List<ConsumedArticles> =
        savedArticleManager
            .getArticles(limit = 100)
            .map {
                ConsumedArticles(
                    articleUrl = getPath(it.contentURL),
                    timestamp = it.lmt ?: 0,
                )
            }

    /**
     * Strictly app should use paths from urls for readlist.
     */
    fun getPath(url: String): String {
        val urlParser = URLParser(url)
        return if (urlParser.getDomain().isNullOrEmpty()) url else urlParser.getPath()
    }
}
