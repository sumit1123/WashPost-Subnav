package com.wapo.flagship.features.grid.model

import com.washingtonpost.android.recirculation.carousel.models.StyleEntity

data class CarouselSevenLive(
    val items: List<CarouselSevenLiveItem>,
    val label: CompoundLabel?,
    val cta: CompoundLabel?,
    val cardify: Boolean?
) : Item()

data class CarouselSevenLiveItem(
    val headline: ExternalHeadline,
    val media: Media?,
    val link: Link,
    val signature: String?,
    val kicker: String?,
    val liveImage: Boolean?,
    val secondaryText: String?,
    val style: StyleEntity?,
    val timestamp: String? = null,
)