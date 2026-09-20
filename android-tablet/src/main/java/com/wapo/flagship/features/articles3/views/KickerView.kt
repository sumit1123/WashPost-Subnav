package com.wapo.flagship.features.articles3.views

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.KickerImage
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper
import com.wapo.flagship.features.articles3.models.ui.KickerUiModel
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.applyUnderline
import com.washingtonpost.android.R
import com.washingtonpost.android.sections.R.drawable
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors

enum class KickerUiStyle {
    DEFAULT,
    BRIEFS,
    THE_SEVEN_LIVE,
    OPINIONS,
    PILL_LIVE,
    PILL_EXCLUSIVE,
    IMAGE
}

@Composable
fun KickerView(
    uiModel: KickerUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            if (uiModel.image == null) {
                uiModel.path?.let {
                    articlesInteractionHelper.onEventFired(ArticleInteractionEvent.KickerClickEvent(it))
                }
            }
        }
    ) {
        if (uiModel.isLive) {
            PulsingRedDot()
            Spacer(modifier = Modifier.width(7.dp))
        }
        when (uiModel.uiStyle) {
            KickerUiStyle.PILL_LIVE -> KickerPillLive()
            KickerUiStyle.PILL_EXCLUSIVE -> KickerPillExclusive()
            KickerUiStyle.BRIEFS, KickerUiStyle.THE_SEVEN_LIVE -> KickerBriefs(uiModel)
            KickerUiStyle.IMAGE -> KickerImageView(uiModel)
            else -> KickerText(uiModel)
        }
    }
}

@Composable
private fun PulsingRedDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "redDotPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redDotAlpha"
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Color(0xffCF000E))
    )
}

@Composable
private fun KickerPillLive() {
    Text(
        text = stringResource(com.washingtonpost.android.articles.R.string.kicker_live_updates),
        style = ArticleTextStyles.KICKER_PILL_LIVE.style,
        modifier = Modifier
            .background(
                color = wpdsColors.liveUpdateTextColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun KickerPillExclusive() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = wpdsColors.primary,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.wp_logo_icon),
            contentDescription = "WP logo",
            tint = wpdsColors.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(com.washingtonpost.android.articles.R.string.kicker_exclusive),
            style = ArticleTextStyles.KICKER_PILL_EXCLUSIVE.style,
        )
    }
}

@Composable
private fun KickerBriefs(uiModel: KickerUiModel) {
    val displayLabel = uiModel.displayLabel
    if (displayLabel.isEmpty()) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_label_briefs),
            contentDescription = "The 7 logo",
            tint = Color.Unspecified,
            modifier = Modifier.size(25.dp, 24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = displayLabel,
            style = ArticleTextStyles.KICKER_BRIEFS.style
        )

        //  Add a live badge if style is the-7-live
        if (uiModel.uiStyle == KickerUiStyle.THE_SEVEN_LIVE) {
            Spacer(modifier = Modifier.width(5.dp))
            Image(
                painter = painterResource(drawable.the_seven_live_chip),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun KickerImageView(uiModel: KickerUiModel) {
    val arrangement = when(uiModel.alignment) {
        "left" -> Arrangement.Start
        "center" -> Arrangement.Center
        "right" -> Arrangement.End
        else -> Arrangement.Start
    }
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically) {
        val image = when(uiModel.image?.imageId) {
            "ripple" -> {
                if (isSystemInDarkTheme()) drawable.ripple_logo_dark else drawable.ripple_logo
            }
            else -> null
        }
        image?.let {
            Image(
                painter = painterResource(image),
                contentDescription = uiModel.image?.imageId,
            )
        }
    }
}

@Composable
private fun KickerText(uiModel: KickerUiModel) {
    val displayLabel = uiModel.displayLabel
    if (displayLabel.isEmpty() && uiModel.displayTransparency.isNullOrEmpty()) return

    if (uiModel.uiStyle == KickerUiStyle.OPINIONS) {
        OpinionsKickerText(uiModel)
        return
    }

    val kickerStyle = getKickerStyle(uiModel.uiStyle)
    val transparencyStyle = ArticleTextStyles.KICKER_DISPLAY_TRANSPARENCY.style

    val text = buildAnnotatedString {
        if (displayLabel.isNotEmpty()) {
            withStyle(kickerStyle.toSpanStyle()) {
                append(displayLabel)
            }
        }
        if (!uiModel.displayTransparency.isNullOrEmpty()) {
            if (length > 0) {
                withStyle(transparencyStyle.toSpanStyle()) {
                    append("  \u2022  ")
                }
            }
            withStyle(transparencyStyle.toSpanStyle()) {
                append(uiModel.displayTransparency)
            }
        }
    }

    Text(text = text)
}

@Composable
fun OpinionsKickerText(uiModel: KickerUiModel) {
    AndroidView(
        factory = { context ->
            AppCompatTextView(context).apply {
                includeFontPadding = false
                text = buildOpinionsKickerSpannable(context, uiModel)
            }
        },
        update = { textView ->
            textView.text = buildOpinionsKickerSpannable(textView.context, uiModel)
        }
    )
}

private fun buildOpinionsKickerSpannable(
    context: android.content.Context,
    uiModel: KickerUiModel
): SpannableStringBuilder {
    val spannableKicker = SpannableStringBuilder()
    val displayLabel = uiModel.displayLabel

    if (displayLabel.isNotEmpty()) {
        spannableKicker.append(displayLabel)
        spannableKicker.setSpan(
            WpTextAppearanceSpan(
                context,
                KickerStyleHelper.getTextKickerDefaultStyle(context)
            ),
            0,
            displayLabel.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Keep legacy OPINIONS underline behavior from the View-based implementation.
        spannableKicker.applyUnderline(
            context,
            0,
            minOf(1, displayLabel.length),
            com.wpds.wpds.R.color.opinion_spark,
            context.resources.getInteger(com.wapo.view.R.integer.first_part_opinion_left_padding_underline).toFloat(),
            context.resources.getInteger(com.wapo.view.R.integer.first_part_opinion_right_padding_underline).toFloat(),
            -4f
        )
        if (displayLabel.length > 2) {
            spannableKicker.applyUnderline(
                context,
                2,
                displayLabel.length,
                com.wpds.wpds.R.color.opinion_spark,
                context.resources.getInteger(com.wapo.view.R.integer.second_part_opinion_left_padding_underline).toFloat(),
                context.resources.getInteger(com.wapo.view.R.integer.second_part_opinion_right_padding_underline).toFloat(),
                -4f
            )
        }
    }

    if (!uiModel.displayTransparency.isNullOrEmpty()) {
        val startIndex = spannableKicker.length
        if (startIndex > 0) {
            spannableKicker.append("  \u2022  ")
        }
        spannableKicker.append(uiModel.displayTransparency)
        spannableKicker.setSpan(
            WpTextAppearanceSpan(
                context,
                KickerStyleHelper.getDisplayTransparencyStyle(context)
            ),
            startIndex,
            spannableKicker.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    return spannableKicker
}

@Composable
private fun getKickerStyle(uiStyle: KickerUiStyle): TextStyle {
    return when (uiStyle) {
        KickerUiStyle.BRIEFS, KickerUiStyle.THE_SEVEN_LIVE -> ArticleTextStyles.KICKER_BRIEFS.style
        KickerUiStyle.OPINIONS -> ArticleTextStyles.KICKER_DEFAULT.style
        else -> ArticleTextStyles.KICKER_DEFAULT.style
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun KickerViewDefaultPreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "National Security",
                displayTransparency = null,
                uiStyle = KickerUiStyle.DEFAULT,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewWithTransparencyPreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "National Security",
                displayTransparency = "Analysis",
                uiStyle = KickerUiStyle.DEFAULT,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewBriefsPreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "The 7",
                displayTransparency = null,
                uiStyle = KickerUiStyle.BRIEFS,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewTheSevenLivePreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "The 7",
                displayTransparency = null,
                uiStyle = KickerUiStyle.THE_SEVEN_LIVE,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewOpinionsPreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "Opinions",
                displayTransparency = null,
                uiStyle = KickerUiStyle.OPINIONS,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewPillLivePreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "Live Updates",
                displayTransparency = null,
                isLive = true,
                path = "https://www.washingtonpost.com/opinions",
                uiStyle = KickerUiStyle.PILL_LIVE,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewPillExclusivePreview() {
    AndroidClassicTheme {
        KickerView(
            uiModel = KickerUiModel(
                displayLabel = "Exclusive",
                displayTransparency = null,
                uiStyle = KickerUiStyle.PILL_EXCLUSIVE,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "left",
                image = KickerImage(""),
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KickerViewImageViewPreview() {
    AndroidClassicTheme {
        KickerImageView(
            uiModel = KickerUiModel(
                displayLabel = "Exclusive",
                displayTransparency = null,
                uiStyle = KickerUiStyle.PILL_EXCLUSIVE,
                path = "https://www.washingtonpost.com/opinions",
                isLive = false,
                alignment = "center",
                image = KickerImage("ripple"),
            )
        )
    }
}

// endregion
