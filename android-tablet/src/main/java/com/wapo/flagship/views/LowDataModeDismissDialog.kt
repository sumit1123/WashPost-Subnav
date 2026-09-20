/*
 *  Copyright (c) 2024 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.wapo.flagship.features.lowdata.LowDataModeNotificationConfigurationImpl
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.Companion.DISMISS_START_COUNTER
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.LowDataModeNotificationState
import com.washingtonpost.android.sections.R

@Composable
fun LowDataModeDismissDialog(
    lowDataModeNotificationHelper: LowDataModeNotificationConfigurationImpl?,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    AlertDialog(
        modifier =
            Modifier
                .padding(horizontal = 10.dp),
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
            ),
        title = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = stringResource(id = R.string.low_data_mode_dialog_title),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily(Font(com.washingtonpost.android.sections.R.font.roboto_regular)),
                )
            }
        },
        onDismissRequest = {
            onAllow()
        },
        buttons = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 25.dp,
                            vertical = 20.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LowDataModeDialogButton(
                    Modifier.clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                        ),
                    ),
                    text = stringResource(id = R.string.low_data_mode_dialog_allow),
                ) {
                    onAllow()
                }
                LowDataModeDialogButton(
                    text = stringResource(id = R.string.low_data_mode_dialog_snooze),
                ) { onSnooze() }
                LowDataModeDialogButton(
                    Modifier.clip(
                        RoundedCornerShape(
                            bottomStart = 14.dp,
                            bottomEnd = 14.dp,
                        ),
                    ),
                    text = stringResource(id = R.string.low_data_mode_dialog_dont_allow),
                ) { onDismiss() }
            }
        },
    )
}

@Composable
fun LowDataModeDialogButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = 60.dp)
                .padding(vertical = 2.dp),
        colors =
            ButtonDefaults.buttonColors(
                backgroundColor = colorResource(id = R.color.low_data_dismiss_banner_buttons),
            ),
        onClick = {
            onClick()
        },
    ) {
        Text(
            modifier = Modifier.padding(10.dp),
            text = text,
            color = colorResource(id = com.wapo.flagship.features.audio.R.color.white),
            fontFamily = FontFamily(Font(com.washingtonpost.android.sections.R.font.roboto_regular)),
        )
    }
}

@Preview
@Composable
fun SimpleComposablePreview() {
    LowDataModeDismissDialog(
        LowDataModeNotificationConfigurationImpl(
            notificationState = LowDataModeNotificationState.Dialog,
            DISMISS_START_COUNTER
        ),
        {},
        {},
        {},
    )
}
