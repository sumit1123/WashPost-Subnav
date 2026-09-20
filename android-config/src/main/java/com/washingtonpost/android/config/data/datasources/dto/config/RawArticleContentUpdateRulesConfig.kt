package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ArticleContentUpdateRulesConfig

/**
 * [timeout] indicates the max time spent on the network request before
 * showing the webview version of the article in the card.
 */
@JsonClass(generateAdapter = true)
data class RawArticleContentUpdateRulesConfig(
    @Json(name = "timeout") val timeout: Long? = null,
    @Json(name = "contentCacheAge") val contentCacheAge: Long? = null,
) {
    fun mapToDomain(): ArticleContentUpdateRulesConfig {
        return ArticleContentUpdateRulesConfig(
            timeout = timeout ?: 5000L,
            contentCacheAge = contentCacheAge ?: 43200000L,
        )
    }
}
