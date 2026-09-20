package com.wapo.flagship.features.articles3.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.InterstitialLinkUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors

@Composable
fun InterstitialLinkView(
    uiModel: InterstitialLinkUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper = dummyArticlesInteractionHelper
) {
    if (uiModel.content.isEmpty()) {
        return
    }

    Text(
        text = buildInterstitialLinkText(uiModel, articlesInteractionHelper),
        style = getInterstitialLinkStyle(uiModel.uiStyle)
    )
}

@Composable
private fun buildInterstitialLinkText(
    uiModel: InterstitialLinkUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
): AnnotatedString {
    return buildAnnotatedString {
        val linkAnnotation = LinkAnnotation.Clickable(
            tag = "interstitial_link",
            linkInteractionListener = {
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.LinkClickEvent(uiModel.url))
            }
        )
        withLink(linkAnnotation) {
            pushStyle(
                SpanStyle(
                    color = wpdsColors.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
            append(uiModel.content)
            pop()
        }
    }
}

@Composable
private fun getInterstitialLinkStyle(uiStyle: InterstitialLinkUiStyle): TextStyle {
    return when (uiStyle) {
        InterstitialLinkUiStyle.DEFAULT -> ArticleTextStyles.SANITIZED_HTML_DEFAULT.style.copy(
            fontStyle = FontStyle.Italic
        )
    }
}

enum class InterstitialLinkUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun InterstitialLinkViewPreview() {
    AndroidClassicTheme {
        InterstitialLinkView(
            uiModel = InterstitialLinkUiModel(
                content = "Related: Read the full report here",
                url = "https://www.washingtonpost.com/example"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InterstitialLinkViewNoUrlPreview() {
    AndroidClassicTheme {
        InterstitialLinkView(
            uiModel = InterstitialLinkUiModel(
                content = "This is just italic text without a link",
                url = "https://www.washingtonpost.com/example"
            )
        )
    }
}
