package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.BlockQuoteAttributionUiModel
import com.wapo.flagship.features.articles3.models.ui.BlockQuoteUiModel
import com.wapo.flagship.features.articles3.models.ui.ElementGroupUiModel
import com.wapo.flagship.features.articles3.models.ui.MimeType
import com.wapo.flagship.features.articles3.models.ui.SanitizedHtmlUiModel
import com.wapo.flagship.features.articles3.parseContent
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors
import com.wpds.utils.isTabletUi

enum class BlockQuoteUiStyle {
    DEFAULT
}

@Composable
fun BlockQuoteView(
    uiModel: BlockQuoteUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    BlockQuoteView(
        content = listOf(
            SanitizedHtmlUiModel(
                content = uiModel.content,
                mime = uiModel.mime,
                questionSets = null,
                sourceAnnotations = null,
                oEmbed = null,
            )
        ),
        attribution = if (uiModel.attribution.isNotEmpty()) {
            BlockQuoteAttributionUiModel(
                content = uiModel.attribution,
                mime = uiModel.mime,
            )
        } else {
            null
        },
        articlesInteractionHelper = articlesInteractionHelper,
    )
}

@Composable
fun BlockQuoteView(
    uiModel: ElementGroupUiModel.ElementGroupBlockQuoteUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    BlockQuoteView(
        content = uiModel.contentElements,
        attribution = uiModel.attribution,
        articlesInteractionHelper = articlesInteractionHelper,
    )
}

@Composable
fun BlockQuoteView(
    content: List<SanitizedHtmlUiModel>,
    attribution: BlockQuoteAttributionUiModel?,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    if ((content.isEmpty() || content.all { it.content.isEmpty() }) && attribution?.content.isNullOrEmpty()) {
        return
    }
    val isTablet = isTabletUi()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(wpdsColors.blockQuoteBackground)
            .padding(if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 14.dp),
    ) {
        content
            .filterNot { it.content.isEmpty() }
            .forEach { element ->
                Text(
                    text = parseContent(
                        content = element.content,
                        mimeType = element.mime ?: MimeType.HTML,
                        articlesInteractionHelper = articlesInteractionHelper,
                    ),
                    style = ArticleTextStyles.BLOCK_QUOTE_TEXT.style
                )
            }

        if (!attribution?.content.isNullOrEmpty()) {
            Text(
                text = parseContent(
                    content = "\u2014 ${attribution.content}",
                    mimeType = attribution.mime ?: MimeType.HTML,
                    articlesInteractionHelper = articlesInteractionHelper,
                ),
                style = ArticleTextStyles.BLOCK_QUOTE_CAPTION.style
            )
        }
    }
}

// region Previews

@PreviewLightDark
@Composable
private fun BlockQuoteViewFullPreview() {
    AndroidClassicTheme {
        BlockQuoteView(
            uiModel = BlockQuoteUiModel(
                content = "The truth is rarely pure and never simple.",
                attribution = "Oscar Wilde",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@PreviewLightDark
@Composable
private fun BlockQuoteViewNoAttributionPreview() {
    AndroidClassicTheme {
        BlockQuoteView(
            uiModel = BlockQuoteUiModel(
                content = "We must accept finite disappointment, but never lose infinite hope.",
                attribution = "",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@PreviewLightDark
@Composable
private fun BlockQuoteViewLongPreview() {
    AndroidClassicTheme {
        BlockQuoteView(
            uiModel = BlockQuoteUiModel(
                content = "This is a longer block quote that demonstrates how the view handles multi-line text with proper line height and spacing in the article layout.",
                attribution = "A senior official, speaking on condition of anonymity",
                mime = MimeType.HTML,
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@PreviewLightDark
@Composable
private fun BlockQuoteElementGroupPreview() {
    AndroidClassicTheme {
        BlockQuoteView(
            ElementGroupUiModel.ElementGroupBlockQuoteUiModel(
                contentElements = listOf(
                    SanitizedHtmlUiModel(
                        content = "<i>Dear Mr. Pelley:</i><br /><i>I meant what I said in my letter last week to the 60 Minutes team.</i>",
                        questionSets = null,
                        sourceAnnotations = null,
                        oEmbed = null,
                        uiStyle = SanitizedHtmlUiStyle.PARAGRAPH
                    ),
                    SanitizedHtmlUiModel(
                        content = "This is a second paragraph with <b>bold text</b>, <i>italic text</i>, and a <a href=\"https://www.washingtonpost.com/\">Washington Post link</a>.",
                        questionSets = null,
                        sourceAnnotations = null,
                        oEmbed = null,
                        uiStyle = SanitizedHtmlUiStyle.PARAGRAPH
                    ),
                    SanitizedHtmlUiModel(
                        content = "This third paragraph omits subtype because paragraph is the default. It is deliberately long enough to wrap across several lines on a phone and exercise the box width, horizontal text inset, Dynamic Type behavior, and a grouped quote that extends beyond a compact viewport.",
                        questionSets = null,
                        sourceAnnotations = null,
                        oEmbed = null,
                        uiStyle = SanitizedHtmlUiStyle.PARAGRAPH
                    )
                ),
                attribution = BlockQuoteAttributionUiModel(
                    content = "<b>Nick Bilton</b>, executive producer",
                    mime = MimeType.HTML,
                ),
                uiStyle = ElementGroupUiStyle.DEFAULT,
            ),
            dummyArticlesInteractionHelper,
        )
    }
}

// endregion

