/*
 *  Copyright (c) 2024 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.placeholder

data class PlaceHolderData(
    val message: String,
    val reloadLabel: String? = null,
    val reloadIcon: Int? = null,
    val aspectRatio: Float? = null,
    val onLoadResource: () -> Unit
)
