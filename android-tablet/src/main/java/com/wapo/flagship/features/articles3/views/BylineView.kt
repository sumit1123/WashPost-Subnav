package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.AuthorInfoUiModel
import com.wapo.flagship.features.articles3.models.ui.BylineUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles

@Composable
fun BylineView(
    uiModel: BylineUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val bylineStyle = getBylineStyle(uiModel.uiStyle)

    Column {
        if (uiModel.text.isNotEmpty()) {
            val annotatedText = buildAnnotatedString {
                val text = uiModel.text
                var currentIndex = 0

                // Make author names clickable
                uiModel.authors.forEach { author ->
                    val authorStart = text.indexOf(author.name, currentIndex)
                    if (authorStart >= 0) {
                        // Append text before author name
                        if (authorStart > currentIndex) {
                            append(text.substring(currentIndex, authorStart))
                        }
                        // Append author name with LinkAnnotation
                        if (author.id != null) {
                            pushLink(
                                LinkAnnotation.Clickable(
                                    tag = author.id,
                                    linkInteractionListener = { articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AuthorNameClickEvent(author.id)) }
                                )
                            )
                            withStyle(
                                getBylineStyle(uiModel.uiStyle).toSpanStyle().copy(
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(author.name.replace(" ", "\u202F"))
                            }
                            pop()
                        } else {
                            append(author.name.replace(" ", "\u202F"))
                        }
                        currentIndex = authorStart + author.name.length
                    }
                }
                // Append remaining text
                if (currentIndex < text.length) {
                    append(text.substring(currentIndex))
                }
            }

            Text(
                text = annotatedText,
                style = bylineStyle
            )
        }

        // Subtext
        val subtextContent = if (uiModel.uiStyle == BylineUiStyle.LIVE_REPORTER_INSIGHT) {
            uiModel.authors.firstOrNull()?.expertise
        } else {
            uiModel.subtext
        }

        if (!subtextContent.isNullOrEmpty()) {
            Text(
                text = subtextContent,
                style = getSubtextStyle(uiModel.uiStyle),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun getBylineStyle(uiStyle: BylineUiStyle): TextStyle {
    return when (uiStyle) {
        BylineUiStyle.LIVE_UPDATE,
        BylineUiStyle.LIVE_REPORTER_INSIGHT -> ArticleTextStyles.BYLINE_LIVE_UPDATE.style
        BylineUiStyle.DEFAULT -> ArticleTextStyles.BYLINE_DEFAULT.style
    }
}

@Composable
private fun getSubtextStyle(uiStyle: BylineUiStyle): TextStyle {
    return when (uiStyle) {
        BylineUiStyle.LIVE_UPDATE,
        BylineUiStyle.DEFAULT -> ArticleTextStyles.SUBTEXT_DEFAULT.style
        BylineUiStyle.LIVE_REPORTER_INSIGHT -> ArticleTextStyles.SUBTEXT_LIVE_REPORTER_INSIGHT.style
    }
}

enum class BylineUiStyle {
    LIVE_UPDATE,
    LIVE_REPORTER_INSIGHT,
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun BylineViewPreview() {
    AndroidClassicTheme {
        BylineView(
            uiModel = BylineUiModel(
                text = "By John Smith and Jane Doe",
                subtext = null,
                imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                bio = "John Smith is a reporter covering national security and foreign policy for The Washington Post.",
                authors = listOf(
                    AuthorInfoUiModel(id = "1", name = "John Smith", null, null, null),
                    AuthorInfoUiModel(id = "2", name = "Jane Doe", null, null, null)
                )
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BylineViewLiveUpdatePreview() {
    AndroidClassicTheme {
        BylineView(
            uiModel = BylineUiModel(
                text = "By John Smith",
                subtext = "Updated 5 minutes ago",
                imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                bio = "John Smith is a reporter covering national security and foreign policy for The Washington Post.",
                authors = listOf(
                    AuthorInfoUiModel(id = "1", name = "John Smith", null, null, null)
                ),
                uiStyle = BylineUiStyle.LIVE_UPDATE
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BylineViewLiveReporterInsightPreview() {
    AndroidClassicTheme {
        BylineView(
            uiModel = BylineUiModel(
                text = "By Jane Doe",
                subtext = null,
                authors = listOf(
                    AuthorInfoUiModel(
                        id = "1",
                        name = "Jane Doe",
                        bio = null,
                        expertise = "National Security Reporter",
                        null
                    )
                ),
                imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                bio = "John Smith is a reporter covering national security and foreign policy for The Washington Post.",
                uiStyle = BylineUiStyle.LIVE_REPORTER_INSIGHT
            ),
            dummyArticlesInteractionHelper
        )
    }
}