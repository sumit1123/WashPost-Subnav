package com.washingtonpost.foryou.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.request.RequestOptions
import com.wapo.android.commons.util.timeAgo
import com.wapo.flagship.features.grid.LabelStyleEntity
import com.washingtonpost.foryou.R
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.byline
import com.washingtonpost.foryou.data.getDate
import com.washingtonpost.foryou.data.hasAuthorImage
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouViewModel
import com.washingtonpost.userhistory.ForYouViewedAction
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.media3.common.util.UnstableApi
import com.wapo.android.commons.util.truncatedString
import com.wapo.flagship.features.audio.service2.media.FOR_YOU_SECTION_NAME
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.washingtonpost.foryou.data.ForYouContentType
import com.washingtonpost.foryou.utils.ForYouVideoUtil
import jp.wasabeef.glide.transformations.BlurTransformation

private val franklinFont = FontFamily(Font(com.wapo.view.R.font.wp_franklinitcstd_font_family))
private val postoniFont = FontFamily(Font(com.wapo.view.R.font.wp_postoniwide_font_family))
private val georgiaFont = FontFamily(Font(com.wapo.view.R.font.wp_georgia_font_family))

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ForYouView(
    index: Int,
    recommendationsItem: RecommendationsItem,
    forYouViewModel: ForYouViewModel,
    userHistoryViewModel: UserHistoryViewModel,
    onItemClicked: ((Int, RecommendationsItem) -> Unit)?,
    onEllipsisClicked: ((Int, RecommendationsItem) -> Unit)?,
    onAudioClicked: ((RecommendationsItem, (Boolean) -> Unit) -> Unit)?,
    onCommentClicked: ((RecommendationsItem) -> Unit)?,
    onSummaryClicked: ((RecommendationsItem) -> Unit)?,
    cardSizePixels: Int,
    modifier: Modifier,
    player2Manager: PostTvPlayer2Manager? = null, // Only needed for video
    gridEnvironment: GridEnvironment,
    isActive: Boolean = false
) {

    val isVideo = recommendationsItem.contentType == ForYouContentType.VIDEO.type

    val recommendationsHelperItem = remember(recommendationsItem) {
        val uiState = forYouViewModel.uiState.value as? ForYouUiState.Feed
        RecommendationsHelperItem(
            recommendationsItem.articleId ?: recommendationsItem.video?.contentId,
            recommendationsItem.recReason,
            uiState?.requestId,
            uiState?.recipeId,
            uiState?.testId,
            recommendationsItem.contentType
        )
    }

    val isLoading = remember { mutableStateOf(false) }
    val placeholderColor = colorResource(id = R.color.foryou_image_placeholder)
    val context = LocalContext.current

    AndroidClassicTheme {
        val aspectRatio = run {
            val imageWidth = recommendationsItem.promoItems?.basic?.width?.toFloatOrNull()
            val imageHeight = recommendationsItem.promoItems?.basic?.height?.toFloatOrNull()
            val aspect = if (imageWidth != null && imageHeight != null && imageHeight != 0f) {
                imageWidth / imageHeight
            } else {
                1.5f
            }
            if (aspect <= 1f) 1.5f else aspect
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            shape = RoundedCornerShape(6.dp),
            elevation = 4.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clickable {
                        if (recommendationsItem.contentType.equals(ForYouContentType.VIDEO.type)) {
                            ForYouVideoUtil.getVideo(recommendationsItem, gridEnvironment.getPageConfig())?.let {
                                gridEnvironment.openForYouVideoCard(
                                    it,
                                    FOR_YOU_SECTION_NAME
                                )
                            }
                        } else {
                            onItemClicked?.invoke(
                                index,
                                recommendationsItem
                            )
                        }
                        userHistoryViewModel.captureForYouViewAction(
                            action = ForYouViewedAction.CLICKED,
                            recommendationsItem = recommendationsHelperItem,
                            adapterPosition = index,
                            surface = ForYouFeedRepositoryImpl.SURFACE_FEED
                        )
                    }
            ) {
                if (isVideo && player2Manager != null && recommendationsItem.video != null) {
                    ForYouVideoUtil.getVideo(recommendationsItem, gridEnvironment.getPageConfig())?.let {
                        ForYouVideoView(
                            video = it,
                            {
                                onEllipsisClicked?.invoke(
                                    index,
                                    recommendationsItem
                                )
                            },
                            gridEnvironment = gridEnvironment,
                            userHistoryViewModel = userHistoryViewModel,
                            recommendationsHelperItem = recommendationsHelperItem,
                            index = index,
                            forYouViewModel = forYouViewModel,
                            isActive = isActive
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(placeholderColor)
                        )

                        GlideImage(
                            model = recommendationsItem.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio),
                        ) {
                            it.override(cardSizePixels, (cardSizePixels / aspectRatio).toInt())
                        }
                    }


                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Blurred background layer
                        GlideImage(
                            model = recommendationsItem.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                            requestBuilderTransform = {
                                it.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 25)))
                            }
                        )
                        // tint
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0x80000000))
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            val prefixColor = colorResource(id = com.wapo.flagship.features.audio.R.color.byline_opinion)

                            val headlinePrefix =
                                recommendationsItem.labelDisplay?.basic?.headlinePrefix
                            val headlineText = recommendationsItem.headline.orEmpty()

                            if (headlineText.isNotEmpty()) {
                                val headlineStyled = remember(headlinePrefix, headlineText) {
                                    buildAnnotatedString {
                                        if (!headlinePrefix.isNullOrEmpty()) {
                                            withStyle(
                                                style = SpanStyle(
                                                    color = prefixColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    fontFamily = postoniFont
                                                )
                                            ) {
                                                append(headlinePrefix)
                                            }

                                            withStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF777777),
                                                    fontSize = 12.sp,
                                                    fontFamily = postoniFont
                                                )
                                            ) {
                                                append(" | ")
                                            }
                                        }

                                        withStyle(
                                            style = SpanStyle(
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = postoniFont
                                            )
                                        ) {
                                            append(headlineText)
                                        }
                                    }
                                }

                                // headline
                                Text(
                                    text = headlineStyled,
                                    color = colorResource(R.color.foryou_card_headline),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = postoniFont,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 16.dp)
                                )

                            }

                            val bylineText = recommendationsItem.byline()
                            val dateText = recommendationsItem.getDate()?.let { timeAgo(it) }

                            val signatureText = remember(bylineText, dateText) {
                                buildString {
                                    if (bylineText.isNotEmpty()) {
                                        append(bylineText)
                                    }
                                    if (!dateText.isNullOrEmpty()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(dateText)
                                    }
                                }
                            }

                            if (signatureText.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 8.dp)
                                ) {
                                    val sizePx = with(LocalDensity.current) {
                                        24.dp.toPx().toInt()
                                    }
                                    if (recommendationsItem.hasAuthorImage()) {
                                        GlideImage(
                                            model = recommendationsItem.credits?.by?.get(0)?.image?.url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                        ) {
                                            it.override(sizePx, sizePx)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text(
                                        text = signatureText,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            color = colorResource(R.color.foryou_card_byline),
                                            fontFamily = franklinFont
                                        ),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = recommendationsItem.description?.basic.orEmpty(),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = colorResource(R.color.foryou_card_blurb),
                                    lineHeight = 19.sp,
                                    fontFamily = georgiaFont
                                ),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(15.dp))
                            Spacer(modifier = Modifier.weight(1f))

                            Divider(
                                color = Color(0x66FFFFFF),
                                thickness = 1.dp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 5.dp)
                            )

                            val labelSpan = recommendationsItem.labelDisplay?.basic?.text.orEmpty()
                            val isOpinion =
                                recommendationsItem.labelDisplay?.basic?.style == LabelStyleEntity.OPINIONS.name.lowercase()


                            // prevents bottom row text from scaling with system font accessibility changes
                            CompositionLocalProvider(
                                LocalDensity provides Density(
                                    density = LocalDensity.current.density,
                                    fontScale = 1f
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        if (isOpinion) {
                                            val firstChar = labelSpan.substring(0, 1)
                                            val secondChar = labelSpan.substring(1, 2)
                                            val remainingText = labelSpan.substring(2)

                                            // red underline under opinion label (matching previous logic of splitting into two)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = firstChar,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    fontFamily = franklinFont,
                                                    modifier = Modifier.drawBehind {
                                                        val y = size.height
                                                        drawLine(
                                                            color = Color.Red,
                                                            start = Offset(0.dp.toPx(), y),
                                                            end = Offset(size.width, y),
                                                            strokeWidth = 1.5.dp.toPx()
                                                        )
                                                    }
                                                )
                                                Text(
                                                    text = secondChar,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    fontFamily = franklinFont,
                                                )
                                                Text(
                                                    text = remainingText,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    fontFamily = franklinFont,
                                                    modifier = Modifier.drawBehind {
                                                        val y = size.height
                                                        drawLine(
                                                            color = Color.Red,
                                                            start = Offset(-4.dp.toPx(), y),
                                                            end = Offset(size.width, y),
                                                            strokeWidth = 1.5.dp.toPx()
                                                        )
                                                    }
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = labelSpan,
                                                color = Color.White,
                                                fontFamily = franklinFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                    }

                                    Row(
                                        modifier = Modifier.weight(1.5f),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (recommendationsItem.summary != null && recommendationsItem.summary.reviewed != false) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(color = Color(0x26FFFFFF))
                                                    .clickable {
                                                        onSummaryClicked?.invoke(
                                                            recommendationsItem
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = com.washingtonpost.android.recirculation.R.drawable.carousel_summary_icon),
                                                    contentDescription = "Summary",
                                                    tint = Color(0xFFF0F0F0)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        if (recommendationsItem.comments?.count != null) {
                                            if (recommendationsItem.comments.count > 0) {
                                                Row(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .width(65.dp)
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(colorResource(R.color.foryou_card_icon_bg))
                                                        .clickable {
                                                            onCommentClicked?.invoke(
                                                                recommendationsItem
                                                            )
                                                        }
                                                        .padding(horizontal = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = com.wpds.wpds.R.drawable.comment),
                                                        contentDescription = "Comments",
                                                        tint = Color(0xFFF0F0F0),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    val commentCount =
                                                        recommendationsItem.comments.count.truncatedString()

                                                    if (commentCount != null) {
                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        Text(
                                                            text = commentCount,
                                                            color = Color(0xFFF0F0F0),
                                                            fontSize = 12.sp,
                                                            fontFamily = franklinFont,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Clip,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(color = Color(0x26FFFFFF))
                                                        .clickable {
                                                            onCommentClicked?.invoke(
                                                                recommendationsItem
                                                            )
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = com.wpds.wpds.R.drawable.comment),
                                                        contentDescription = "Comments",
                                                        tint = Color(0xFFF0F0F0)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        if (recommendationsItem.additionalProperties?.audioArticle?.enabled == true) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(color = Color(0x26FFFFFF))
                                                    .clickable {
                                                        onAudioClicked?.invoke(recommendationsItem) {
                                                            isLoading.value = it
                                                        }
                                                        userHistoryViewModel.captureForYouViewAction(
                                                            action = ForYouViewedAction.LISTENED,
                                                            recommendationsItem = recommendationsHelperItem,
                                                            adapterPosition = index,
                                                            surface = ForYouFeedRepositoryImpl.SURFACE_FEED
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_headphones),
                                                    contentDescription = "Listen",
                                                    tint = Color(0xFFF0F0F0)
                                                )

                                                if (isLoading.value) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(32.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color = Color(0x26FFFFFF))
                                                .clickable {
                                                    onEllipsisClicked?.invoke(
                                                        index,
                                                        recommendationsItem
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_ellipsis_img),
                                                contentDescription = "More",
                                                tint = Color(0xFFF0F0F0),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

            }
        }
    }
}