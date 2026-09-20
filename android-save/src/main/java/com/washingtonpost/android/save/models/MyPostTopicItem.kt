/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.models

data class MyPostTopicItem(
    val topicId: String,
    val iconUrl: String,
    val text: String,
    val destinationUrl: String?,
    val isFollowing: Boolean
)
