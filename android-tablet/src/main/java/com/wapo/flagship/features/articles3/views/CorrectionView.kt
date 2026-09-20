package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.CorrectionUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors
import com.wapo.flagship.features.articles3.parseHtmlContent

@Composable
fun CorrectionView(
    uiModel: CorrectionUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(wpdsColors.correctionBackground)
            .padding(16.dp)
    ) {
        if (uiModel.correctionType != null) {
            Text(
                text = uiModel.correctionType.uppercase(),
                style = getCorrectionTitleStyle(uiModel.uiStyle),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Text(
            text = parseHtmlContent(uiModel.content, articlesInteractionHelper),
            style = getCorrectionBodyStyle(uiModel.uiStyle)
        )
    }
}

@Composable
private fun getCorrectionTitleStyle(uiStyle: CorrectionUiStyle): TextStyle {
    return when (uiStyle) {
        CorrectionUiStyle.DEFAULT -> ArticleTextStyles.CORRECTION_TITLE.style
    }
}

@Composable
private fun getCorrectionBodyStyle(uiStyle: CorrectionUiStyle): TextStyle {
    return when (uiStyle) {
        CorrectionUiStyle.DEFAULT -> ArticleTextStyles.CORRECTION_BODY.style
    }
}

enum class CorrectionUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun CorrectionViewPreview() {
    AndroidClassicTheme {
        CorrectionView(
            uiModel = CorrectionUiModel(
                correctionType = "Correction",
                content = "An earlier version of this article incorrectly stated the date of the event. The article has been <a href=\"https://washingtonpost.com\">corrected</a>."
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CorrectionViewClarificationPreview() {
    AndroidClassicTheme {
        CorrectionView(
            uiModel = CorrectionUiModel(
                correctionType = "Clarification",
                content = "This article has been updated to <b>clarify</b> the role of the <i>individual mentioned</i>."
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CorrectionViewNoTypePreview() {
    AndroidClassicTheme {
        CorrectionView(
            uiModel = CorrectionUiModel(
                correctionType = null,
                content = "This article has been updated."
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}


