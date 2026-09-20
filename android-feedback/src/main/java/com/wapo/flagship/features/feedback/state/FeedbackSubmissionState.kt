package com.wapo.flagship.features.feedback.state

sealed class FeedbackSubmissionState {

    object Loading : FeedbackSubmissionState()

    data class Success(
        val message: String? = null
    ) : FeedbackSubmissionState()

    data class Failure(
        val errorMessage: String? = null
    ) : FeedbackSubmissionState()
}