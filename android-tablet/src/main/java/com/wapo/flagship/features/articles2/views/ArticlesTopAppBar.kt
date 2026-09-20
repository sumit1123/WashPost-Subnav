package com.wapo.flagship.features.articles2.views

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticlesTopAppBarInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesTopAppBarInteractionHelper
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesTopAppBar(
    currentUrl: String?,
    audioDuration: Long?,
    commentCount: String?,
    isCommentsAvailable: Boolean,
    isSummaryAvailable: Boolean,
    scrollProgress: Float,
    isSaved: Boolean,
    isInPlaylist: Boolean,
    articlesTopAppBarInteractionHelper: ArticlesTopAppBarInteractionHelper,
) {
    Column {
        TopAppBar(
            title = { },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = wpdsColors.gray700,
                ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                IconButton(
                    onClick = {
                        articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                            ArticlesTopAppBarInteractionEvent.BackClickEvent,
                        )
                    },
                ) {
                    Icon(
                        painter = painterResource(com.washingtonpost.android.R.drawable.ic_back_arrow),
                        contentDescription = "Back",
                        tint = wpdsColors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .retainLargestWidth()
                            .then(
                                if (audioDuration != null) {
                                    Modifier.clickable(
                                        onClickLabel = "Listen",
                                        role = Role.Button,
                                    ) {
                                        articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                            ArticlesTopAppBarInteractionEvent.ListenClickEvent,
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    audioDuration?.let {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_listen),
                                contentDescription = "Listen",
                                tint = wpdsColors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = AudioViewUtils.getDurationText(it / 1000, LocalContext.current),
                            style =
                                TextStyle(
                                    color = wpdsColors.primary,
                                    fontSize = 14.sp,
                                    lineHeight = 17.5.sp,
                                ),
                        )
                    }
                }

                if (isCommentsAvailable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .retainLargestWidth()
                                .clickable(
                                    onClickLabel = "Comments",
                                    role = Role.Button,
                                ) {
                                    articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                        ArticlesTopAppBarInteractionEvent.CommentsClickEvent,
                                    )
                                },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = com.washingtonpost.android.R.drawable.ic_comment),
                                contentDescription = "Comments",
                                tint = wpdsColors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        commentCount?.let {
                            Text(
                                text = it,
                                style =
                                    TextStyle(
                                        color = wpdsColors.primary,
                                        fontSize = 14.sp,
                                        lineHeight = 17.5.sp,
                                    ),
                            )
                        }
                    }
                }

                var expanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            painter = painterResource(id = com.wapo.flagship.features.aixp.R.drawable.ic_ellipsis),
                            contentDescription = "More options",
                            tint = wpdsColors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    DropdownMenu(
                        modifier =
                            Modifier
                                .background(wpdsColors.wallPrimaryBg)
                                .width(200.dp),
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        currentUrl?.let { url ->
                            if (isSummaryAvailable) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "AI Overview",
                                            style =
                                                TextStyle(
                                                    fontSize = 14.sp,
                                                    lineHeight = 17.5.sp,
                                                    fontFamily = FranklinItcStandardFontFamily,
                                                    color = wpdsColors.primary,
                                                ),
                                        )
                                    },
                                    onClick = {
                                        articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                            ArticlesTopAppBarInteractionEvent.SummaryClickEvent(url),
                                        )
                                        expanded = false
                                    },
                                    trailingIcon = {
                                        Icon(
                                            painter = painterResource(com.washingtonpost.android.R.drawable.summary_icon),
                                            contentDescription = "AI Overview",
                                            tint = wpdsColors.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                )
                                HorizontalDivider(color = wpdsColors.outline, thickness = 1.dp)
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Share",
                                        style =
                                            TextStyle(
                                                fontSize = 14.sp,
                                                lineHeight = 17.5.sp,
                                                fontFamily = FranklinItcStandardFontFamily,
                                                color = wpdsColors.primary,
                                            ),
                                    )
                                },
                                onClick = {
                                    articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                        ArticlesTopAppBarInteractionEvent.ShareClickEvent(url),
                                    )
                                    expanded = false
                                },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(com.washingtonpost.android.R.drawable.share),
                                        contentDescription = "Share",
                                        tint = wpdsColors.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                            HorizontalDivider(color = wpdsColors.outline, thickness = 1.dp)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text =
                                            if (isSaved) {
                                                "Remove from Saved Stories"
                                            } else {
                                                "Add to Saved Stories"
                                            },
                                        style =
                                            TextStyle(
                                                fontSize = 14.sp,
                                                lineHeight = 17.5.sp,
                                                fontFamily = FranklinItcStandardFontFamily,
                                                color = wpdsColors.primary,
                                            ),
                                    )
                                },
                                onClick = {
                                    articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                        ArticlesTopAppBarInteractionEvent.SaveClickEvent(url),
                                    )
                                    expanded = false
                                },
                                trailingIcon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isSaved) {
                                                    com.washingtonpost.android.R.drawable.ic_article_save_filled
                                                } else {
                                                    com.washingtonpost.android.R.drawable.ic_article_save
                                                },
                                            ),
                                        contentDescription =
                                            if (isSaved) {
                                                "Remove from Saved Stories"
                                            } else {
                                                "Add to Saved Stories"
                                            },
                                        tint = wpdsColors.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                            HorizontalDivider(color = wpdsColors.outline, thickness = 1.dp)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Gift",
                                        style =
                                            TextStyle(
                                                fontSize = 14.sp,
                                                lineHeight = 17.5.sp,
                                                fontFamily = FranklinItcStandardFontFamily,
                                                color = wpdsColors.primary,
                                            ),
                                    )
                                },
                                onClick = {
                                    articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                        ArticlesTopAppBarInteractionEvent.GiftClickEvent(url),
                                    )
                                    expanded = false
                                },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(com.washingtonpost.android.R.drawable.ic_gift),
                                        contentDescription = "Gift",
                                        tint = wpdsColors.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                        }
                        audioDuration?.let {
                            if (currentUrl != null) {
                                HorizontalDivider(color = wpdsColors.outline, thickness = 1.dp)
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text =
                                            if (isInPlaylist) {
                                                "Remove from playlist"
                                            } else {
                                                "Add to playlist"
                                            },
                                        style =
                                            TextStyle(
                                                fontSize = 14.sp,
                                                lineHeight = 17.5.sp,
                                                fontFamily = FranklinItcStandardFontFamily,
                                                color = wpdsColors.primary,
                                            ),
                                    )
                                },
                                onClick = {
                                    articlesTopAppBarInteractionHelper.onTopAppBarEventFired(
                                        ArticlesTopAppBarInteractionEvent.PlaylistClickEvent(isInPlaylist),
                                    )
                                    expanded = false
                                },
                                trailingIcon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isInPlaylist) {
                                                    com.wapo.zendesk.R.drawable.ic_clear
                                                } else {
                                                    R.drawable.ic_playlist
                                                },
                                            ),
                                        contentDescription =
                                            if (isInPlaylist) {
                                                "Remove from playlist"
                                            } else {
                                                "Add to playlist"
                                            },
                                        tint = wpdsColors.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            },
        )
        val animatedProgress by animateFloatAsState(
            targetValue = scrollProgress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 100, easing = LinearEasing),
            label = "scrollProgress",
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .background(wpdsColors.gray300)
                .offset(x = (-1).dp),
            color = wpdsColors.blue80,
            trackColor = wpdsColors.gray300,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun Modifier.retainLargestWidth(): Modifier {
    var largestWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    return widthIn(min = with(density) { largestWidthPx.toDp() })
        .onSizeChanged { size ->
            largestWidthPx = max(largestWidthPx, size.width)
        }
}

val dummyArticlesTopAppBarInteractionHelper =
    object : ArticlesTopAppBarInteractionHelper {
        override fun onTopAppBarEventFired(event: ArticlesTopAppBarInteractionEvent) {
            // no-op for Compose preview
        }
    }

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ArticlesTopAppBarPreview() {
    AndroidClassicTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = wpdsColors.surface,
        ) {
            ArticlesTopAppBar(
                currentUrl = "https://www.washingtonpost.com",
                audioDuration = 4500000L,
                commentCount = "500k",
                isCommentsAvailable = true,
                isSummaryAvailable = true,
                scrollProgress = .25f,
                isSaved = false,
                isInPlaylist = false,
                articlesTopAppBarInteractionHelper = dummyArticlesTopAppBarInteractionHelper,
            )
        }
    }
}
