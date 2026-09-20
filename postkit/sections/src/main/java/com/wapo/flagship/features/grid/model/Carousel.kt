/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

data class Carousel(
    val items: List<Bright>
): Item()

data class Bright(
    val media: Media,
    val link: Link,
    val excerpt : Excerpt?
)
