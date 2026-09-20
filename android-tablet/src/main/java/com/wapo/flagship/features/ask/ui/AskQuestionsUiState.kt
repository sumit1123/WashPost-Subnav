// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.ui

import com.wapo.flagship.features.ask.models.QuestionItem

sealed class AskQuestionsUiState {
    class Feed(
        val items: List<QuestionItem>,
    ) : AskQuestionsUiState()

    data object Loading : AskQuestionsUiState()

    data object Error : AskQuestionsUiState()
}
