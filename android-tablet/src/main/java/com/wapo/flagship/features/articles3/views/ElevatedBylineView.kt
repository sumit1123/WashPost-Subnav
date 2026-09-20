package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.AuthorInfoUiModel
import com.wapo.flagship.features.articles3.models.ui.ElevatedBylineUiModel
import com.wapo.flagship.features.articles3.models.ui.KickerUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.wpdsColors

enum class ElevatedBylineUiStyle {
    DEFAULT,
    OPINIONS
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ElevatedBylineView(
    uiModel: ElevatedBylineUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val bylineStyle = getElevatedBylineStyle(uiModel.uiStyle)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Author images - side by side with 8dp space
        Row(
            verticalAlignment = Alignment.Top
        ) {
            // First image (larger)
            if (uiModel.authors.isNotEmpty() && !uiModel.authors[0].imageUrl.isNullOrEmpty()) {
                GlideImage(
                    model = uiModel.authors[0].imageUrl,
                    contentDescription = "Author image for ${uiModel.authors[0].name}",
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(color = wpdsColors.gray200),
                    contentScale = ContentScale.Crop
                )
            }

            // Space between images
            if (uiModel.authors.size > 1 && !uiModel.authors[1].imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))

                // Second image (smaller)
                GlideImage(
                    model = uiModel.authors[1].imageUrl,
                    contentDescription = "Author image for ${uiModel.authors[1].name}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color = wpdsColors.gray200),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text content (kicker and byline)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Kicker
            if (uiModel.kicker.isNotEmpty()) {
                if (uiModel.uiStyle == ElevatedBylineUiStyle.OPINIONS) {
                    OpinionsKickerText(
                        KickerUiModel(
                            displayLabel = uiModel.kicker,
                            displayTransparency = null,
                            isLive = false,
                            path = "/opinions",
                            uiStyle = KickerUiStyle.OPINIONS,
                            alignment = null,
                            image = null
                        )
                    )
                } else {
                    Text(
                        text = uiModel.kicker,
                        style = getKickerStyle(uiModel.uiStyle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
                // Byline with clickable authors
                if (uiModel.byline.isNotEmpty()) {
                    val opinionsStyle = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = PostiniFontFamily,
                        lineHeight = 20.sp,
                        color = wpdsColors.gray20
                    )
                    val currentBylineStyle = if (uiModel.uiStyle == ElevatedBylineUiStyle.OPINIONS) opinionsStyle else bylineStyle
                    val annotatedText = buildAnnotatedString {
                        val text = uiModel.byline
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
                                pushLink(
                                    LinkAnnotation.Clickable(
                                        tag = author.id,
                                        linkInteractionListener = {
                                            articlesInteractionHelper.onEventFired(
                                                ArticleInteractionEvent.AuthorNameClickEvent(author.id)
                                            )
                                        }
                                    )
                                )
                                withStyle(
                                    currentBylineStyle.toSpanStyle().copy(
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append(author.name.replace(" ", "\u202F"))
                                }
                                pop()
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
                        style = currentBylineStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (uiModel.uiStyle == ElevatedBylineUiStyle.OPINIONS) 12.dp else 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getElevatedBylineStyle(uiStyle: ElevatedBylineUiStyle): TextStyle {
    return when (uiStyle) {
        ElevatedBylineUiStyle.DEFAULT -> ArticleTextStyles.BYLINE_DEFAULT.style
        ElevatedBylineUiStyle.OPINIONS -> ArticleTextStyles.BYLINE_DEFAULT.style
    }
}

@Composable
private fun getKickerStyle(uiStyle: ElevatedBylineUiStyle): TextStyle {
    return when (uiStyle) {
        ElevatedBylineUiStyle.DEFAULT -> ArticleTextStyles.SUBTEXT_DEFAULT.style
        ElevatedBylineUiStyle.OPINIONS -> ArticleTextStyles.SUBTEXT_DEFAULT.style
    }
}

@Preview(showBackground = true)
@Composable
private fun ElevatedBylineViewPreview() {
    AndroidClassicTheme {
        Surface(
            color = wpdsColors.secondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            ElevatedBylineView(
                uiModel = ElevatedBylineUiModel(
                    kicker = "Opinion",
                    byline = "Opinion by John Smith and Jane Doe",
                    authors = listOf(
                        AuthorInfoUiModel(
                            id = "1",
                            name = "John Smith",
                            bio = null,
                            expertise = null,
                            imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png"
                        )
                    ),
                    uiStyle = ElevatedBylineUiStyle.OPINIONS
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ElevatedBylineViewDefaultPreview() {
    AndroidClassicTheme {
        Surface(
            color = wpdsColors.secondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            ElevatedBylineView(
                uiModel = ElevatedBylineUiModel(
                    kicker = "Featured",
                    byline = "By Sarah Johnson",
                    authors = listOf(
                        AuthorInfoUiModel(
                            id = "3",
                            name = "Sarah Johnson",
                            bio = null,
                            expertise = null,
                            imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png"
                        ),
                        AuthorInfoUiModel(
                            id = "2",
                            name = "Jane Doe",
                            bio = null,
                            expertise = null,
                            imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png"
                        )
                    ),
                    uiStyle = ElevatedBylineUiStyle.DEFAULT
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

