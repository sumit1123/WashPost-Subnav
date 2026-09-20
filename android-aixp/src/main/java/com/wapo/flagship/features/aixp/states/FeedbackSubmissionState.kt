/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.states

import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackResponse

sealed class FeedbackSubmissionState {

    data object Loading : FeedbackSubmissionState()

    class Failure(val message: String?) : FeedbackSubmissionState()

    class Success(val summary: ArticleSummaryFeedbackResponse) : FeedbackSubmissionState()
}