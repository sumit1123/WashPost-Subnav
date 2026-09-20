package com.washingtonpost.android.recirculation.carousel.models

data class EllipsisActionItem(
    val url: String = "",
    val imageUrl: String = "",
    val byline: String = "",
    val headline: String = "",
    val articleList: List<String>? = null,
    val pageName: String = "",
    val articleLinkIsWebType: Boolean = false,
    val carouselItemType: String = ""
)
