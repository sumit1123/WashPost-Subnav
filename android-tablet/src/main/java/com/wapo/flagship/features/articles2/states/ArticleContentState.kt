package com.wapo.flagship.features.articles2.states

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Article415
import com.wapo.flagship.features.articles3.models.ui.ArticleItemUiModel

sealed class ArticleContentState {
    object Loading : ArticleContentState()

    object Failure : ArticleContentState()

    object Processing : ArticleContentState()

    /**
     * @param article data to display
     * @param source indicates where the data came from
     * @param pendingArticle updated data (LUF) that has not been displayed yet
     * @param updatesCount number of updates received in background
     * @param isUpdate indicates that the data needs to be re-rendered
     */
    class Success(
        val article: Article2,
        val uiItems: List<ArticleItemUiModel>,
        val source: Source,
        val pendingArticle: Article2? = null,
        val updatesCount: Int = 0,
        val isUpdate: Boolean = false,
    ) : ArticleContentState()

    class Unsupported(
        val article: Article2? = null,
        val article415: Article415? = null,
    ) : ArticleContentState()

    object UITimedOut : ArticleContentState()

    enum class Source { NETWORK, CACHE }
}
