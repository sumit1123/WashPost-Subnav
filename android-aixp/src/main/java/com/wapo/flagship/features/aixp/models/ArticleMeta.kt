/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.models

/**
 * Article data from the app module to send data in api calls
 */
data class ArticleMeta(
    val contentId: String,
    val contentUrl: String,
    val lmt: Long
)