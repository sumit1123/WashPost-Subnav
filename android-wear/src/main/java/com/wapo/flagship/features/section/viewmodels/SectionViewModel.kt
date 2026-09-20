/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.section.use_cases.SectionUseCases
import com.wapo.flagship.utils.UiEvent
import com.wapo.flagship.utils.UiText
import com.washingtonpost.android.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * View model for Section feature.
 *
 */
@HiltViewModel
class SectionViewModel @Inject constructor(
    private val sectionUseCases: SectionUseCases
) : ViewModel() {

    private var getFusionSectionJob: Job? = null
    private var getPageBuilderSectionJob: Job? = null

    private val _sectionState = MutableStateFlow(
        SectionState(
            articleMetaList = emptyList(),
            isLoading = false
        )
    )
    val sectionState = _sectionState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun getFusionSection(sectionName: String) {
        getFusionSectionJob?.cancel()
        _sectionState.update {
            it.copy(
                articleMetaList = emptyList(),
                isLoading = true
            )
        }
        getFusionSectionJob = sectionUseCases.getFusionSection(sectionName)
            .onEach { articleListResult ->
                articleListResult
                    .onSuccess { articleList ->
                        _sectionState.update {
                            it.copy(
                                articleMetaList = articleList,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure {
                        _sectionState.update {
                            it.copy(isLoading = false)
                        }
                        _uiEvent.send(
                            UiEvent.ShowSnackbar(
                                UiText.StringResource(R.string.error_loading_section)
                            )
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

    fun getPageBuilderSection(sectionName: String) {
        getPageBuilderSectionJob?.cancel()
        _sectionState.update {
            it.copy(
                articleMetaList = emptyList(),
                isLoading = true
            )
        }
        getPageBuilderSectionJob = sectionUseCases.getPageBuilderSection(sectionName)
            .onEach { articleListResult ->
                articleListResult
                    .onSuccess { articleList ->
                        _sectionState.update {
                            it.copy(
                                articleMetaList = articleList,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure {
                        _sectionState.update {
                            it.copy(isLoading = false)
                        }
                        _uiEvent.send(
                            UiEvent.ShowSnackbar(
                                UiText.StringResource(R.string.error_loading_section)
                            )
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

}