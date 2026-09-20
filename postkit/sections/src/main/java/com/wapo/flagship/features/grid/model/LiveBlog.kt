/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

data class LiveBlog(
    val primeTimeURL: String?,
    val numToShow: Int,
    val subtypes: List<String>?,
    val showTimestamps: Boolean,
    val compoundLabel: CompoundLabel? = null
)