/* Copyright (c) 2025 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

data class CarouselComments(
    val items: List<CarouselCommentsItem?>?,
    val cardify: Boolean?,
) : Item()

data class CarouselCommentsItem(
    val link: Link?,
    val text: String?,
    val authorName: String?,
    val authorRole: String?,
    val authorAvatarUrl: String?,
    val reactionCount: Int?,
    val repliesCount: Int?,
    val repliesAvatarUrls: List<String?>?,
)
