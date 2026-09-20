package com.wapo.flagship.features.grid.model

data class BlurbList(
    val items: List<BlurbItem>?,
    val info: BlurbInfo?,
    val style: BlurbStyle?
)

data class BlurbItem(
    val text: String?,
    val type: BulletType = BulletType.NORMAL,
    val mime: String?
)

data class BlurbInfo(
    val size: Size?,
    val fontStyle: BlurbFontStyle?
)

enum class BlurbFontStyle {
    NORMAL_STYLE,
    LIKE_ARTICLE_BODY
}

enum class BlurbStyle {
    CONVERSATIONS
}
