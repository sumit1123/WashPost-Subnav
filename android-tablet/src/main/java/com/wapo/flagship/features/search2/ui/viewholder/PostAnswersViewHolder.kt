// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.search2.ui.viewholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.PostAnswerContainerItem
import com.wapo.flagship.features.search2.ui.PostAnswersContainer
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.PostAnswersContainerBinding
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

class PostAnswersViewHolder(
    val binding: PostAnswersContainerBinding,
    val askThePostEvent: (AskThePostEvent) -> Unit,
    val onItemClick: (UserEvent) -> Unit,
    val askThePostViewModel: AskThePostViewModel? = null,
    val fragmentManager: FragmentManager? = null
) : Search2Adapter.SearchViewHolder<PostAnswerContainerItem>(binding.root) {
    override fun bind(item: PostAnswerContainerItem) {
        super.bind(item)
        if (askThePostViewModel == null || fragmentManager == null) return
        binding.root.setContent {
            AndroidClassicTheme {
                Surface(
                    color = wpdsColors.findBg,
                ) {
                    val modifier = Modifier
                        .padding(8.dp)
                        .wrapContentHeight()
                        .background(wpdsColors.findBg)
                        .padding(8.dp)
                    PostAnswersContainer(
                        askThePostUIState = askThePostViewModel.uiState.collectAsStateWithLifecycle().value,
                        askThePostUIEvent = askThePostEvent,
                        postAnswersUIState = item.state,
                        onUserEvent = onItemClick,
                        modifier = modifier,
                        fragmentManager = fragmentManager
                    )
                }
            }
        }
    }
}
