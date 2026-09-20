/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.states

import com.wapo.flagship.features.aixp.models.ArticleSummary

sealed class ArticleSummaryState {

    data object Loading : ArticleSummaryState()

    class Failure(val message: String?) : ArticleSummaryState()

    class Success(val summary: ArticleSummary) : ArticleSummaryState()
}