package com.wapo.flagship.features.backendhealth.events

import com.wapo.flagship.features.search2.model.ArticleItem

sealed interface Event {
    class LoadArticles(
        val url: String,
    ): Event

    class ArticleItemClick(
        val item: ArticleItem,
    ): Event
}