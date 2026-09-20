package com.wapo.flagship.features.articles3.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.ElementGroupUiModel
import com.wapo.flagship.features.articles3.models.ui.ExpandCollapseUiModel
import com.wapo.flagship.features.articles3.models.ui.SanitizedHtmlUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

@Composable
fun ElementGroupLinkBoxView(
    uiModel: ElementGroupUiModel.ElementGroupLinkBoxUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val isExpanded = uiModel.expandCollapseUiModel?.isExpanded ?: true
    val style = getElementGroupStyle(uiModel.uiStyle)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(4.dp),
            ambientColor = Color.Black,
            spotColor = Color.Black
        ),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = wpdsColors.secondary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Kicker
            uiModel.kicker?.let { kicker ->
                Text(
                    text = kicker,
                    style = style.kickerStyle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Title/Headline
            uiModel.title?.let { title ->
                Text(
                    text = title,
                    style = style.headlineStyle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Subheadline
            uiModel.subheadline?.let { subheadline ->
                Text(
                    text = subheadline,
                    style = style.subheadlineStyle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Date
            uiModel.displayDate?.let { date ->
                Text(
                    text = "Updated $date",
                    style = style.dateStyle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Top rule/divider
            if (uiModel.kicker != null || uiModel.title != null || uiModel.subheadline != null || uiModel.displayDate != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(wpdsColors.outline)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            val minItemsCount = uiModel.expandCollapseUiModel?.minItemsCount
            val itemsToShow = if (isExpanded || minItemsCount == null) {
                uiModel.contentElements
            } else {
                uiModel.contentElements.take(minItemsCount)
            }

            itemsToShow.forEach { element ->
                when (element) {
                    is SanitizedHtmlUiModel -> SanitizedHtmlView(element, articlesInteractionHelper)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            uiModel.expandCollapseUiModel?.let { expandCollapseUiModel ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ExpandCollapseView(expandCollapseUiModel.copy(isExpanded = isExpanded), articlesInteractionHelper) {
                        articlesInteractionHelper.onEventFired(
                            ArticleInteractionEvent.ElementGroupExpandCollapse(uiModel.expandCollapseUiModel.group)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getElementGroupStyle(uiStyle: ElementGroupUiStyle): ElementGroupStyleSet {
    return when (uiStyle) {
        ElementGroupUiStyle.LINK_BOX,
        ElementGroupUiStyle.DEFAULT -> ElementGroupStyleSet(
            kickerStyle = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = wpdsColors.primary,
                letterSpacing = 1.sp
            ),
            headlineStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = wpdsColors.onSurface,
                lineHeight = 24.sp
            ),
            subheadlineStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = wpdsColors.onSurface
            ),
            dateStyle = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = wpdsColors.gray40
            )
        )
    }
}

private data class ElementGroupStyleSet(
    val kickerStyle: TextStyle,
    val headlineStyle: TextStyle,
    val subheadlineStyle: TextStyle,
    val dateStyle: TextStyle
)

enum class ElementGroupUiStyle {
    LINK_BOX,
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun ElementGroupLinkBoxViewPreview() {
    AndroidClassicTheme {
        ElementGroupLinkBoxView(
            uiModel = ElementGroupUiModel.ElementGroupLinkBoxUiModel(
                "Follow along as the story develops",
                listOf(
                    SanitizedHtmlUiModel(
                        content = "First update: Officials have announced new measures.",
                        oEmbed = null,
                        questionSets = null,
                        sourceAnnotations = null,
                        uiStyle = SanitizedHtmlUiStyle.DEFAULT
                    ),
                    SanitizedHtmlUiModel(
                        content = "Second update: The situation continues to evolve.",
                        oEmbed = null,
                        questionSets = null,
                        sourceAnnotations = null,
                        uiStyle = SanitizedHtmlUiStyle.DEFAULT
                    ),
                    SanitizedHtmlUiModel(
                        content = "Third update: Additional details have emerged.",
                        oEmbed = null,
                        questionSets = null,
                        sourceAnnotations = null,
                        uiStyle = SanitizedHtmlUiStyle.DEFAULT
                    ),
                    SanitizedHtmlUiModel(
                        content = "Fourth update: Experts weigh in on the implications.",
                        oEmbed = null,
                        questionSets = null,
                        sourceAnnotations = null,
                        uiStyle = SanitizedHtmlUiStyle.DEFAULT
                    ),
                    SanitizedHtmlUiModel(
                        content = "Fifth update: Public reaction has been mixed.",
                        questionSets = null,
                        sourceAnnotations = null,
                        oEmbed = null,
                        uiStyle = SanitizedHtmlUiStyle.DEFAULT
                    )
                ),
                "link_box",
                "LIVE UPDATES",
                "Our reporters are tracking the latest developments",
                "March 10, 2026",
                ExpandCollapseUiModel(
                    isExpanded = false,
                    expandedLabel = "Show less content",
                    truncatedLabel = "Show more content",
                    group = "123",
                    minItemsCount = 3
                ),
                ElementGroupUiStyle.DEFAULT
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}
