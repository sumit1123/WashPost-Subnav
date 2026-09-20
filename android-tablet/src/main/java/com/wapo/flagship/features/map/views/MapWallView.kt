// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.map.views

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideSubcomposition
import com.bumptech.glide.integration.compose.RequestState
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.map.models.MapWallType
import com.wapo.flagship.features.map.models.MapWallUiData
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.paywall.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.utils.IconUtils

class MapWallView {
    @Composable
    fun MapWall(
        mapWallUiData: MapWallUiData,
        menu: Boolean,
        isSaved: Boolean,
        trackingInfo: OmnitureX?,
        closeMapWall: () -> Unit,
        snoozeMapWall: () -> Unit,
        saveOrRemoveMapRecommendation: () -> Unit,
        recommendNewMapArticle: () -> Unit,
    ) {
        val context = LocalContext.current
        AndroidClassicTheme {
            Surface(
                color = wpdsColors.wallPrimaryBg,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, wpdsColors.wallSecondaryBg),
                shape = RoundedCornerShape(4.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 8.dp)
                            .fillMaxWidth()
                            .clickable {
                                mapWallUiData.url?.let { openArticle(it, context) }
                            },
                ) {
                    Image(mapWallUiData.imageUrl)
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                        ) {
                            Label(mapWallUiData.wpdsIconName, mapWallUiData.label)
                            Spacer(modifier = Modifier.weight(1f))
                            if (menu) {
                                ActionMenuButton(
                                    isSaved,
                                    trackingInfo,
                                    saveOrRemoveMapRecommendation,
                                    recommendNewMapArticle,
                                    snoozeMapWall,
                                )
                            }
                            CloseButton(closeMapWall)
                        }
                        Title(mapWallUiData.title)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun Image(imageUrl: String?) {
        imageUrl?.let {
            GlideSubcomposition(
                model = it,
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .height(80.dp)
                        .widthIn(0.dp, 80.dp),
            ) {
                when (state) {
                    is RequestState.Success -> {
                        androidx.compose.foundation.Image(
                            painter = painter,
                            contentDescription = "article image",
                            modifier =
                                Modifier
                                    .height(80.dp)
                                    .width(80.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    @Composable
    fun Label(
        wpdsIconName: String?,
        text: String?,
    ) {
        Row {
            val drawableId = IconUtils(LocalContext.current).getDrawableId(wpdsIconName)
            drawableId?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = "",
                    modifier =
                        Modifier
                            .padding(0.dp, 8.dp, 6.dp, 0.dp)
                            .height(16.dp)
                            .width(16.dp),
                    tint = wpdsColors.primary,
                )
            }
            HtmlText(text ?: "")
        }
    }

    @Composable
    fun HtmlText(htmlText: String) {
        val spanned = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val annotatedString = spanned.toAnnotatedString()
        Text(
            modifier = Modifier.padding(0.dp, 8.dp, 0.dp, 0.dp),
            text = annotatedString,
            color = wpdsColors.primary,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
        )
    }

    @Composable
    fun Title(text: String?) {
        Text(
            text = text ?: "",
            color = wpdsColors.primary,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
        )
    }

    @Composable
    fun ActionMenuButton(
        isSaved: Boolean,
        trackingInfo: OmnitureX?,
        saveOrRemoveMapRecommendation: () -> Unit,
        recommendNewMapArticle: () -> Unit,
        snoozeMapWall: () -> Unit,
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
        ) {
            var expanded by remember { mutableStateOf(false) }
            IconButton(
                modifier =
                    Modifier
                        .padding(end = 8.dp)
                        .height(32.dp)
                        .width(32.dp),
                onClick = {
                    expanded = !expanded
                    Measurement.trackOnpageTap(
                        Measurement.MAP_MENU_ICON_CLICK,
                        trackingInfo?.pageName,
                        trackingInfo?.arcId,
                    )
                },
                content = {
                    Icon(
                        painter = painterResource(com.wpds.wpds.R.drawable.dots_vertical),
                        contentDescription = "",
                        tint = wpdsColors.primary,
                    )
                },
            )

            DropdownMenu(
                modifier = Modifier.background(wpdsColors.wallPrimaryBg),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                val style =
                    TextStyle(
                        color = wpdsColors.gray20,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily((Font(com.wapo.view.R.font.franklinitcstd_light))),
                    )
                DropdownMenuItem(
                    leadingIcon = {
                        if (isSaved) {
                            Icon(
                                painter = painterResource(com.wpds.wpds.R.drawable.bookmark_solid),
                                contentDescription = "bookmark solid icon",
                                tint = wpdsColors.primary,
                            )
                        } else {
                            Icon(
                                painter = painterResource(com.wpds.wpds.R.drawable.bookmark),
                                contentDescription = "bookmark icon",
                                tint = wpdsColors.primary,
                            )
                        }
                    },
                    text = {
                        if (isSaved) {
                            Text(
                                text = stringResource(com.wpds.wpds.R.string.remove_from_saved_stories),
                                style = style,
                            )
                        } else {
                            Text(
                                text = stringResource(com.wpds.wpds.R.string.add_to_saved_stories),
                                style = style,
                            )
                        }
                    },
                    onClick = {
                        expanded = !expanded
                        saveOrRemoveMapRecommendation()
                    },
                )
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(com.wpds.wpds.R.drawable.refresh),
                            contentDescription = "refresh icon",
                            tint = wpdsColors.primary,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(com.wpds.wpds.R.string.recommend_new),
                            style = style,
                        )
                    },
                    onClick = {
                        expanded = !expanded
                        Measurement.trackOnpageTap(
                            Measurement.MAP_MENU_RECOMMEND_CLICK,
                            trackingInfo?.pageName,
                            trackingInfo?.arcId,
                        )
                        recommendNewMapArticle()
                    },
                )
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(com.wpds.wpds.R.drawable.time),
                            contentDescription = "snooze icon",
                            tint = wpdsColors.primary,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(com.wpds.wpds.R.string.snooze),
                            style = style,
                        )
                    },
                    onClick = {
                        expanded = !expanded
                        Measurement.trackOnpageTap(
                            Measurement.MAP_MENU_SNOOZE_CLICK,
                            trackingInfo?.pageName,
                            trackingInfo?.arcId,
                        )
                        snoozeMapWall()
                    },
                )
            }
        }
    }

    @Composable
    fun CloseButton(closeMapWall: () -> Unit) {
        IconButton(
            modifier =
                Modifier
                    .height(32.dp)
                    .width(32.dp),
            onClick = {
                closeMapWall()
            },
            content = {
                Icon(
                    painter = painterResource(com.wpds.wpds.R.drawable.close),
                    contentDescription = "",
                    tint = wpdsColors.primary,
                )
            },
        )
    }

    /**
     * Converts a [Spanned] into an [AnnotatedString] trying to keep as much formatting as possible.
     *
     * Currently supports `bold`, `italic`, and `underline`
     */
    private fun Spanned.toAnnotatedString(): AnnotatedString =
        buildAnnotatedString {
            val spanned = this@toAnnotatedString
            append(spanned.toString())
            getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                when (span) {
                    is StyleSpan ->
                        when (span.style) {
                            Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            Typeface.BOLD_ITALIC ->
                                addStyle(
                                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                    start,
                                    end,
                                )
                        }
                    is UnderlineSpan ->
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start,
                            end,
                        )
                }
            }
        }

    private fun openArticle(
        url: String?,
        context: Context,
    ) {
        url ?: return
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(url)
                .setNavigationBehavior(Measurement.MAP_RECIRC)
                .setItId(Measurement.MAP_RECIRC)
                .buildIntent(context)
        context.startActivity(intent)
    }

    @Preview(showBackground = true, device = "id:pixel_6")
    @Composable
    private fun Preview() {
        AndroidClassicTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = wpdsColors.wallPrimaryBg,
            ) {
                val mapWallUiData =
                    MapWallUiData(
                        "FOR YOU",
                        "for-you",
                        "Going plastic-free is nearly impossible. These people are trying anyway.",
                        "By Washington Post",
                        "https://www.washingtonpost.com/home/2024/06/26/reduce-waste-plastic-free-july/",
                        "https://www.washingtonpost.com/wp-apps/imrs.php?src=https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/D3FVNOHXRRBVTEQVCNYLINE2LQ.png",
                        MapWallType.FLEX,
                    )

                MapWall(
                    mapWallUiData = mapWallUiData,
                    menu = true,
                    isSaved = false,
                    trackingInfo = null,
                    closeMapWall = {},
                    snoozeMapWall = {},
                    saveOrRemoveMapRecommendation = {},
                    recommendNewMapArticle = {},
                )
            }
        }
    }
}
