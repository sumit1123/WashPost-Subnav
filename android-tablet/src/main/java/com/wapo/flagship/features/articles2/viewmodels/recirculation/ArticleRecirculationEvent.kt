package com.wapo.flagship.features.articles2.viewmodels.recirculation

import com.wapo.flagship.features.articles2.activities.ArticlesParcel

sealed class ArticleRecirculationEvent {
    data class OpenArticle(val articlesParcel: ArticlesParcel.Builder) : ArticleRecirculationEvent()
}