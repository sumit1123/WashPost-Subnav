package com.wapo.flagship.features.articles3.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.wapo.flagship.features.articles3.models.ui.DeckUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles

@Composable
fun DeckView(uiModel: DeckUiModel) {
    if (uiModel.content.isEmpty()) {
        return
    }

    Text(
        text = uiModel.content,
        style = getDeckStyle(uiModel.uiStyle)
    )
}

@Composable
private fun getDeckStyle(uiStyle: DeckUiStyle): TextStyle {
    return when (uiStyle) {
        DeckUiStyle.DEFAULT -> ArticleTextStyles.DECK.style
    }
}

enum class DeckUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun DeckViewPreview() {
    AndroidClassicTheme {
        DeckView(
            uiModel = DeckUiModel(
                content = "A subheadline that provides additional context about the article's main story"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeckViewLongPreview() {
    AndroidClassicTheme {
        DeckView(
            uiModel = DeckUiModel(
                content = "This is a longer deck text that might span multiple lines to demonstrate how the view handles wrapping and line height in the article layout"
            )
        )
    }
}

