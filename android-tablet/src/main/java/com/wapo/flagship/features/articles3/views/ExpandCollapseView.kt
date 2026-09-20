package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.ExpandCollapseUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

/**
 * Reusable expand/collapse button component
 * Used by ElementGroupView and can be used by other components that need expand/collapse functionality
 */
@Composable
fun ExpandCollapseView(
    uiModel: ExpandCollapseUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
    itemToToggle: Item? = null,
    onToggleExpand: (() -> Unit)? = null
) {
    val label = if (uiModel.isExpanded) uiModel.expandedLabel else uiModel.truncatedLabel
    val icon = if (uiModel.isExpanded) {
        Icons.Filled.KeyboardArrowUp
    } else {
        Icons.Filled.KeyboardArrowDown
    }
    val style = getExpandCollapseStyle(uiModel.uiStyle)

    TextButton(
        onClick = {
            itemToToggle?.let {
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.ArticleCardExpandCollapse(it))
            }
            onToggleExpand?.invoke()
        },
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = style.textStyle
            )
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = style.iconTint
            )
        }
    }
}

@Composable
private fun getExpandCollapseStyle(uiStyle: ExpandCollapseUiStyle): ExpandCollapseStyleSet {
    return when (uiStyle) {
        ExpandCollapseUiStyle.DEFAULT -> ExpandCollapseStyleSet(
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = wpdsColors.primary
            ),
            iconTint = wpdsColors.primary
        )
        ExpandCollapseUiStyle.SECONDARY -> ExpandCollapseStyleSet(
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = wpdsColors.onSurface
            ),
            iconTint = wpdsColors.onSurface
        )
    }
}

private data class ExpandCollapseStyleSet(
    val textStyle: TextStyle,
    val iconTint: Color
)

enum class ExpandCollapseUiStyle {
    DEFAULT,
    SECONDARY
}

@Preview(showBackground = true)
@Composable
private fun ExpandCollapseViewCollapsedPreview() {
    AndroidClassicTheme {
        ExpandCollapseView(
            ExpandCollapseUiModel(
                isExpanded = false,
                expandedLabel = "Show less",
                truncatedLabel = "Show more",
                group = "123",
                minItemsCount = 3
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandCollapseViewExpandedPreview() {
    AndroidClassicTheme {
        ExpandCollapseView(
            ExpandCollapseUiModel(
                isExpanded = true,
                expandedLabel = "Show less",
                truncatedLabel = "Show more",
                group = "123",
                minItemsCount = 3
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandCollapseViewCustomLabelsPreview() {
    AndroidClassicTheme {
        ExpandCollapseView(
            ExpandCollapseUiModel(
                isExpanded = false,
                expandedLabel = "See less updates",
                truncatedLabel = "See more updates",
                group = "123",
                minItemsCount = 3,
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandCollapseViewSecondaryStylePreview() {
    AndroidClassicTheme {
        ExpandCollapseView(
            ExpandCollapseUiModel(
                isExpanded = false,
                expandedLabel = "Show less",
                truncatedLabel = "Show more",
                minItemsCount = 3,
                group = "123",
                uiStyle = ExpandCollapseUiStyle.SECONDARY
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}
