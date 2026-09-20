package com.wapo.flagship.features.articles2.models

data class TableOfContentsEvent(
    val metaId: String?,
    val tableOfContents: TableOfContents,
    val currentAnchorId: String? = null,
)
