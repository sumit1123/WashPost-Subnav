/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.washingtonpost.android.config.domain.models.config

data class AgeRestrictionConfig(
    val androidEnabled: Boolean = false,
    val amazonEnabled: Boolean = false,
    val minAmazonOsVersion: Int? = null,
    val chromebookEnabled: Boolean = false
)
