package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.ContextBoxUiModel.ContextBoxPageUiModel
import com.wapo.flagship.features.articles3.models.ui.ContextBoxUiModel
import com.wapo.flagship.features.articles3.models.ui.ListUiModel
import com.wapo.flagship.features.articles3.models.ui.SanitizedHtmlUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.launch

@Composable
fun ContextBoxView(
    uiModel: ContextBoxUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val pagerState = rememberPagerState(pageCount = { uiModel.pages.size })
    val coroutineScope = rememberCoroutineScope()
    val contextBoxStyle = getContextBoxStyle(uiModel.uiStyle)
    val pageHeightsPx = remember(uiModel.pages.size) { mutableStateMapOf<Int, Int>() }
    val tallestPagePx = pageHeightsPx.values.maxOrNull()?.takeIf { it > 0 }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Divider top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(wpdsColors.outline)
                .padding(vertical = 1.dp)
        )

        // Headline
        if (uiModel.headline.isNotEmpty()) {
            Text(
                text = uiModel.headline,
                style = contextBoxStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // Pager with pages
        if (uiModel.pages.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .let { modifier ->
                        tallestPagePx?.let { with(density) { modifier.height(it.toDp()) } }
                            ?: modifier.wrapContentHeight()
                    }
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (size.height > 0) {
                                pageHeightsPx[page] = size.height
                            }
                        }
                ) {
                    ContextBoxPageContent(uiModel.pages[page], articlesInteractionHelper)
                }
            }

            // Tab indicators (dots)
            if (uiModel.pages.size > 1) {
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    containerColor = wpdsColors.surface,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    repeat(uiModel.pages.size) { index ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            modifier = Modifier.height(8.dp).width(8.dp),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .height(8.dp).width(8.dp)
                                        .background(
                                            if (pagerState.currentPage == index)
                                                wpdsColors.primary
                                            else
                                                wpdsColors.outline,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .padding(8.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Divider bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(wpdsColors.outline)
                .padding(vertical = 1.dp)
        )
    }
}

@Composable
private fun ContextBoxPageContent(
    page: ContextBoxPageUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        page.contentElements.forEach { element ->
            when (element) {
                is SanitizedHtmlUiModel -> SanitizedHtmlView(element, articlesInteractionHelper)
                is ListUiModel -> ListView(element, articlesInteractionHelper)
                else -> {
                    Logger.w("ContextBoxView", "Unsupported content type: $element")
                }
            }
        }
    }
}

@Composable
private fun getContextBoxStyle(uiStyle: ContextBoxUiStyle): TextStyle {
    return when (uiStyle) {
        ContextBoxUiStyle.DEFAULT -> TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = wpdsColors.onSurface
        )
    }
}

enum class ContextBoxUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun ContextBoxViewPreview() {
    AndroidClassicTheme {
        ContextBoxView(
            uiModel = ContextBoxUiModel(
                headline = "Related context",
                pages = listOf(
                    ContextBoxPageUiModel(
                        contentElements = listOf(
                            SanitizedHtmlUiModel(
                                content = "This is sample context box content",
                                questionSets = null,
                                sourceAnnotations = null,
                                oEmbed = null,
                                uiStyle = SanitizedHtmlUiStyle.DEFAULT
                            )
                        )
                    ),
                    ContextBoxPageUiModel(
                        contentElements = listOf(
                            SanitizedHtmlUiModel(
                                content = "This is another page of context",
                                questionSets = null,
                                sourceAnnotations = null,
                                oEmbed = null,
                                uiStyle = SanitizedHtmlUiStyle.DEFAULT
                            )
                        )
                    )
                )
            ),
            dummyArticlesInteractionHelper
        )
    }
}
