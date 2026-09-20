/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.view.habittiles

import com.wapo.android.commons.util.Logger
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.view.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.utils.scaleAndAlphaTap


enum class TileContextIndicator(val value: String) {
    OMITTED("omitted"),
    RED_BLINKING("red_blinking"),
    LIVE("live"),
    TRENDING("trending"),
    SAVED("saved"),
    PROGRESS_10("progress_10"),
    PROGRESS_20("progress_20"),
    PROGRESS_30("progress_30"),
    PROGRESS_40("progress_40"),
    PROGRESS_50("progress_50"),
    PROGRESS_60("progress_60"),
    PROGRESS_70("progress_70"),
    PROGRESS_80("progress_80"),
    PROGRESS_90("progress_90")
}
enum class TileLabelBehavior(val value: String) {
    WRAP("wrap"),
    SCROLLING("scrolling")
}
@Composable
fun TilesGrid(
    tiles: List<Tile>,
    areThereEnoughTilesToRender: Boolean,
    cardified: Boolean,
    phoneBreakpoint: Boolean,
    updatedIndices: List<Int>,
    onTileTapped: (Tile) -> Unit = {},
    preview: Boolean = false
) {

    var tallestCardHeight by remember {
        mutableStateOf(72.dp)
    }

    if (tiles.isNotEmpty() && !preview) {
        SubcomposeLayout { constraints ->
            val placeables = subcompose("measureCards") {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TileCell(tile = tiles[0], cardified = cardified, phoneBreakpoint = phoneBreakpoint, checkHeight = true)
                }
            }.map {
                it.measure(constraints)
            }

            val maxCardHeight = placeables.maxOfOrNull { it.height }?.toDp() ?: 72.dp

            if (maxCardHeight > tallestCardHeight) {
                tallestCardHeight = maxCardHeight
            }

            layout(constraints.minWidth, constraints.minHeight) { }
        }
    }

    val cardSpacing = 8.dp

    Column(
        modifier = Modifier
            .padding(
                bottom = if (areThereEnoughTilesToRender) 8.dp else 0.dp,
                top = if (areThereEnoughTilesToRender) 4.dp else 0.dp
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(cardSpacing)
    ) {
        val numTilesInRow = if (phoneBreakpoint) {
            2
        } else {
            if (tiles.size >= 4) 4 else 2
        }

        var index = 0
        while (tiles.size - index >= numTilesInRow) {
            Row(
                modifier = Modifier
                    .height(tallestCardHeight)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                repeat(numTilesInRow) {
                    Logger.d("TilesGrid", "index: $index")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .weight(1f)
                    ) {
                        val hasChanged = index in updatedIndices
                        TileCell(tiles[index], cardified, phoneBreakpoint, false, hasChanged, onTileTapped)
                        index += 1
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun TileCell(
    tile: Tile,
    cardified: Boolean,
    phoneBreakpoint: Boolean,
    checkHeight: Boolean = false,
    hasChanged: Boolean = false,
    onTileTapped: (Tile) -> Unit = {}
) {
    val animatable = remember {
        Animatable(1f)
    }

    var currentTile by remember {
        mutableStateOf(tile)
    }

    LaunchedEffect(tile) {
        if (hasChanged) {
            animatable.animateTo(0f, animationSpec = tween(300))
            currentTile = tile
            animatable.animateTo(1f, animationSpec = tween(500))
        } else {
            currentTile = tile
            animatable.animateTo(1f)
        }
    }

    val cardElevation = 0.dp
    val cardShape = RoundedCornerShape(4.dp)

    Card(
        modifier = Modifier
            .fillMaxHeight()
            .alpha(alpha = animatable.value)
            .animateContentSize()
            .scaleAndAlphaTap(0.9f, 0.8f) {
                if (currentTile.persoPodcastMetadata?.itemType == "placeholder" || !currentTile.tileLink.isNullOrEmpty()) {
                    onTileTapped.invoke(tile)
                }
            }
            .padding(cardElevation)
            .defaultMinSize(minHeight = 72.dp)
            .fillMaxWidth(),
        shape = cardShape,
        elevation = 0.dp,
        backgroundColor = wpdsColors.gridCardBg,
        border = BorderStroke(Dp.Hairline, colorResource(R.color.tile_border))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1F)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row { ContextLabel(currentTile) }
                Row { TitleLabel(currentTile, checkHeight) }
            }
            if (!currentTile.imageUrl.isNullOrEmpty()) {
                Column(
                    Modifier.width(40.dp)
                ) {
                    GlideImage(
                        model = currentTile.imageUrl,
                        contentDescription = currentTile.tileLabel,
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.FillHeight
                    )
                }
            }
        }
    }
}

@Composable
fun LufPrefix() {
    Text(
        text = "Live" ,
        modifier = Modifier.padding(end = 4.dp),
        style = TextStyle(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
            fontWeight = FontWeight(300),
            color = wpdsColors.liveUpdateTextColor
        ),
    )
}

@Composable
fun ContextLabel(tile: Tile) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        when (tile.contextIndicator) {
            TileContextIndicator.LIVE.value, TileContextIndicator.RED_BLINKING.value -> LufPrefix()
            TileContextIndicator.SAVED.value -> GetIcon()
            in listOf(
                TileContextIndicator.PROGRESS_10.value,
                TileContextIndicator.PROGRESS_20.value,
                TileContextIndicator.PROGRESS_30.value,
                TileContextIndicator.PROGRESS_40.value,
                TileContextIndicator.PROGRESS_50.value,
                TileContextIndicator.PROGRESS_60.value,
                TileContextIndicator.PROGRESS_70.value,
                TileContextIndicator.PROGRESS_80.value,
                TileContextIndicator.PROGRESS_90.value,
            ) -> tile.contextIndicator?.let { getCircularProgressBar(it.substringAfter("_").toFloat()/100, tile.contextLabel) }
        }

        Text(
            text = tile.contextLabel ?: "",
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                fontWeight = FontWeight(300),
                color = if (tile.contextIndicator == TileContextIndicator.LIVE.value) wpdsColors.liveUpdateTextColor else wpdsColors.gray80
            ),
            maxLines = 1,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (tile.tileLabelBehavior == TileLabelBehavior.SCROLLING.value && tile.imageUrl == null) {
                Modifier
            } else {
                Modifier.padding(end = 5.dp)
            }
        )
    }
}

@Composable
fun GetIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_bookmark_saved),
        tint = wpdsColors.gray0,
        contentDescription = "bookmark"
    )
}


@ExperimentalFoundationApi
@Composable
fun TitleLabel(tile: Tile, checkHeight: Boolean = false) {
    Text(
        text = tile.tileLabel ?: "",
        modifier = if (tile.tileLabelBehavior == TileLabelBehavior.WRAP.value) {
            Modifier.padding(end = 5.dp)
        } else {
            Modifier
                .padding(end = 5.dp)
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawFadedEdge(leftEdge = false)
                }
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2000,
                    repeatDelayMillis = 2000,
                    velocity = MarqueeDefaults.Velocity + (MarqueeDefaults.Velocity * 0.5f)
                )
        },
        style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 17.5.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
            fontWeight = FontWeight(700),
            color = wpdsColors.primary
        ),
        maxLines = 2,
        minLines = if (checkHeight) 2 else 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun ContentDrawScope.drawFadedEdge(leftEdge: Boolean) {
    val edgeWidth = 32.dp
    val edgeWidthPx = edgeWidth.toPx()
    drawRect(
        topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
        size = Size(edgeWidthPx, size.height),
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0X00FFFFFF), Color(0XADFFFFFF)),
            startX = if (leftEdge) 0f else size.width,
            endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx
        ),
        blendMode = BlendMode.DstIn
    )
}

@Preview(
    showBackground = true,
    device = "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420"
)
@Composable
private fun Preview() {
    AndroidClassicTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = wpdsColors.wallSecondaryBg
        ) {
            Column {
                TilesGrid(
                    tilesPreviewList,
                    areThereEnoughTilesToRender = true,
                    cardified = false,
                    phoneBreakpoint = true,
                    updatedIndices = emptyList(),
                    preview = true
                )
                TilesCta(
                    cta = TilesCta(text = "Give feedback", url = ""),
                    areThereEnoughTilesToRender = tilesPreviewList.isNotEmpty(),
                    onClick = null
                )
            }
        }
    }
}

@Composable
fun TilesCta(
    cta: TilesCta?,
    areThereEnoughTilesToRender: Boolean,
    onClick: ((String) -> Unit)?
) {
    if (!areThereEnoughTilesToRender)
        return
    val text = cta?.text ?: return
    val url = cta.url ?: return
    val iconSize = 14.sp

    val arrowContentId = "inlineContent"
    val annotatedString = buildAnnotatedString {
        append(text)
        // Append a placeholder string "[arrow_placeholder]" and attach an annotation "inlineContent" on it.
        appendInlineContent(arrowContentId, "[arrow_placeholder]")
    }

    val inlineContent = mapOf(
        Pair(
            // This tells the [Text] to replace the placeholder string "[arrow_placeholder]" by
            // the composable given in the [InlineTextContent] object.
            arrowContentId,
            InlineTextContent(
                // Placeholder tells text layout the expected size and vertical alignment of
                // children composable.
                Placeholder(
                    width = iconSize,
                    height = iconSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
                )
            ) {
                Icon(
                    modifier = Modifier
                        .padding(start = with(LocalDensity.current) { 2.sp.toDp() })
                        .size(with(LocalDensity.current) { iconSize.toDp() }),
                    painter = painterResource(com.wpds.wpds.R.drawable.comment),
                    tint = wpdsColors.gray80,
                    contentDescription = null
                )
            }
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 8.dp, top = 8.dp, end = 0.dp, bottom = 8.dp)
                .clickable {
                    onClick?.invoke(url)
                },
        ) {
            Text(
                text = annotatedString,
                inlineContent = inlineContent,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = wpdsColors.gray80,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily(Font(R.font.wp_franklinitcstd_font_family)),
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight(300)
                ),
            )
        }
    }
}

@Composable
fun getCircularProgressBar(
    progress: Float?,
    contextLabel: String?,
    progressColor: Color = wpdsColors.gray0,
    backgroundColor: Color = wpdsColors.gray300,
    strokeWidth: Dp = 2.dp,
    size: Dp = 13.dp
) {
    if (progress == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        var animationPlayed by remember { mutableStateOf(true) }
        val animatedProgress by animateFloatAsState(
            targetValue = if (animationPlayed) progress else 0f,
            animationSpec = tween(durationMillis = 1000), label = ""
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier.size(size)
            ) {
                drawArc(
                    color = backgroundColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360,
                    useCenter = false,
                    style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }

        }

        if (contextLabel != null) {
            Text(
                text = contextLabel,
                Modifier.padding(start = 4.dp),
                style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                    fontWeight = FontWeight(300),
                    color = wpdsColors.gray80
                )
            )
        }
    }
}

val tilesPreviewList = listOf(
    Tile(
        1.0,
        "ai_podcast",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/QTAKD6BHFVHMFMBHOSX7267PIY.png",
        "Listen to",
        "omitted",
        "Your personalized podcast",
        "https://wapo-personalized-podcasts-staging.s3.amazonaws.com/onboarding/onboarding.mp3?AWSAccessKeyId=ASIATKPZH4EEARZYOKQA&Signature=0M%2FQqC5vExod0TxcUZbewkaAgeY%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEN%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCIQCcEGdwp%2FsSavC204E1jPkwxILaEy7oj1FKjGIbpgQG4QIgc8%2FiqcI8359XMafvDCzaO8uWeF9I0583dLGAawa%2FcHAqhgQIp%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAEGgwyMjg2OTI4NDQ4MDgiDFL2jtGUWnldMcXsYCraA5aRyJIaS7rdt2nj%2F%2FbqpCjrMIqcdGrPMgSfJ2nl%2FKRiZYrjgVlvc0EUooqIo%2BSbIxopE1UIHBLdNVhNe4he%2FHRDI1xHSRGlZC7Pbplvqt4bCGZrm72A0jSBTE6W4LiuSCDvDN340nWY4VreKObzQV%2B7fQxecMs0ii2Xhn6iU%2F0lrwoDJqv4B2zMJXOhCtE995TbKceuEnBN71z%2FHyy6UlFUwyKeTJg3GTYjskDzcBfwmgz2ljr9UmJDnW1ce%2BvFY379cTomL3BGCfxrKex5HSpzL2l%2FSBDd8KkZdShGhl8sSgrwyK1fN8lz%2F5aeppI7dD8fRaf88lLuyeN4Ay5zswTgeqFIF1vxHBWSYyPgZ3WVXAGdaOGzMRVhzQeNcQFGFqg6lP4gVdxxwKp36ufRevHrBEqxa8ZIFTkvU7Gy4VvYNkSYeMTWEZu6RCBkoPyJzXMJXXnc3ykD7eQ4bLWnT4kMsDDthJM2hp2KC4cJdtb9dSeTsahF1T3lswBwtZVmqs5JEVMD26GK2IgpGtSSFPVLUtU1KU7a4iD0SxIs6moRcpjEGO2oRrJNwZOop9hZG%2BDdLaBBo%2FJQXiEaUCnsxhPRObmMii2BT7bUAobRGEGEJaS%2FTn9c115g5jD5tf7OBjqlAdu3PQTRJFZiQ4DU7heIrSGnxCNpcmKijRlaw7LyVe%2BQj8RCVC%2F%2BUHgKGujtxXTQXeaMasfug9ObmjnC4QRVQuUxfR2u91Rhx4TzLkLol86NnYC6AgoArH9uDivGl4OFkXN8BKS14ccabSGVHe47UTxviz8Ul2cpFKsGhqFLSFA7Wleddn0wj1m2k56f2J%2BxNFtcHY94S6S9U55mfxPMltiY%2BQ2kIg%3D%3D&Expires=1776270993", //add onboarding mp3 link for testing
        "wrap",
        "ai_podcast",
        0,
        persoPodcastMetadata = PersonalizedPodcast(
            "onboarding_832_20251107",
            null,
            "onboarding",
            "Welcome",
            "Your generated podcast",
            "Welcome to Your Personalized Podcast",
            image = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/FTNQXJJ6XVBMNKFO3E6YDZBPGM.png",
            audioFilePath = "https://wapo-personalized-podcasts-staging.s3.amazonaws.com/onboarding/onboarding.mp3?AWSAccessKeyId=ASIATKPZH4EEARZYOKQA&Signature=0M%2FQqC5vExod0TxcUZbewkaAgeY%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEN%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCIQCcEGdwp%2FsSavC204E1jPkwxILaEy7oj1FKjGIbpgQG4QIgc8%2FiqcI8359XMafvDCzaO8uWeF9I0583dLGAawa%2FcHAqhgQIp%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAEGgwyMjg2OTI4NDQ4MDgiDFL2jtGUWnldMcXsYCraA5aRyJIaS7rdt2nj%2F%2FbqpCjrMIqcdGrPMgSfJ2nl%2FKRiZYrjgVlvc0EUooqIo%2BSbIxopE1UIHBLdNVhNe4he%2FHRDI1xHSRGlZC7Pbplvqt4bCGZrm72A0jSBTE6W4LiuSCDvDN340nWY4VreKObzQV%2B7fQxecMs0ii2Xhn6iU%2F0lrwoDJqv4B2zMJXOhCtE995TbKceuEnBN71z%2FHyy6UlFUwyKeTJg3GTYjskDzcBfwmgz2ljr9UmJDnW1ce%2BvFY379cTomL3BGCfxrKex5HSpzL2l%2FSBDd8KkZdShGhl8sSgrwyK1fN8lz%2F5aeppI7dD8fRaf88lLuyeN4Ay5zswTgeqFIF1vxHBWSYyPgZ3WVXAGdaOGzMRVhzQeNcQFGFqg6lP4gVdxxwKp36ufRevHrBEqxa8ZIFTkvU7Gy4VvYNkSYeMTWEZu6RCBkoPyJzXMJXXnc3ykD7eQ4bLWnT4kMsDDthJM2hp2KC4cJdtb9dSeTsahF1T3lswBwtZVmqs5JEVMD26GK2IgpGtSSFPVLUtU1KU7a4iD0SxIs6moRcpjEGO2oRrJNwZOop9hZG%2BDdLaBBo%2FJQXiEaUCnsxhPRObmMii2BT7bUAobRGEGEJaS%2FTn9c115g5jD5tf7OBjqlAdu3PQTRJFZiQ4DU7heIrSGnxCNpcmKijRlaw7LyVe%2BQj8RCVC%2F%2BUHgKGujtxXTQXeaMasfug9ObmjnC4QRVQuUxfR2u91Rhx4TzLkLol86NnYC6AgoArH9uDivGl4OFkXN8BKS14ccabSGVHe47UTxviz8Ul2cpFKsGhqFLSFA7Wleddn0wj1m2k56f2J%2BxNFtcHY94S6S9U55mfxPMltiY%2BQ2kIg%3D%3D&Expires=1776270993", //add onboarding mp3 link for testing
            articlesUsed = null,
            audioDuration = 60.0f,
            summary = null,
            totalCharacters = null,
            createdAt = null
        )
    ),
    Tile(
        0.008635578583765112,
        "subsections",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/3E7GDI6LPNGTFOAXWRI7NQUQXQ.jpg",
        "Catch Up On",
        "omitted",
        "Help Desk",
        "https://www.washingtonpost.com/technology/consumer-tech/",
        "wrap",
        null,
        1
    ),
    Tile(
        0.006908462867012091,
        "subsections",
        "",
        "Trending",
        "trending",
        "Comics",
        "https://www.washingtonpost.com/entertainment/comics/",
        "wrap",
        null,
        2
    ),
    Tile(
        1.0,
        "author",
        "https://s3.amazonaws.com/arc-authors/washpost/59ec5235-2ff6-495e-b95f-5bbf4fec51fc.png",
        "Continue reading",
        "progress_40",
        "‘People are shocked’: A hostile health-care takeover is underway",
        "https://www.washingtonpost.com/opinions/2025/01/29/medical-research-trump-administration-nih/#SEA7MOBPFRA57ORV5HTXQEC5IE",
        "wrap",
        null,
        3
    ),
    Tile(
        1.0,
        "games",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/4YTNQF6G7ZESZMXZO2GN3JMTD4.png",
        "Today's",
        "omitted",
        "Daily Crossword",
        "https://www.washingtonpost.com/crossword-puzzles/daily/",
        "wrap",
        null,
        4
    ),
    Tile(
        0.40000000000000013,
        "games",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/NLZIZNK2TNDHFINONFZ44F56CI.png",
        "Today's",
        "omitted",
        "On the Record",
        "https://www.washingtonpost.com/news-quiz/",
        "wrap",
        null,
        5
    ),
    Tile(
        1.0,
        "lufs",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/6F2RURKVXORQ5NNKEHG4YSQRLI_size-normalized.jpg",
        "21 min ago",
        "red_blinking",
        "FBI hunts for Trump rally shooter’s motive; Secret Service scrutinized",
        "https://www.washingtonpost.com/nation/2024/07/15/trump-rally-shooting-updates-shooter-victims/",
        "scrolling",
        null,
        6
    ),
    Tile(
        0.05522523445085069,
        "lufs",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/7SOSVN7GVNE6HCTN3BUMYZKKQE.JPG",
        "Live Updates",
        "omitted",
        "Elections 2024 live updates: Biden events draw heightened scrutiny after debate",
        "/politics/2024/07/02/election-2024-campaign-updates/",
        "wrap",
        null,
        7
    ),
    Tile(
        1.0,
        "print_edition",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/EU4VLA54I5HVFAQPJQFJCER3GY.png",
        "Today's paper",
        "omitted",
        "Print Edition",
        "https://www.washingtonpost.com/todays_paper/updates/",
        "wrap",
        null,
        8
    ),
    Tile(
        1.0,
        "the_7",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/X7K3GSVEEFBEHLHFS4KPSV6LCA.png",
        "The 7",
        "omitted",
        "Monday briefing: Trump shooting investigation; Biden’s Oval Office address; Euro 2024 final; Carlos Alcaraz; and more",
        "https://www.washingtonpost.com/the-seven/2025/09/05/what-to-know-for-september-5/",
        "scrolling",
        null,
        9
    ),
    Tile(
        1.0,
        "the_7",
        "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/X7K3GSVEEFBEHLHFS4KPSV6LCA.png",
        "The 7",
        "omitted",
        "Monday briefing: Trump shooting investigation; Biden’s Oval Office address; Euro 2024 final; Carlos Alcaraz; and more",
        "https://www.washingtonpost.com/the-seven/2024/07/15/what-to-know-for-july-15/",
        "wrap",
        null,
        11
    ),
    Tile(
        1.0,
        "games",
        "https://www.washingtonpost.com/wp-apps/imrs.php?src=https://www.washingtonpost.com/resizer/SkyX9JciHkMvwyAkSDVcSvdtBSQ=/arc-anglerfish-washpost-prod-washpost/public/AT6XC7XB3EI6ZLTENMR6KFK3MI.jpg&w=540",
        "Catch Up On",
        "omitted",
        "Games",
        "https://www.washingtonpost.com/games/",
        "wrap",
        null,
        12
    ),
    Tile(
        1.0,
        "no context label",
        "https://www.washingtonpost.com/wp-apps/imrs.php?src=https://s3.amazonaws.com/arc-authors/washpost/c8473889-4049-455b-91b5-95fa2dd69254.png&w=196&h=196",
        null,
        "omitted",
        "No context label",
        "https://www.washingtonpost.com/lifestyle/carolyn-hax/",
        "wrap",
        null,
        13
    )
)