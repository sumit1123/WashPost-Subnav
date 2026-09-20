package com.washingtonpost.foryou.data

data class ForYouModel(
    val id: String,
    val headline: String,
    val byline: String,
    val authorImageUrl: String?,
    val timeStamp: Long,
    val blurb: String,
    val image: String,
    val url: String,
    val sectionLabel: String,
    val hasAudio: Boolean,
    val headlinePrefix: String?,
    val transparencyLabel : String?
)