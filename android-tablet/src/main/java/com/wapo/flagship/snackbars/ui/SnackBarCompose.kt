package com.wapo.flagship.snackbars.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Icon
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.snackbars.model.SnackBarEvent
import com.washingtonpost.android.R

@Preview
@Composable
fun NoConnectionSnackbar(onClick: (SnackBarEvent) -> Unit = {}) {
    Snackbar(backgroundColor = Color.Black) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.alert_network_problems),
                color = Color.White,
            )
            Text(
                text = stringResource(id = R.string.alert_settings),
                color = Color.LightGray,
                modifier =
                    Modifier
                        .wrapContentSize()
                        .clickable {
                            onClick(SnackBarEvent.OpenSettings)
                        },
            )
        }
    }
}

@Preview
@Composable
fun TurnOnLowDataModeSnackbar(onClick: (SnackBarEvent) -> Unit = {}) {
    Snackbar(backgroundColor = colorResource(id = com.wapo.flagship.features.audio.R.color.low_data_mode_snackbar_background)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.low_data_mode_snackbar_message),
                color = Color.White,
            )
            Text(
                text = stringResource(id = R.string.low_data_mode_snackbar_turn_on),
                color = colorResource(id = com.wapo.flagship.features.audio.R.color.low_data_mode_snackbar_turn_on_color),
                modifier =
                    Modifier
                        .wrapContentSize()
                        .padding(horizontal = 20.dp)
                        .clickable {
                            onClick(SnackBarEvent.TurnOnLowDataMode)
                        },
            )
            Icon(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clickable {
                            onClick(SnackBarEvent.DismissLowDataMode)
                        },
                painter = painterResource(id = com.washingtonpost.android.paywall.R.drawable.ic_close),
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

@Preview
@Composable
fun AddToPlaylistSnackbar(onClick: (SnackBarEvent) -> Unit = {}) {
    val viewList = "View Playlist"
    val annotatedString =
        buildAnnotatedString {
            append(text = "Added to audio playlist. ")
            withStyle(style = SpanStyle(color = Color.Black, textDecoration = TextDecoration.Underline)) {
                pushStringAnnotation(tag = "link", annotation = viewList)
                append(viewList)
            }
        }

    Snackbar(backgroundColor = colorResource(id = com.wapo.flagship.features.audio.R.color.snackbar_background)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_green_tick),
                contentDescription = null,
                tint = Color.Unspecified,
            )
            ClickableText(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                text = annotatedString,
                onClick = { offset ->
                    annotatedString
                        .getStringAnnotations(offset, offset)
                        .firstOrNull()
                        ?.let {
                            onClick(SnackBarEvent.OpenListenToThePost)
                        }
                },
            )
            Icon(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clickable {
                            onClick(SnackBarEvent.Dismiss)
                        },
                painter = painterResource(id = R.drawable.ic_close_2),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
    }
}
