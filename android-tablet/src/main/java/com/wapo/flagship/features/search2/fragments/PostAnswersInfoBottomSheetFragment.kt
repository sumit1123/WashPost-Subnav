// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.search2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wapo.flagship.features.ask.events.AskQuestionsClickEvent
import com.wapo.flagship.features.ask.ui.AskLearnMoreSheetContent
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

class PostAnswersInfoBottomSheetFragment : BaseBottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                AndroidClassicTheme {
                    Surface(
                        color = wpdsColors.wallPrimaryBg,
                    ) {
                        InfoContent()
                    }
                }
            }
        }

    @Composable
    fun InfoContent() {
        Column(
            modifier =
                Modifier
                    .padding(vertical = 16.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(end = 16.dp)
                        .height(30.dp)
                        .fillMaxWidth(),
            ) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        this@PostAnswersInfoBottomSheetFragment.dismiss()
                    },
                    modifier =
                        Modifier
                            .height(14.dp)
                            .width(14.dp),
                ) {
                    Icon(
                        painter = painterResource(com.wpds.wpds.R.drawable.close),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .height(14.dp)
                                .width(14.dp),
                        tint = wpdsColors.primary,
                    )
                }
            }
            AskLearnMoreSheetContent(false) { event ->
                when (event) {
                    is AskQuestionsClickEvent.DeepLinkClickEvent -> {
                        DeepLinksProcessor.processAsync(event.link, scope = lifecycleScope)
                    }

                    else -> {
                    }
                }
            }
        }
    }

    companion object {
        val TAG: String = PostAnswersInfoBottomSheetFragment::class.java.simpleName
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
private fun Preview() {
    AndroidClassicTheme {
        Surface(
            color = wpdsColors.secondary,
        ) {
            PostAnswersInfoBottomSheetFragment().InfoContent()
        }
    }
}
