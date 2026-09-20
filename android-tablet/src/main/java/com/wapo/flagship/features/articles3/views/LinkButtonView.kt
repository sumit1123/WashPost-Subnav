package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.LinkButtonUiModel
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

enum class LinkButtonUiStyle {
    DEFAULT,
    OUTCOME
}

@Composable
fun LinkButtonView(
    uiModel: LinkButtonUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val isOutcome = uiModel.uiStyle == LinkButtonUiStyle.OUTCOME

    val containerColor = if (isOutcome) Color.Transparent else wpdsColors.primary
    val contentColor = if (isOutcome) wpdsColors.primary else wpdsColors.secondary
    val border = if (isOutcome) {
        BorderStroke(1.dp, wpdsColors.gray200)
    } else {
        null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.LinkClickEvent(uiModel.url))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            shape = RoundedCornerShape(30.dp),
            border = border
        ) {
            Text(
                text = uiModel.label,
                style = TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.sp
                )
            )
            if (uiModel.showArrow) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_right_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun LinkButtonViewDefaultPreview() {
    AndroidClassicTheme {
        LinkButtonView(
            uiModel = LinkButtonUiModel(
                label = "Continue reading",
                url = "https://www.washingtonpost.com/article",
                showArrow = true
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkButtonViewNoArrowPreview() {
    AndroidClassicTheme {
        LinkButtonView(
            uiModel = LinkButtonUiModel(
                label = "Continue reading",
                url = "https://www.washingtonpost.com/article",
                showArrow = false
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkButtonViewOutcomePreview() {
    AndroidClassicTheme {
        LinkButtonView(
            uiModel = LinkButtonUiModel(
                label = "See full results",
                url = "https://www.washingtonpost.com/results",
                showArrow = true,
                uiStyle = LinkButtonUiStyle.OUTCOME
            ),
            dummyArticlesInteractionHelper
        )
    }
}

// endregion


