package com.washingtonpost.userhistory.models

data class AutoRecircHelperItem(
    val articleId: String,
    val requestId: String,
    val currentUrl: String,
    val collectionCategory: String,
    val positionInModule: Int,
)
