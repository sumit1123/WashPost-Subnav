// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ads.targeting.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.ads.targeting.models.ContentResponse
import com.wapo.flagship.features.ads.targeting.repo.ContentRepository
import com.wapo.flagship.features.ads.targeting.ui.ContentUIState
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.config.domain.models.config.ContextualTargetingContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val config: ContextualTargetingContent?,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableLiveData<ContentUIState>(ContentUIState.Loading(""))
    val uiState: LiveData<ContentUIState> = _uiState
    /**
     * A job that will run in the background for UI timeout
     */
    var job: Job? = null

    fun getContent(id: String) {
        if (uiState.value?.id == id && uiState.value is ContentUIState.Loading) return
        cancelOnGoingRequestIfAny()
        _uiState.value = ContentUIState.Loading(id)
        viewModelScope.launch {
            startTimer {
                if (uiState.value is ContentUIState.Loading)
                    _uiState.postValue(ContentUIState.UITimeout(id))
            }
            withContext(Dispatchers.IO) {
                val result = contentRepository.getContent(id)
                if (uiState.value?.id == id) {
                    withContext(Dispatchers.Main) {
                        updateState(id, result)
                    }
                }
            }
        }
    }

    private fun updateState(videoId: String, result: APIResult<ContentResponse>) {
        when (result) {
            is APIResult.Success -> {
                _uiState.value = ContentUIState.Content(
                    videoId,
                    result.data?.items?.filterNotNull() ?: emptyList()
                )
            }

            is APIResult.Failure -> _uiState.value = ContentUIState.Error(videoId)
            is APIResult.NetworkError -> _uiState.value = ContentUIState.Error(videoId)
        }
    }

    private fun cancelOnGoingRequestIfAny() {
        val id = _uiState.value?.id ?: return
        if (uiState.value is ContentUIState.Loading && id.isNotEmpty()) {
            _uiState.value = ContentUIState.Cancelled(id)
        }
        job?.cancel()
    }

    private fun startTimer(callback: () -> Unit) {
        val timeoutMillis = config?.timeout ?: return
        job?.cancel()
        job = viewModelScope.launch(dispatcherProvider.io) {
            delay(timeoutMillis)
            callback.invoke()
        }
    }
}