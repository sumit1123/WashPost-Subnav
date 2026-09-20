package com.wapo.flagship.features.feedback.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.feedback.models.FeedbackProvider
import com.wapo.flagship.features.feedback.state.FeedbackSubmissionState
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.feedback.domain.FeedbackRepositoryProvider
import com.wapo.flagship.features.feedback.models.PersonalizedPodcastMetadata
import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.feedback.domain.model.Feedback
import com.wapo.kmpshared.features.feedback.domain.model.FeedbackSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val feedbackRepositoryProvider: FeedbackRepositoryProvider,
) : ViewModel() {

    private val _feedbackSubmittedEvent: MediatorLiveData<Any> = LiveEvent()
    val feedbackSubmittedEvent: LiveData<Any> = _feedbackSubmittedEvent

    private val _feedbackSubmissionState: MediatorLiveData<FeedbackSubmissionState?> =
        MediatorLiveData()
    val feedbackSubmissionState: LiveData<FeedbackSubmissionState?> = _feedbackSubmissionState

    fun submitFeedback(provider: FeedbackProvider, rating: Int, feedback: String?) {
        viewModelScope.launch {
            try {
                _feedbackSubmissionState.postValue(FeedbackSubmissionState.Loading)

                val surface = mapToKMPSurface(provider)

                val kmpFeedback = Feedback(
                    surface = surface,
                    rating = rating,
                    comment = feedback
                )

                when (val result = feedbackRepositoryProvider.repository?.submitFeedback(kmpFeedback)) {
                    is KMPResult.Success -> {
                        _feedbackSubmissionState.postValue(FeedbackSubmissionState.Success())
                    }

                    is KMPResult.Error -> {
                        if (AppContextUtils.isConnectingOrConnected()) {
                            RemoteLog.e(
                                context,
                                EventLog
                                    .Builder()
                                    .setMessage("Feedback submission failed: ${result.message}")
                                    .setModule(LogModules.FEEDBACK)
                                    .build(),
                            )
                            _feedbackSubmissionState.postValue(
                                FeedbackSubmissionState.Failure(result.message)
                            )
                        } else {
                            _feedbackSubmissionState.postValue(
                                FeedbackSubmissionState.Failure(result.message)
                            )
                        }
                    }

                    else -> {
                        _feedbackSubmissionState.postValue(
                            FeedbackSubmissionState.Failure("Feedback is disabled")
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _feedbackSubmissionState.postValue(FeedbackSubmissionState.Failure(e.message))
            }
        }
    }

    fun dispatchFeedbackSubmittedEvent() {
        _feedbackSubmittedEvent.value = Any()
    }

    fun resetFeedbackSubmissionState() {
        _feedbackSubmissionState.value = null
    }

    private fun mapToKMPSurface(provider: FeedbackProvider): FeedbackSurface {
        return when (provider.type()) {
            FeedbackSurface.PERSONALIZED_PODCAST -> {
                val nativeMetadata = provider.metadata() as PersonalizedPodcastMetadata
                FeedbackSurface.PersonalizedPodcast(
                    podcastId = provider.contentId(),
                    position = (nativeMetadata.position).toDouble()
                )
            }
            else -> throw IllegalArgumentException("Unsupported feedback surface: ${provider.type()}")
        }
    }

}
