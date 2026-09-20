package com.wapo.flagship.features.search2.model

sealed class SearchResultApiStatus {
    data class Success(
        val articlesResult: Results,
        val sections: List<Section>,
        val filters: Filters? = null,
    ) : SearchResultApiStatus()

    data class LoadMore(
        val articles: List<Document>,
    ) : SearchResultApiStatus()

    object Failure : SearchResultApiStatus()
}
