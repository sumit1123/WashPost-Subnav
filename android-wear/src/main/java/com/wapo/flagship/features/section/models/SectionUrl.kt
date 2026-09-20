/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.models

data class SectionUrl(
    val endpoint: String,
    val pageType: PageType = PageType.PAGE_BUILDER
)