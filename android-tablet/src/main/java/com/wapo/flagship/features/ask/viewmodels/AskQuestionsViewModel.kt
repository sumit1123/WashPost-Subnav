/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.ask.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.getHourOfDay
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.models.AskThePostQuestionsUiState
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "AskQuestionsViewModel"

@HiltViewModel
class AskQuestionsViewModel @Inject constructor(
    private val askQuestionsRepo: AskQuestionsRepo
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(AskThePostQuestionsUiState())
    val uiState: StateFlow<AskThePostQuestionsUiState> = _uiState.asStateFlow()

    val displayName: String?
        get() {
            if (PaywallService.getInstance().loggedInUser == null) return null
            return PaywallService.getInstance().loggedInUser.firstName
                ?: PaywallService.getInstance().loggedInUser.displayName
        }

    init {
        fetchData()
        setTimeOfDay()
    }

    private fun setTimeOfDay() {
        val askThePostTimeOfDay =
            ConfigManager.getInstance().config.search2Config.askThePost.askThePostTimeOfDay
        val timeOfDay = when (getHourOfDay()) {
            in 0 until 5 -> askThePostTimeOfDay.lateNight
            in 5 until 12 -> askThePostTimeOfDay.goodMorning
            in 12 until 17 -> askThePostTimeOfDay.goodAfternoon
            in 17 until 24 -> askThePostTimeOfDay.goodEvening
            else -> askThePostTimeOfDay.goodMorning
        }

        _uiState.update { state ->
            state.copy(
                timeOfDay = timeOfDay
            )
        }
    }

    fun fetchData(skipCache: Boolean = true) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (val result = askQuestionsRepo.getQuestions(skipCache)) {
                    is APIResult.Success -> {
                        if (!result.isCached) {
                            Logger.d(TAG, "AskQuestions: questions from n/w")
                            _uiState.update {
                                it.copy(
                                    questions = result.data?.questions?.filterNotNull()
                                )
                            }
                        } else {
                            // Show default questions
                            Logger.d(TAG, "AskQuestions: questions from config")
                            _uiState.update { state ->
                                state.copy(
                                    questions = askQuestionsRepo.defaultQuestions
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun updateEnteredText(text: String) {
        _uiState.update { state ->
            state.copy(
                enteredText = text
            )
        }
    }

    companion object {
        const val DISCLAIMER = "This is an experiment. Answers are AI-generated from reporting."
        const val LEARN_MORE_TITLE = "How Ask The Post AI works"
    }
}
