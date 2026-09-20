package com.washingtonpost.android.save.models

import com.washingtonpost.android.save.types.MyPostSection

data class ArticleActionItem(
    val section: MyPostSection,
    val url: String,
    val isPreviewHeroArticle: Boolean = false,
    val articleList: List<MyPostArticleItem>? = null,
    val recipePageName: String = "",
    val itemClicked: MyPostArticleItem? = null,
    val shouldPlayAudioArticle: Boolean = false
)