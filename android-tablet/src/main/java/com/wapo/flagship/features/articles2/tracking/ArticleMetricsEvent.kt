package com.wapo.flagship.features.articles2.tracking

import com.wapo.flagship.features.articles2.states.ArticleContentState

sealed class ArticleMetricsEvent(
    val url: String,
) {
    class StartLoading(
        url: String,
    ) : ArticleMetricsEvent(url)

    class StopLoading(
        url: String,
    ) : ArticleMetricsEvent(url)

    class StartProcessing(
        url: String,
    ) : ArticleMetricsEvent(url)

    class StopProcessing(
        url: String,
    ) : ArticleMetricsEvent(url)

    class StartDrawing(
        url: String,
    ) : ArticleMetricsEvent(url)

    class StopDrawing(
        url: String,
        val source: ArticleContentState.Source,
    ) : ArticleMetricsEvent(
            url,
        )
}
