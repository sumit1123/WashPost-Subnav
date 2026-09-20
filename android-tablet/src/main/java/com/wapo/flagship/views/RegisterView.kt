package com.wapo.flagship.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.washingtonpost.android.R
import com.wpds.components.CtaButton
import com.wpds.components.CtaOutlinedButton
import com.wpds.components.WapoText
import com.wpds.theme.wpdsColors
import com.wpds.wptheme.WpTheme

@Composable
fun RegisterView(
    header: String,
    subtitle: String,
    ctaText: String,
    ctaSubTextPrefix: String,
    ctaSubTextSuffix: String,
    showCloseButton: Boolean,
    secondaryCtaText: String? = null,
    dismissModal: () -> Unit = {},
    primaryButtonClicked: () -> Unit = {},
    secondaryButtonClicked: () -> Unit = {},
    linkClicked: () -> Unit = {},
) {

    Box {

        if (showCloseButton) {
            IconButton(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd),
                onClick = {
                    dismissModal.invoke()
                }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = wpdsColors.gray80
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(24.dp)
        ) {

            WpTheme.Text.WapoText(
                modifier = Modifier.padding(bottom = 8.dp),
                text = header,
                textSize = 18,
                isBold = true
            )
            WpTheme.Text.WapoText(
                text = subtitle,
                textSize = 16,
                isBold = false
            )

            WpTheme.Button.CtaButton(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                textModifier = Modifier,
                text = ctaText,
                useElevation = false,
                textSize = 16,
                onClick = {
                    primaryButtonClicked.invoke()
                }
            )

            secondaryCtaText?.let {
                WpTheme.Button.CtaOutlinedButton(
                    modifier = Modifier
                        .padding(top = 0.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    textModifier = Modifier,
                    text = it,
                    textSize = 16,
                    textColor = wpdsColors.gray0,
                    useElevation = false,
                    backgroundColorResId = com.wpds.wpds.R.color.gray700,
                    onClick = {
                        secondaryButtonClicked.invoke()
                    }
                )
            }

            val annotatedString = buildAnnotatedString {
                append(ctaSubTextPrefix)
                append(" ")
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(ctaSubTextSuffix)
                }
            }
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .clickable {
                        linkClicked.invoke()
                    },
                text = annotatedString,
                color = wpdsColors.gray0,

            )

        }
    }
}

@Composable
@Preview
fun RegisterViewPreview() {
    RegisterView(
        header = stringResource(R.string.ask_the_post_register_headline),
        subtitle = stringResource(R.string.ask_the_post_register_subtitle),
        ctaText = stringResource(R.string.ask_the_post_register_cta),
        ctaSubTextPrefix = stringResource(R.string.ask_the_post_register_cta_subtitle_prefix),
        ctaSubTextSuffix = stringResource(R.string.ask_the_post_register_cta_subtitle_suffix),
        secondaryCtaText = stringResource(R.string.ask_the_post_save_chat_cta),
        showCloseButton = true,
        dismissModal = { },
        primaryButtonClicked = { },
        secondaryButtonClicked = { },
    )

}