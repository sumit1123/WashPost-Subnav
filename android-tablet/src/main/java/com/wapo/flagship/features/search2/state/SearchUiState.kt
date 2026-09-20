package com.wapo.flagship.features.search2.state

import com.wapo.flagship.features.search2.model.*

sealed interface SearchUiState {
    class Landing(
        val recents: List<SearchQueryItem>,
        val top: List<SearchQueryItem> = listOf(),
    ) : SearchUiState

    object Failure : SearchUiState

    object NoMatches : SearchUiState

    object Loading : SearchUiState

    class ElectionLanding(
        val stateslist: List<ElectionItem>,
    ) : SearchUiState

    class Success(
        val sections: List<SectionItem>,
        val articles: List<SearchItem>,
        val totalCount: Int,
        val filters: Map<FilterHeaderItem, List<FilterRadioItem>>?,
    ) : SearchUiState

    object NextPageLoading : SearchUiState

    class NextPageLoaded(
        val articles: List<SearchItem>,
    ) : SearchUiState

    object RecipeLanding : SearchUiState
}
