package com.wapo.flagship.features.onboarding2.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R

@Composable
fun NotificationComposable(
    dialogTitle: String,
    dialogText: String,
    imageResource: Int,
    nightModeEnabled: Boolean,
    onUiEvent: (PushAlertPromptEvent) -> Unit
) {
    val openDialog = remember { mutableStateOf(true) }
    if (openDialog.value) {
        Dialog(onDismissRequest = {}) {
            Measurement.trackAlertPrompt()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (nightModeEnabled) colorResource(R.color.prompt_card_view_bg_night) else Color.White,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    IconButton(
                        onClick = {
                            openDialog.value = false
                            onUiEvent(PushAlertPromptEvent.OnNotificationIconClick(false))
                            Measurement.trackPushPromptDismiss()
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.belvedere_close),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.End),
                            colorFilter = if (nightModeEnabled) ColorFilter.tint(Color.White) else null,
                        )
                    }
                    Image(
                        painter = painterResource(imageResource),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 8.dp)
                                .offset(y = (-16).dp),
                    )
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.h6,
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp),
                        textAlign = TextAlign.Center,
                        color = if (nightModeEnabled) Color.White else Color.Black,
                    )
                    Text(
                        text = dialogText,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.align(Alignment.CenterHorizontally).padding(
                                bottom = 12.dp,
                            ),
                        color = if (nightModeEnabled) Color.White else Color.Black,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        OutlinedButton(
                            onClick = {
                                openDialog.value = false
                                onUiEvent(PushAlertPromptEvent.OnNotificationIconClick(true))
                            },
                            border = null,
                            colors =
                                ButtonColors(
                                    containerColor = colorResource(com.wapo.view.R.color.tooltip_background),
                                    contentColor = Color.White,
                                    disabledContainerColor = colorResource(com.wapo.view.R.color.tooltip_background),
                                    disabledContentColor = Color.White,
                                ),
                        ) {
                            Text(
                                stringResource(R.string.sign_up_for_alerts),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.tap_allow_on_the_next_screen),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp),
                        color = if (nightModeEnabled) Color.White else Color.Black,
                    )
                }
            }
        }
    }
}
