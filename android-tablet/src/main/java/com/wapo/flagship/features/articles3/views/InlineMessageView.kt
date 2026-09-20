package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.InlineMessageUiModel
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.washingtonpost.android.sections.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

/**
 * Inline message view based on InlineMessageArticleHolder / InlineOfferView.
 * Displays a promotional offer with icon, title, optional body, and action button.
 */
@Composable
fun InlineMessageView(
    uiModel: InlineMessageUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    if (!uiModel.isVisible || uiModel.articleInlineMessage?.title.isNullOrEmpty() || uiModel.articleInlineMessage.action.isNullOrEmpty()) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent)
    ) {
        Column {
            // Icon with horizontal dividers on each side
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (startDiv, image, endDiv) = createRefs()

                HorizontalDivider(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .constrainAs(startDiv) {
                            start.linkTo(parent.start)
                            end.linkTo(image.start)
                            top.linkTo(image.top)
                            bottom.linkTo(image.bottom, margin = 10.dp)
                            width = Dimension.fillToConstraints
                        },
                    thickness = 1.dp,
                    color = wpdsColors.gray400
                )

                HorizontalDivider(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .constrainAs(endDiv) {
                            top.linkTo(image.top)
                            bottom.linkTo(image.bottom, margin = 10.dp)
                            start.linkTo(image.end)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        },
                    thickness = 1.dp,
                    color = wpdsColors.gray400
                )

                val icon = if (isSystemInDarkTheme()) {
                    R.drawable.ic_extra_account_wp_dark
                } else {
                    R.drawable.ic_extra_account_wp
                }
                Image(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(bottom = 8.dp)
                        .constrainAs(image) {
                            start.linkTo(startDiv.end)
                            end.linkTo(endDiv.end)
                        },
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }

            // Title
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = AnnotatedString.fromHtml(uiModel.articleInlineMessage?.title ?: ""),
                color = colorResource(com.washingtonpost.android.paywall.R.color.top_label_text),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontFamily = FranklinItcStandardFontFamily
            )

            // Body (optional)
            if (!uiModel.articleInlineMessage?.body.isNullOrEmpty()) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 12.dp)
                        .align(Alignment.CenterHorizontally),
                    text = AnnotatedString.fromHtml(uiModel.articleInlineMessage.body ?: ""),
                    color = colorResource(com.washingtonpost.android.paywall.R.color.top_label_text),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontFamily = FranklinItcStandardFontFamily
                )
            }

            // Action button
            Button(
                modifier = Modifier
                    .defaultMinSize(minWidth = 336.dp)
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = wpdsColors.blue100Static
                ),
                shape = RoundedCornerShape(24.dp),
                onClick = {
                    articlesInteractionHelper.onEventFired(
                        ArticleInteractionEvent.InlineMessageBannerEvent(
                            BannerEvent.BannerClicked(message = null, iamMessageType = IamMessageType.ARTICLE.type)
                        )
                    )
                }
            ) {
                Text(
                    text = AnnotatedString.fromHtml(uiModel.articleInlineMessage?.action ?: ""),
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    color = wpdsColors.gray700Static,
                    fontFamily = FontFamily(Font(R.font.franklinitcstd_light))
                )
            }
        }

        // Bottom divider
        HorizontalDivider(
            thickness = 1.dp,
            color = wpdsColors.gray400
        )
    }
}

enum class InlineOfferUiStyle {
    DEFAULT
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun InlineMessageViewPreview() {
    AndroidClassicTheme {
        InlineMessageView(
            uiModel = InlineMessageUiModel(
                ArticleInlineMessage(
                    attributionInfo = null,
                    title = "Share 3 extra accounts with friends and family",
                    body = null,
                    action = "Share extra account",
                    url = "https://washingtonpost.com/my-post/account/extra-accounts",
                    isEligiblePromo = null,
                ),
            ).apply { isVisible = true },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InlineMessageViewWithBodyPreview() {
    AndroidClassicTheme {
        InlineMessageView(
            uiModel = InlineMessageUiModel(
                ArticleInlineMessage(
                    attributionInfo = null,
                    title = "Share 3 extra accounts with friends and family",
                    body = "Included with your subscription at no extra cost.",
                    action = "Share extra account",
                    url = "https://washingtonpost.com/my-post/account/extra-accounts",
                    isEligiblePromo = null,
                ),
            ).apply { isVisible = true },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InlineMessageViewHiddenPreview() {
    AndroidClassicTheme {
        InlineMessageView(
            uiModel = InlineMessageUiModel(
                ArticleInlineMessage(
                    attributionInfo = null,
                    title = "This should not be visible",
                    body = null,
                    action = "Hidden",
                    url = null,
                    isEligiblePromo = null,
                ),
            ).apply { isVisible = false },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

// endregion
