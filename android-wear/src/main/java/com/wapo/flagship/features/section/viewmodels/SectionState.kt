/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.viewmodels

import com.wapo.flagship.features.section.models.ArticleMeta

data class SectionState(
    val articleMetaList: List<ArticleMeta>,
    val isLoading: Boolean
)
