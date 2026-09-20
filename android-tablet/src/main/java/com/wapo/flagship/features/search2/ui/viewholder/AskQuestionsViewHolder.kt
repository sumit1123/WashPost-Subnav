// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.search2.ui.viewholder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapo.flagship.features.ask.events.AskQuestionsClickEvent
import com.wapo.flagship.features.ask.ui.AskQuestionsContainer
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.AskQuestionsItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchAskQuestionsItemBinding
import com.wpds.theme.AndroidClassicTheme

class AskQuestionsViewHolder(
    val binding: SearchAskQuestionsItemBinding,
    private val viewModel: AskQuestionsViewModel?,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<AskQuestionsItem>(binding.root) {
    override fun bind(item: AskQuestionsItem) {
        super.bind(item)
        viewModel ?: return
        binding.askQuestionsComposeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool,
            )
            setContent {
                AndroidClassicTheme {
                    val askThePostQuestionUIState =
                        viewModel.uiState.collectAsStateWithLifecycle().value

                    Surface(
                        color = Color.Unspecified,
                    ) {
                        val questions =
                            askThePostQuestionUIState.questions
                        if (questions?.isEmpty() == true) return@Surface
                        Column(
                            modifier =
                                Modifier
                                    .padding(top = 16.dp, bottom = 0.dp)
                                    .fillMaxWidth(),
                        ) {
                            AskQuestionsContainer(askThePostQuestionUIState, true) { event ->
                                when (event) {
                                    AskQuestionsClickEvent.LearnMoreClickEvent -> {
                                    }

                                    is AskQuestionsClickEvent.QuestionClickEvent -> {
                                        onItemClick.invoke(
                                            UserEvent.AskQuestionItemClick(
                                                event.id,
                                                event.question
                                            ),
                                        )
                                    }

                                    is AskQuestionsClickEvent.DeepLinkClickEvent -> {
                                        onItemClick.invoke(UserEvent.DeepLinkItemClick(event.link))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
