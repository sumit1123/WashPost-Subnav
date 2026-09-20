package com.wapo.flagship.features.personalizedpodcasts.model

data class PersoPodTrackingInfo(
    val date: String? = null,
    val touchpoint: String? = null,
    val podcastType: String? = null,
    val tags: String? = null,
    val id: String? = null,
    val title: String? = null,
    val isRollThrough: Boolean? = false,
    val transcriptUrl: String? = null,
    val sources: List<com.wapo.view.habittiles.ArticleSource>? = null
)
