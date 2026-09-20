package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.AskThePostUIState
import com.wapo.flagship.features.ask.models.PostIterableBannerEvent
import com.washingtonpost.android.R
import com.wpds.components.CtaButton
import com.wpds.components.WapoText
import com.wpds.theme.wpdsColors
import com.wpds.wptheme.WpTheme

@Composable
fun AskThePostIterableBanner(
    askThePostUIState: AskThePostUIState,
    askThePostUiEvent: (AskThePostEvent) -> Unit
) {
    askThePostUIState.askThePostMessage?.let { banner ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(wpdsColors.blue600)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(wpdsColors.blue600)
                    .padding(8.dp)
            ) {
                WpTheme.Text.WapoText(
                    modifier = Modifier
                        .align(Alignment.CenterVertically).weight(1f)
                        .padding(end = 8.dp),
                    banner.title ?: "",
                    textSize = 12,
                    textColor = Color.Black
                )
                WpTheme.Button.CtaButton(
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .wrapContentHeight(),
                    textModifier = Modifier,
                    text = banner.action ?: "",
                    horizontalPadding = 12,
                    onClick = {
                        val event = if (!banner.blocker.isNullOrEmpty()) {
                            PostIterableBannerEvent.Subscribing
                        } else {
                            PostIterableBannerEvent.Dismiss
                        }
                        askThePostUiEvent.invoke(
                            AskThePostEvent.ATPCtaClicked(
                                banner,
                                event
                            )
                        )
                    }
                )

                IconButton(onClick = {
                    askThePostUiEvent.invoke(AskThePostEvent.ATPCtaDismissed(banner))
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = wpdsColors.gray80
                    )
                }
            }
        }
    }


}