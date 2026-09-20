package com.wapo.flagship.features.grid.model

import com.washingtonpost.android.recirculation.carousel.models.StyleEntity

data class CarouselExternal(
    val items: List<CarouselExternalItem>,
    val label: CompoundLabel?,
    val cta: CompoundLabel?,
    val cardify: Boolean?,
): Item()

data class CarouselExternalItem(
    val headline: ExternalHeadline,
    val media: Media?,
    val link: Link,
    val signature: String?,
    val kicker: String?,
    val liveImage: Boolean?,
    val secondaryText: String?,
    val style: StyleEntity?
)

data class ExternalHeadline(
    val text: String,
    val prefix: String = "",
)