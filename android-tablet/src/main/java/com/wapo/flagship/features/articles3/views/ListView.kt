package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.ListUiModel
import com.wapo.flagship.features.articles3.parseHtmlContent
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors

enum class ListUiStyle {
    DEFAULT,
    BRIEFS,
    OPINIONS,
    BLOCKQUOTE
}

enum class ListType {
    ORDERED,
    UNORDERED
}

@Composable
fun ListView(uiModel: ListUiModel, articlesInteractionHelper: ArticlesInteractionHelper) {
    if (uiModel.items.isEmpty()) return

    val style = getListStyle(uiModel.uiStyle)

    when (uiModel.uiStyle) {
        ListUiStyle.BLOCKQUOTE -> {
            val lineColor = wpdsColors.primary
            Column(
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(start = 23.5.dp)
            ) {
                ListItems(uiModel, style, articlesInteractionHelper)
            }
        }
        else -> {
            Column {
                ListItems(uiModel, style, articlesInteractionHelper)
            }
        }
    }
}

@Composable
private fun ListItems(uiModel: ListUiModel, style: TextStyle, articlesInteractionHelper: ArticlesInteractionHelper) {
    for (index in uiModel.items.indices) {
        val item = uiModel.items[index]
        val prefix = when (uiModel.listType) {
            ListType.ORDERED -> "${index + 1}."
            ListType.UNORDERED -> "\u2022"
        }
        Row {
            Text(
                text = prefix,
                style = style,
                modifier = Modifier.width(
                    if (uiModel.listType == ListType.ORDERED) 24.dp else 16.dp
                )
            )
            Text(
                text = parseHtmlContent(item, articlesInteractionHelper),
                style = style,
                modifier = Modifier.weight(1f)
            )
        }
        if (index < uiModel.items.lastIndex) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun getListStyle(uiStyle: ListUiStyle): TextStyle {
    return when (uiStyle) {
        ListUiStyle.BRIEFS -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD.style
        ListUiStyle.OPINIONS -> ArticleTextStyles.SANITIZED_HTML_OPINIONS.style
        ListUiStyle.BLOCKQUOTE -> ArticleTextStyles.SANITIZED_HTML_BLOCKQUOTE.style
        ListUiStyle.DEFAULT -> ArticleTextStyles.SANITIZED_HTML_DEFAULT.style
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun ListViewUnorderedPreview() {
    AndroidClassicTheme {
        ListView(
            uiModel = ListUiModel(
                items = listOf(
                    "First unordered item in the list",
                    "Second unordered item with more text to show wrapping",
                    "Third unordered item"
                ),
                listType = ListType.UNORDERED
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListViewOrderedPreview() {
    AndroidClassicTheme {
        ListView(
            uiModel = ListUiModel(
                items = listOf(
                    "<b>First</b> ordered item in the <i>list</i>",
                    "Second ordered item with more text to show wrapping",
                    "Third ordered item"
                ),
                listType = ListType.ORDERED
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListViewBlockquotePreview() {
    AndroidClassicTheme {
        ListView(
            uiModel = ListUiModel(
                items = listOf(
                    "A blockquote list item",
                    "Another blockquote list item"
                ),
                listType = ListType.UNORDERED,
                uiStyle = ListUiStyle.BLOCKQUOTE
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListViewBriefsPreview() {
    AndroidClassicTheme {
        ListView(
            uiModel = ListUiModel(
                items = listOf(
                    "A briefs list item",
                    "Another briefs list item"
                ),
                listType = ListType.UNORDERED,
                uiStyle = ListUiStyle.BRIEFS
            ),
            dummyArticlesInteractionHelper
        )
    }
}

// endregion


