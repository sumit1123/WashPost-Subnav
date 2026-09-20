package com.washingtonpost.android.recirculation.carousel.models

data class CarouselViewGroup(
    val id: String,
    val requestId: String?,
    val category: String?,
    val currentUrl: String?,
    val items: List<CarouselViewItem>,
)