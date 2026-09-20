package com.wapo.flagship.features.search2.state

import com.wapo.flagship.features.search2.ui.PostAnswerUIItem

sealed class PostAnswersUIState {
    data object Loading : PostAnswersUIState()

    data class Success(
        val data: List<PostAnswerUIItem>,
    ) : PostAnswersUIState()

    data object Error : PostAnswersUIState()
}
