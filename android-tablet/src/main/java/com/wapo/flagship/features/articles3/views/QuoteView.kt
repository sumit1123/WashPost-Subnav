package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.MimeType
import com.wapo.flagship.features.articles3.models.ui.QuoteUiModel
import com.wapo.flagship.features.articles3.parseContent
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles

enum class QuoteUiStyle {
    DEFAULT
}

@Composable
fun QuoteView(uiModel: QuoteUiModel, articlesInteractionHelper: ArticlesInteractionHelper) {
    if (uiModel.content.isNullOrEmpty() && uiModel.attribution.isNullOrEmpty()) return

    Column(
        modifier = Modifier.padding(start = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!uiModel.content.isNullOrEmpty()) {
            Text(
                text = parseContent(
                    content = uiModel.content,
                    mimeType = uiModel.mime ?: MimeType.HTML,
                    articlesInteractionHelper = articlesInteractionHelper,
                ),
                style = ArticleTextStyles.QUOTE_TEXT.style
            )
        }
        if (!uiModel.attribution.isNullOrEmpty()) {
            Text(
                text = parseContent(
                    content = uiModel.attribution,
                    mimeType = uiModel.mime ?: MimeType.HTML,
                    articlesInteractionHelper = articlesInteractionHelper,
                ),
                style = ArticleTextStyles.QUOTE_CAPTION.style
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun QuoteViewFullPreview() {
    AndroidClassicTheme {
        QuoteView(
            uiModel = QuoteUiModel(
                content = "The truth is rarely pure and never simple.",
                attribution = "Oscar Wilde",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper
        )
    }
}