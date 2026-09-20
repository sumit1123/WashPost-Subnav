package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.articles3.models.ui.LiveOutcomeUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors

enum class LiveOutcomeUiStyle {
    DEFAULT,
    LIVE_UPDATE
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LiveOutcomeView(uiModel: LiveOutcomeUiModel) {
    val hasSubHeadline = !uiModel.subHeadline.isNullOrEmpty()
    val hasImage = !uiModel.imageUrl.isNullOrEmpty()
    val isLiveUpdate = uiModel.uiStyle == LiveOutcomeUiStyle.LIVE_UPDATE

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (hasSubHeadline) Alignment.Top else Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isLiveUpdate) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(wpdsColors.liveUpdateTextColor, CircleShape)
                    )
                    Text(
                        text = "LIVE",
                        style = ArticleTextStyles.DATELINE_LIVE_UPDATE.style,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
            }
            Text(
                text = uiModel.headline.orEmpty(),
                style = getHeadlineStyle(uiModel.uiStyle),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (hasSubHeadline) {
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = uiModel.subHeadline,
                    style = getSubHeadlineStyle(uiModel.uiStyle)
                )
            }
        }
        if (hasImage) {
            Spacer(modifier = Modifier.width(8.dp))
            GlideImage(
                model = uiModel.imageUrl,
                contentDescription = uiModel.headline,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(4.5.dp))
                    .background(wpdsColors.gray500),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun getHeadlineStyle(uiStyle: LiveOutcomeUiStyle): TextStyle {
    return when (uiStyle) {
        LiveOutcomeUiStyle.DEFAULT -> ArticleTextStyles.LIVE_OUTCOME_HEADLINE.style
        LiveOutcomeUiStyle.LIVE_UPDATE -> ArticleTextStyles.LIVE_OUTCOME_HEADLINE.style.copy(
            color = wpdsColors.liveUpdateTextColor
        )
    }
}

@Composable
private fun getSubHeadlineStyle(uiStyle: LiveOutcomeUiStyle): TextStyle {
    return when (uiStyle) {
        LiveOutcomeUiStyle.DEFAULT -> ArticleTextStyles.LIVE_OUTCOME_SUBHEADLINE.style
        LiveOutcomeUiStyle.LIVE_UPDATE -> ArticleTextStyles.LIVE_OUTCOME_SUBHEADLINE.style.copy(
            color = wpdsColors.liveUpdateTextColor
        )
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun LiveOutcomeViewFullPreview() {
    AndroidClassicTheme {
        LiveOutcomeView(
            uiModel = LiveOutcomeUiModel(
                headline = "Biden leads Trump in electoral votes as key swing states are called",
                subHeadline = "270 needed to win",
                imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOutcomeViewNoSubHeadlinePreview() {
    AndroidClassicTheme {
        LiveOutcomeView(
            uiModel = LiveOutcomeUiModel(
                headline = "Biden leads Trump in electoral votes",
                subHeadline = null,
                imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOutcomeViewNoImagePreview() {
    AndroidClassicTheme {
        LiveOutcomeView(
            uiModel = LiveOutcomeUiModel(
                headline = "Biden leads Trump in electoral votes as key swing states are called",
                subHeadline = "270 needed to win",
                imageUrl = ""
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOutcomeViewLiveUpdatePreview() {
    AndroidClassicTheme {
        LiveOutcomeView(
            uiModel = LiveOutcomeUiModel(
                headline = "Senate hearing underway as committee debates final amendments",
                subHeadline = "Updates posted in real time",
                uiStyle = LiveOutcomeUiStyle.LIVE_UPDATE,
                imageUrl = ""
            )
        )
    }
}

// endregion

