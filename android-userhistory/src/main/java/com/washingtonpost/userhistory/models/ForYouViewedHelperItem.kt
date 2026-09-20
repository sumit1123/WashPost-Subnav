package com.washingtonpost.userhistory.models

data class ForYouViewedHelperItem(
    val forYouViewedItem: ForYouViewedItem,
    val adapterPosition: Int,
    var startTime: Long? = null
)
