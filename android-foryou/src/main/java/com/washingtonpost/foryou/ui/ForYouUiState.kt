package com.washingtonpost.foryou.ui

import com.squareup.moshi.Json
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem

sealed class ForYouUiState {
    class Feed(val requestId: String?, val recipeId: String?, val testId: String?,
               val items: List<RecommendationsItem>) : ForYouUiState()
    object Loading: ForYouUiState()
    object SwipeToRefresh: ForYouUiState()
    object Error: ForYouUiState()
}