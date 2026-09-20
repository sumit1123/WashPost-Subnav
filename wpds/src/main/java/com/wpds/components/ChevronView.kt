package com.wpds.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wpds.theme.wpdsColors
import com.wpds.wpds.R
import com.wpds.wptheme.WpTheme

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChevronView(
    modifier: Modifier = Modifier,
    headlineText: String,
    subtitle: String,
    iconUrl: String,
    darkIconUrl: String?,
    showButton: Boolean,
    buttonText: String?,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(enabled = onClick != null, onClick = {
                onClick?.invoke()
            }),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = wpdsColors.mediaPlayerBackground,
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val selectedIconUrl =
                if (isSystemInDarkTheme() && darkIconUrl != null) darkIconUrl else iconUrl
            GlideImage(
                model = selectedIconUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .height(60.dp)
                        .width(60.dp)
                        .align(Alignment.CenterVertically),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f)
                    .padding(start = 8.dp, end = 24.dp)
            ) {
                WpTheme.Text.AnnotatedHeadline(modifier = Modifier, text = headlineText)
                WpTheme.Text.Subtitle(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    text = subtitle
                )
            }

            if (showButton && buttonText != null) {
                Button(
                    modifier = Modifier.wrapContentWidth()
                        .align(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = wpdsColors.myPostBannerButtonBackground,
                    ),
                    onClick = { onClick?.invoke() },
                    shape = RoundedCornerShape(8.dp),
                    content = {
                        WpTheme.Text.WapoText(modifier = Modifier, text = buttonText, textSize = 12)
                    })
            } else {
                Icon(
                    painter = painterResource(R.drawable.chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                        .align(Alignment.CenterVertically)
                        .wrapContentSize(),
                    tint = wpdsColors.gray80

                )
            }
        }
    }
}

@Composable
@Preview
fun ChevronViewPreview() {
    ChevronView(
        modifier = Modifier,
        headlineText = "Your Premium Benefits",
        subtitle = "Gift Articles, Archive access, discounts, and more!",
        iconUrl = "",
        darkIconUrl = "",
        showButton = false,
        buttonText = "Button Text",
        onClick = null
    )
}
