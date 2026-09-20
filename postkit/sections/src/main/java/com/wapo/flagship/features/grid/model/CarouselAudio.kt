/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.ItemType

data class CarouselAudio(
    var items: List<CarouselAudioItem>,
    val cardify: Boolean?,
    var carouselType: ItemType? = null
) : Item()

data class CarouselAudioItem(
    val audioArticle: AudioArticle?,
    val audio: Audio?,
    val headline: String,
    val media: Media?,
    val link: Link?,
    val carouselItemType: String? = null
)
