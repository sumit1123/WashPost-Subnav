package com.washingtonpost.userhistory.models

data class HeadlineViewHelperItem(
    val headlineViewEvent: DefaultHeadlineViewEvent,
    val adapterPosition: Int,
    var startTime: Long? = null
)