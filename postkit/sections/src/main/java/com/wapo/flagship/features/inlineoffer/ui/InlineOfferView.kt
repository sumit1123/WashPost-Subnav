package com.wapo.flagship.features.inlineoffer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
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
import com.washingtonpost.android.sections.R


fun showInlineOfferView(
    composeView: ComposeView,
    isVisible: Boolean,
    isSingleColumn: Boolean,
    title: String? = "Share 3 extra accounts with friends and family - included with your subscription",
    body: String?,
    url: String? = "https://washingtonpost.com/my-post/account/extra-accounts",
    action: String? = "Share extra account",
    inlineOfferType: InlineOfferType,
    onClick: (String?) -> Unit
) {
    composeView.apply {
        setContent {
            InlineOfferView(
                modifier = Modifier.padding(
                    top = 0.dp,
                    end = 0.dp,
                    start = 0.dp,
                    bottom = 7.dp
                ),
                isVisible = isVisible,
                isSingleColumn,
                title = title,
                body = body,
                url = url,
                action = action,
                inlineOfferType = inlineOfferType,
                onClick = onClick
            )
        }
    }
}

@Composable
@Preview
fun InlineOfferView(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    isSingleColumn: Boolean = true,
    title: String? = "Share 3 extra accounts with friends and family - included with your subscription",
    body: String?,
    url: String? = "https://washingtonpost.com/my-post/account/extra-accounts",
    action: String? = "Share extra account",
    inlineOfferType: InlineOfferType = InlineOfferType.HOMEPAGE,
    onClick: ((String?)) -> Unit = {}
) {
    if (isVisible && !title.isNullOrEmpty() && !action.isNullOrEmpty()) {
        val fontFamily =
            remember { FontFamily(Font(com.wapo.view.R.font.wp_franklinitcstd_font_family)) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(com.wapo.view.R.color.table_row_header_bg)),
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (isSingleColumn) 20.dp else 0.dp
                    )
            ) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    val (startDiv, image, endDiv) = createRefs()
                    if (inlineOfferType == InlineOfferType.ARTICLE) {
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
                            color = colorResource(com.wapo.view.R.color.live_image_container_border)
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
                            color = colorResource(com.wapo.view.R.color.live_image_container_border)
                        )
                    }

                    val icon =
                        if (isSystemInDarkTheme()) R.drawable.ic_extra_account_wp_dark else R.drawable.ic_extra_account_wp
                    Image(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(bottom = 8.dp)
                            .constrainAs(image) {
                                if (inlineOfferType == InlineOfferType.ARTICLE) {
                                    start.linkTo(startDiv.end)
                                    end.linkTo(endDiv.end)
                                } else {
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }

                            },
                        painter = painterResource(icon),
                        contentDescription = null
                    )

                }

                val text = AnnotatedString.fromHtml(title)
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = text,
                    color = colorResource(com.washingtonpost.android.paywall.R.color.top_label_text),
                    textAlign = TextAlign.Center,
                    fontSize = if (inlineOfferType == InlineOfferType.HOMEPAGE) 20.sp else 14.sp,
                    fontFamily = fontFamily,
                )

                if (!body.isNullOrEmpty()) {
                    val bodyText = AnnotatedString.fromHtml(body)
                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 12.dp)
                            .align(Alignment.CenterHorizontally),
                        text = bodyText,
                        color = colorResource(com.washingtonpost.android.paywall.R.color.top_label_text),
                        textAlign = TextAlign.Center,
                        fontSize = if (inlineOfferType == InlineOfferType.HOMEPAGE) 20.sp else 14.sp,
                        fontFamily = fontFamily,
                    )
                }

                val viewTypeModifier =
                    if (inlineOfferType == InlineOfferType.HOMEPAGE) Modifier.wrapContentWidth() else Modifier.defaultMinSize(
                        minWidth = 336.dp
                    )
                Button(
                    modifier = viewTypeModifier
                        .padding(bottom = if (inlineOfferType == InlineOfferType.HOMEPAGE) 20.dp else 16.dp)
                        .align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(com.washingtonpost.android.paywall.R.color.subscription_blue)),
                    shape = RoundedCornerShape(24.dp),
                    onClick = { onClick.invoke(url) }) {
                    Text(
                        text = AnnotatedString.fromHtml(action),
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center,
                        color = colorResource(com.wapo.flagship.features.audio.R.color.white),
                        fontFamily = FontFamily(Font(R.font.franklinitcstd_light))
                    )
                }
            }
            if (inlineOfferType == InlineOfferType.ARTICLE) {
                HorizontalDivider(
                    modifier = Modifier,
                    thickness = 1.dp,
                    color = colorResource(com.wapo.view.R.color.live_image_container_border)
                )
            }
        }
    }
}

enum class InlineOfferType {
    ARTICLE, HOMEPAGE
}