/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

import com.washingtonpost.android.recirculation.carousel.models.StyleEntity

data class CarouselImmersion(
    val items: List<CarouselImmersionItem>,
    val label: CompoundLabel?,
    val cta: CompoundLabel?,
    val cardify: Boolean?
) : Item()

data class CarouselImmersionItem(
    val headline: ImmersionHeadline,
    val media: Media?,
    val link: Link,
    val signature: String?,
    val kicker: String?,
    val liveImage: Boolean?,
    val secondaryText: String?,
    val style: StyleEntity?
)
