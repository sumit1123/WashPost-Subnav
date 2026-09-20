package com.wapo.flagship.features.articles3.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.wapo.flagship.features.articles3.models.ui.TitleUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles

@Composable
fun TitleView(uiModel: TitleUiModel) {
    Text(
        text = uiModel.text,
        style = getTitleStyle(uiModel.uiStyle)
    )
}

@Composable
private fun getTitleStyle(uiStyle: TitleUiStyle): TextStyle {
    return when (uiStyle) {
        TitleUiStyle.H1 -> ArticleTextStyles.TITLE_H1.style
        TitleUiStyle.H2 -> ArticleTextStyles.TITLE_H2.style
        TitleUiStyle.H3 -> ArticleTextStyles.TITLE_H3.style
        TitleUiStyle.H4 -> ArticleTextStyles.TITLE_H4.style
        TitleUiStyle.H5 -> ArticleTextStyles.TITLE_H5.style
        TitleUiStyle.H6 -> ArticleTextStyles.TITLE_H6.style
        TitleUiStyle.LIVE_REPORTER_INSIGHTS -> ArticleTextStyles.TITLE_LIVE_REPORTER_INSIGHT.style
        TitleUiStyle.STYLE_SECTION -> ArticleTextStyles.TITLE_STYLE.style
        else -> ArticleTextStyles.TITLE_DEFAULT.style
    }
}

enum class TitleUiStyle {
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,
    LIVE_REPORTER_INSIGHTS,
    STYLE_SECTION,
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun TitleViewStylePreview() {
    AndroidClassicTheme {
        TitleView(
            uiModel = TitleUiModel(
                text = "This is the Style section",
                uiStyle = TitleUiStyle.STYLE_SECTION
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleViewH1Preview() {
    AndroidClassicTheme {
        TitleView(
            uiModel = TitleUiModel(
                text = "Breaking: This is \"h1\"",
                uiStyle = TitleUiStyle.H1
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleViewH2Preview() {
    AndroidClassicTheme {
        TitleView(
            uiModel = TitleUiModel(
                text = "Breaking: This is \"h2\"",
                uiStyle = TitleUiStyle.H2
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleViewLiveReporterPreview() {
    AndroidClassicTheme {
        TitleView(
            uiModel = TitleUiModel(
                text = "Update: This is \"live-reporter-insight\"",
                uiStyle = TitleUiStyle.LIVE_REPORTER_INSIGHTS
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleViewDefaultPreview() {
    AndroidClassicTheme {
        TitleView(
            uiModel = TitleUiModel(
                text = "Breaking: This is the default style",
                uiStyle = TitleUiStyle.DEFAULT
            )
        )
    }
}
