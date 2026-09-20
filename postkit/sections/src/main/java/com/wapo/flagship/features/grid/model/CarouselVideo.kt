/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.posttv.model.Video

data class CarouselVideo(
    val items: List<CarouselVideoItem>
) : Item()

data class CarouselVideoItem(val media: Media, val video: Video)
