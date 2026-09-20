package com.wapo.flagship.features.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.android.commons.iterable.AttributionInfo
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.wpds.components.CtaButton
import com.wpds.components.HeadlineBold200
import com.wpds.components.Subtitle
import com.wpds.theme.wpdsColors
import com.wpds.wptheme.WpTheme

@Composable
fun BottomSheetPrompt(
    message: BannerPaywallMessage,
    onButtonClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = wpdsColors.appBarBg
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
                    .size(36.dp),
                onClick = {
                    onDismiss.invoke()
                }) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = wpdsColors.gray80
                )
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                message.title?.let {
                    if (it.isNotBlank()) {
                        WpTheme.Text.HeadlineBold200(
                            modifier = Modifier.padding(top = 16.dp)
                                .align(Alignment.CenterHorizontally),
                            text = it,
                            textSize = 24
                        )
                    }
                }

                message.body?.let {
                    if (it.isNotBlank()) {
                        WpTheme.Text.Subtitle(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.CenterHorizontally),
                            text = it,
                        )
                    }
                }

                message.action?.let {
                    if (it.isNotBlank()) {
                        WpTheme.Button.CtaButton(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 16.dp)
                                .wrapContentWidth(),
                            horizontalPadding = 32,
                            textModifier = Modifier,
                            text = it,
                            useElevation = false,
                            textSize = 16,
                            onClick = {
                                onButtonClicked.invoke()
                            }
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun BottomSheetPromptPreview() {
    BottomSheetPrompt(
        BannerPaywallMessage(
            attributionInfo = AttributionInfo(
                placementId = 123,
                campaignId = 456,
                messageId = "789"
            ),
            messageRequirements = null,
            title = "Sign in to save your preferences",
            body = "Create an account or sign in to sync your preferences across devices.",
            action = "Sign in or create an account",
        ), {}, {
            // no-op
        })
}