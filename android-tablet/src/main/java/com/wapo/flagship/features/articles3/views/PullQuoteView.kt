package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.MimeType
import com.wapo.flagship.features.articles3.models.ui.PullQuoteUiModel
import com.wapo.flagship.features.articles3.parseContent
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors

enum class PullQuoteUiStyle {
    DEFAULT
}

@Composable
fun PullQuoteView(uiModel: PullQuoteUiModel, articlesInteractionHelper: ArticlesInteractionHelper) {
    if (uiModel.content.isNullOrEmpty() && uiModel.attribution.isNullOrEmpty()) return

    val ruleColor = wpdsColors.gray0

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = ruleColor
        )
        Column(modifier = Modifier.padding(start = 24.dp)) {
            if (!uiModel.content.isNullOrEmpty()) {
                Text(
                    text = parseContent(
                        content = uiModel.content,
                        mimeType = uiModel.mime ?: MimeType.HTML,
                        articlesInteractionHelper = articlesInteractionHelper
                    ),
                    style = ArticleTextStyles.PULL_QUOTE_TEXT.style
                )
            }
            if (!uiModel.attribution.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = parseContent(
                        content = "\u2014 ${uiModel.attribution}",
                        mimeType = uiModel.mime ?: MimeType.HTML,
                        articlesInteractionHelper = articlesInteractionHelper
                    ),
                    style = ArticleTextStyles.PULL_QUOTE_CAPTION.style
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun QuoteViewFullPreview() {
    AndroidClassicTheme {
        PullQuoteView(
            uiModel = PullQuoteUiModel(
                content = "The truth is rarely pure and never simple.",
                attribution = "Oscar Wilde",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PullQuoteViewNoAttributionPreview() {
    AndroidClassicTheme {
        PullQuoteView(
            uiModel = PullQuoteUiModel(
                content = "We must accept finite disappointment, but never lose infinite hope.",
                attribution = "",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PullQuoteViewLongPreview() {
    AndroidClassicTheme {
        PullQuoteView(
            uiModel = PullQuoteUiModel(
                content = "This is a longer pull quote that demonstrates how the view handles multi-line text with proper line height and spacing in the article layout.",
                attribution = "A senior official, speaking on condition of anonymity",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper
        )
    }
}

// endregion

