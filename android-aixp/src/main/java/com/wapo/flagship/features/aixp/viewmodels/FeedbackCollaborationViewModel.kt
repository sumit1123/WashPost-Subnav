/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackResponse
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackRequest
import com.wapo.flagship.features.aixp.models.Status
import com.wapo.flagship.features.aixp.models.ArticleSummary
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.aixp.repo.ArticleSummaryRepository
import com.wapo.flagship.features.aixp.repo.PostAnswersFeedbackRepository
import com.wapo.flagship.features.aixp.states.FeedbackSubmissionState
import com.wapo.flagship.features.aixp.ui.FeedbackFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FeedbackCollaborationViewModel @Inject constructor(
    private val articleSummaryRepository: ArticleSummaryRepository,
    private val postAnswersFeedbackRepository: PostAnswersFeedbackRepository
) : ViewModel() {

    private val _feedbackLinkClickEvent: MediatorLiveData<FeedbackFragment.FeedbackType> = LiveEvent()
    val feedbackLinkClickEvent: LiveData<FeedbackFragment.FeedbackType> = _feedbackLinkClickEvent

    private val _feedbackSubmittedEvent: MediatorLiveData<Any> = LiveEvent()
    val feedbackSubmittedEvent: LiveData<Any> = _feedbackSubmittedEvent

    private val _feedbackSubmissionState: MediatorLiveData<FeedbackSubmissionState?> = MediatorLiveData()
    val feedbackSubmissionState: LiveData<FeedbackSubmissionState?> = _feedbackSubmissionState

    fun dispatchFeedbackLinkClickEvent(type: FeedbackFragment.FeedbackType) {
        _feedbackLinkClickEvent.value = type
    }

    fun dispatchFeedbackSubmittedEvent() {
        _feedbackSubmittedEvent.value = Any()
    }

    fun submitArticleSummaryFeedback(
        contentId: String?,
        summary: ArticleSummary?,
        rating: Int,
        feedback: String?,
        fromRealtimeSummary: Boolean?
    ) {
        summary ?: return
        _feedbackSubmissionState.postValue(FeedbackSubmissionState.Loading)
        viewModelScope.launch {
            val feedbackResponse = articleSummaryRepository.submitSummary(
                contentId,
                summary,
                rating,
                feedback,
                fromRealtimeSummary
            )
            when (feedbackResponse) {
                is Status.Network -> {
                    _feedbackSubmissionState.postValue(FeedbackSubmissionState.Success(feedbackResponse.data))
                }

                is Status.Error -> {
                    _feedbackSubmissionState.postValue(FeedbackSubmissionState.Failure(feedbackResponse.message))
                }

                else -> {
                }
            }
        }
    }

    fun submitPostAnswersFeedback(endpoint: String?, responseId: String?, rating: Int, feedback: String?) {
        val body = PostAnswersFeedbackRequest(
            uuid = UUID.randomUUID().toString(),
            responseId = responseId,
            rating = rating,
            feedback = feedback,
            turnId = responseId,
            conversationId = responseId?.substringBeforeLast("_")
        )

        endpoint?.let {
            viewModelScope.launch {
                val feedbackResponse = postAnswersFeedbackRepository.submitConversationFeedback(body, body.uuid)
                when (feedbackResponse) {
                    is APIResult.Success -> {
                        _feedbackSubmissionState.postValue(
                            FeedbackSubmissionState.Success(ArticleSummaryFeedbackResponse(null, null))
                        )
                    }

                    is APIResult.Failure -> {
                        _feedbackSubmissionState.postValue(
                            FeedbackSubmissionState.Failure(feedbackResponse.rawResponse)
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    fun resetFeedbackSubmissionState() {
        _feedbackSubmissionState.value = null
    }

    fun isBetaFlow(): Boolean {
        return AppContextUtils.isDebuggableBuild() && false
    }
}