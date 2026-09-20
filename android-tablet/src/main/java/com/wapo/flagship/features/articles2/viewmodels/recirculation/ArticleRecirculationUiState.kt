package com.wapo.flagship.features.articles2.viewmodels.recirculation

import com.washingtonpost.android.recirculation.carousel.models.CarouselViewGroup

data class ArticleRecirculationUiState(
    val isLoading: Boolean = false,
    val collections: Map<String, CarouselViewGroup> = emptyMap(),
    val error: String? = null,
)