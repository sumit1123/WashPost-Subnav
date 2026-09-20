package com.wapo.flagship.features.articles3.views

import android.os.Build
import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.AuthorInfoUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AuthorInfoView(
    uiModel: AuthorInfoUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val style = getAuthorInfoStyle(uiModel.uiStyle, isClickable = uiModel.id != null)
    val bioStyle = getAuthorBioStyle(uiModel.uiStyle)

    Column {
        // Divider at the top
        if (uiModel.showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = wpdsColors.gray300)
            )
        }

        // Author info row with image and text
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = uiModel.id != null) {
                    articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AuthorNameClickEvent(uiModel.id))
                }
                .padding(vertical = 32.dp)
        ) {
            // Author image
            GlideImage(
                model = uiModel.imageUrl,
                contentDescription = "Author image for ${uiModel.name}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = wpdsColors.gray200),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Author name and bio
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiModel.name,
                    style = style,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(5.dp))

                uiModel.bio?.let {
                    Text(
                        text = parseHtmlText(it),
                        style = bioStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun parseHtmlText(htmlText: String): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(htmlText).toString()
    }
}

@Composable
private fun getAuthorInfoStyle(uiStyle: AuthorInfoUiStyle, isClickable: Boolean): TextStyle {
    return when (uiStyle) {
        AuthorInfoUiStyle.DEFAULT -> TextStyle(
            color = if (isClickable) wpdsColors.authorNameClickable else wpdsColors.articleText,
            fontSize = 16.sp,
            fontFamily = FranklinItcStandardFontFamily,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun getAuthorBioStyle(uiStyle: AuthorInfoUiStyle): TextStyle {
    return when (uiStyle) {
        AuthorInfoUiStyle.DEFAULT -> ArticleTextStyles.SUBTEXT_DEFAULT.style
    }
}

enum class AuthorInfoUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun AuthorInfoViewPreview() {
    AndroidClassicTheme {
        Surface(
            color = wpdsColors.secondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            AuthorInfoView(
                uiModel = AuthorInfoUiModel(
                    id = "1",
                    name = "Jamie Ross",
                    bio = "Jamie Ross is a writer for The 7, The Washington Post’s morning briefing, which is available each weekday from 7 a.m. to 10 a.m. Eastern time on mobile, desktop and inboxes.",
                    expertise = "",
                    imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                    showDivider = true
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}


