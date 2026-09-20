package com.wapo.flagship.features.backendhealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.backendhealth.models.FailoverArticle
import com.wapo.flagship.features.backendhealth.models.FailoverState
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallbackScreen(
    state: FailoverState,
    onRetry: () -> Unit,
    onCallSite: () -> Unit,
    onArticleClicked: (FailoverArticle) -> Unit,
) {
    Logger.d("Fallback", "Shows: ${state.showFallbackPopUp}")
    if (state.showFallbackPopUp) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(wpdsColors.wallPrimaryBg),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            if (!state.isLoading && state.articles.isEmpty()) {
                FallbackDefaultContent(onRetry, onCallSite)
            }
            if (!state.isLoading && state.articles.isNotEmpty()) {
                PullToRefreshBox(
                    isRefreshing = state.healthCheckIsLoading,
                    onRefresh = onRetry,
                ) {
                    FallbackArticlesContent(state.articles, onArticleClicked)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FallbackDefaultContent(
    onRetry: () -> Unit,
    onCallSite: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .align(Alignment.Center)
                .wrapContentWidth(),
    ) {
        Text(
            text = "We are currently experiencing\ntechnical difficulties",
            style = MaterialTheme.typography.h2.copy(color = wpdsColors.gray20),
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally),
        )
        Text(
            text = "Please try again or visit our web site",
            style =
                TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 14.sp,
                    color = wpdsColors.gray20,
                ),
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
        )
        OutlinedButton(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(150.dp)
                    .padding(top = 14.dp),
            border = BorderStroke(1.5.dp, color = wpdsColors.gray20),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = wpdsColors.wallPrimaryBg,
                ),
            onClick = { onRetry() },
        ) {
            Text(
                text = "Retry",
                style =
                    TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 13.sp,
                        color = wpdsColors.gray20,
                    ),
                fontWeight = FontWeight.SemiBold,
            )
        }
        OutlinedButton(
            modifier =
                Modifier
                    .width(150.dp)
                    .align(Alignment.CenterHorizontally),
            border = BorderStroke(1.5.dp, color = wpdsColors.gray20),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = wpdsColors.wallPrimaryBg,
                ),
            onClick = { onCallSite() },
        ) {
            Text(
                text = "Visit our site",
                style =
                    TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 13.sp,
                        color = wpdsColors.gray20,
                    ),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FallbackArticlesContent(
    articles: List<FailoverArticle>,
    onArticleClicked: (FailoverArticle) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(wpdsColors.gridCardBg),
        contentPadding = PaddingValues(16.dp),
    ) {
        itemsIndexed(articles) { index, article ->
            FailoverArticleContent(
                modifier = Modifier.fillMaxWidth(),
                item = article,
                onClicked = { onArticleClicked(article) }
            )
            if (index != articles.lastIndex) {
                Divider(color = wpdsColors.faint)
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun FailoverArticleContent(
    modifier: Modifier = Modifier,
    item: FailoverArticle,
    onClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(wpdsColors.gridCardBg)
            .clickable { onClicked() }
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                item.headline,
                style = TextStyle(
                    color = wpdsColors.primary,
                    fontFamily = PostiniFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                item.bylineText,
                style = TextStyle(
                    color = wpdsColors.gray80,
                    fontFamily = PostiniFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            )
        }
        Spacer(Modifier.width(8.dp))
        GlideImage(
            modifier = Modifier
                .size(106.dp, 90.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(4.dp)),
            model = item.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
    }
}

@PreviewLightDark
@Composable
fun FallbackPopUpDefaultPreview() {
    AndroidClassicTheme {
        FallbackScreen(
            FailoverState(),
            {},
            {},
            {}
        )
    }
}

@PreviewLightDark
@Composable
fun FallbackPopUpArticlesPreview() {
    AndroidClassicTheme {
        FallbackScreen(
            FailoverState(
                articles = listOf(
                    FailoverArticle(
                        headline = "Trump put allies on obscure board set to decide White House ballroom's fate",
                        byline = "Jonathan Edwards",
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/RSX62GSI4NP23OOFVHXFGGDNGE.JPG",
                        contentUrl = "https://www.washingtonpost.com/politics/2025/10/28/trump-ballroom-planning-commission/",
                        publishTime = 1761642004000,
                    ),
                    FailoverArticle(
                        headline = "Hurricane Melissa hits Cuba after ripping through Jamaica",
                        byline = "Livern Barrett",
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/RSX62GSI4NP23OOFVHXFGGDNGE.JPG",
                        contentUrl = "https://www.washingtonpost.com/politics/2025/10/28/trump-ballroom-planning-commission/",
                        publishTime = 1761642004000,
                    )
                ),
                healthCheckIsLoading = false
            ),
            {},
            {},
            {}
        )
    }
}
