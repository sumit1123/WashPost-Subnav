package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles3.models.SubNavStrip
import com.wapo.flagship.features.articles3.models.SubNavTabsLoader
import com.wapo.flagship.features.articles3.models.ui.SubNavTabUiModel
import com.wapo.flagship.features.articles3.models.ui.SubNavUiModel
import com.wapo.flagship.features.articles3.models.ui.WebEmbedUiModel
import com.wapo.flagship.util.tracking.Measurement
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import com.wpds.utils.IconUtils

// ============================================================================
// STYLE ENUM
// ============================================================================

enum class SubNavUiStyle {
    DEFAULT
}

// ============================================================================
// VIEW
// ============================================================================

/**
 * Horizontally scrolling nav strip with a content panel underneath.
 *
 * The chips are not in the article payload — they are fetched from [SubNavUiModel.tabsUrl] the
 * first time the element composes, so the newsroom controls them without an app release. Until a
 * chip is picked the panel shows [SubNavUiModel.defaultContentUrl]; picking one swaps the panel to
 * that chip's own url.
 *
 * Both states render through [WebEmbedView], so the tab content reuses the existing WebView pool,
 * height-reporting and theming rather than introducing a second embed path. Each url gets its own
 * pool key, so switching tabs back and forth does not reload.
 */
@Composable
fun SubNavView(
    index: Int,
    uiModel: SubNavUiModel,
    onArticleInteractionEvent: (ArticleInteractionEvent) -> Unit,
    webEmbedSettings: WebEmbedSettings,
) {
    val strip by produceState(initialValue = SubNavStrip.EMPTY, key1 = uiModel.tabsUrl) {
        val url = uiModel.tabsUrl
        value = if (url.isNullOrBlank()) SubNavStrip.EMPTY else SubNavTabsLoader.load(url)
    }

    // -1 means "nothing picked yet" -> show the element's own embed.
    var selectedIndex by rememberSaveable(uiModel.tabsUrl) { mutableStateOf(-1) }

    val selectedTab = strip.tabs.getOrNull(selectedIndex)
    val contentUrl = selectedTab?.contentUrl ?: uiModel.defaultContentUrl

    Column(modifier = Modifier.fillMaxWidth()) {

        if (!strip.isEmpty) {
            SubNavStripRow(
                strip = strip,
                selectedIndex = selectedIndex,
                onTabSelected = { tappedIndex, tab ->
                    // Selecting is local state only — the panel below swaps, we do not navigate.
                    selectedIndex = if (selectedIndex == tappedIndex) -1 else tappedIndex
                    tab.behavior?.let { Measurement.trackSubNavItemClick(it) }
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(wpdsColors.divider)
            )
        }

        if (!contentUrl.isNullOrBlank()) {
            WebEmbedView(
                // The pool key is "embed_<index>_<url>_…", so passing this element's own index is
                // enough — each tab has a distinct url and therefore a distinct WebView, which is
                // what lets switching tabs back and forth avoid a reload.
                index = index,
                uiModel = WebEmbedUiModel(
                    url = contentUrl,
                    oembed = null,
                    subtype = loadableSubtype(selectedTab?.subtype ?: uiModel.subtype),
                    widthFactor = uiModel.widthFactor,
                    uiStyle = WebEmbedUiStyle.DEFAULT,
                ),
                onArticleInteractionEvent = onArticleInteractionEvent,
                webEmbedSettings = webEmbedSettings,
            )
        }
    }
}

@Composable
private fun SubNavStripRow(
    strip: SubNavStrip,
    selectedIndex: Int,
    onTabSelected: (Int, SubNavTabUiModel) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        strip.sectionLabel?.let { label ->
            item(key = "sub_nav_section_label") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubNavIcon(iconName = strip.sectionIconName)
                    Text(
                        text = label,
                        style = sectionLabelStyle(),
                        color = wpdsColors.primary,
                    )
                }
            }
        }

        itemsIndexed(items = strip.tabs, key = { _, tab -> tab.id }) { tabIndex, tab ->
            val isSelected = tabIndex == selectedIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = tab.contentUrl != null) { onTabSelected(tabIndex, tab) }
                    .padding(vertical = 4.dp),
            ) {
                SubNavIcon(iconName = tab.iconName)
                Text(
                    text = tab.label,
                    style = tabStyle(isSelected),
                    // Design shows unselected chips in the same near-black as the label, not grey.
                    color = wpdsColors.primary,
                )
            }
        }
    }
}

/**
 * Maps a feed subtype onto one [WebEmbedView] will actually load.
 *
 * `loadEmbed` only calls `loadUrl` for the "datawrapper" and "partial" subtypes — every other
 * subtype without an `oembed` payload silently loads nothing and collapses to an empty box. Sub-nav
 * panels are always plain urls (e.g. the "elex" components), so they map to "partial", which is the
 * generic load-this-url path. "datawrapper" is preserved because it carries its own scroll and
 * navigation handling in [EmbedWebViewClient].
 *
 * Doing this here rather than adding an `else` branch to `loadEmbed` keeps the change off every
 * other embed in the article.
 */
private fun loadableSubtype(feedSubtype: String?): String =
    if (feedSubtype == "datawrapper") "datawrapper" else "partial"

/**
 * Feed-driven icon (e.g. the elections flag). Renders nothing when the feed names no icon or the
 * name does not resolve to a drawable, so an unknown icon costs a gap rather than a crash.
 */
@Composable
private fun SubNavIcon(iconName: String?) {
    if (iconName.isNullOrBlank()) return
    val context = LocalContext.current
    val drawableId = remember(iconName) { IconUtils(context).getDrawableId(iconName) } ?: return

    Icon(
        painter = painterResource(id = drawableId),
        contentDescription = null,
        // Unspecified keeps the flag's own red/blue instead of flattening it to the text colour.
        tint = Color.Unspecified,
        modifier = Modifier
            .size(16.dp)
            .padding(end = 6.dp),
    )
}

// ============================================================================
// TEXT STYLES
// ============================================================================

@Composable
private fun sectionLabelStyle(): TextStyle = TextStyle(
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.4.sp,
)

@Composable
private fun tabStyle(isSelected: Boolean): TextStyle = TextStyle(
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
    fontSize = 13.5.sp,
    textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None,
)

// ============================================================================
// PREVIEW
// ============================================================================

@Preview(showBackground = true)
@Composable
private fun SubNavStripRowPreview() {
    AndroidClassicTheme {
        SubNavStripRow(
            strip = SubNavStrip(
                sectionLabel = "Election 2024",
                tabs = listOf(
                    SubNavTabUiModel("1", "Find results", "https://example.com/a", "search"),
                    SubNavTabUiModel("2", "Live updates", "https://example.com/b", "luf"),
                    SubNavTabUiModel("3", "Balance of power", "https://example.com/c"),
                ),
            ),
            selectedIndex = 1,
            onTabSelected = { _, _ -> },
        )
    }
}
