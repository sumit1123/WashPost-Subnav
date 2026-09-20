package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.CommentsUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import com.washingtonpost.android.R

@Composable
fun CommentsView(
    uiModel: CommentsUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 250.dp)
                .background(
                    color = wpdsColors.surface,
                    shape = RoundedCornerShape(50.dp)
                )
                .border(
                    width = 1.dp,
                    color = wpdsColors.onSurface,
                    shape = RoundedCornerShape(50.dp)
                )
                .clickable {
                    articlesInteractionHelper.onEventFired(
                        ArticleInteractionEvent.ViewCommentsClickEvent
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = com.washingtonpost.android.articles.R.string.view_comments),
                style = getCommentsButtonStyle()
            )
        }
    }
}

@Composable
private fun getCommentsButtonStyle(): TextStyle {
    return TextStyle(
        fontFamily = FranklinItcStandardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = wpdsColors.onSurface
    )
}

enum class CommentsUiStyle {
    // May need to add more styles when comments go native
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun CommentsViewPreview() {
    AndroidClassicTheme {
        CommentsView(
            uiModel = CommentsUiModel(),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}




