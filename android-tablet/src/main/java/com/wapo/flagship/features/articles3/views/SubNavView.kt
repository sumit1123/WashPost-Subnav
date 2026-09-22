package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles3.models.SubNavStrip
import com.wapo.flagship.features.articles3.models.SubNavTabsLoader
import com.wapo.flagship.features.articles3.models.ui.SubNavTabUiModel
import com.wapo.flagship.features.articles3.models.ui.SubNavUiModel
import com.wapo.flagship.features.articles3.models.ui.WebEmbedUiModel
import com.wapo.flagship.features.grid.ComponentSize
import com.wapo.flagship.features.pagebuilder.getSize
import com.wapo.flagship.util.tracking.Measurement
import kotlinx.coroutines.flow.flowOf
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import com.wpds.utils.IconUtils

enum class SubNavUiStyle {
    DEFAULT
}

@Composable
fun SubNavView(
    index: Int,
    uiModel: SubNavUiModel,
    onArticleInteractionEvent: (ArticleInteractionEvent) -> Unit,
    webEmbedSettings: WebEmbedSettings,
) {
    val context = LocalContext.current

    // ConfigManager emits the local copy immediately and the remote one when it downloads, so a
    // single collect covers both phases. The subscription is released when this leaves
    // composition.
    val tabsUrl = uiModel.tabsUrl
    val strip by remember(tabsUrl) {
        if (tabsUrl.isNullOrBlank()) flowOf(SubNavStrip.EMPTY)
        else SubNavTabsLoader.strips(context, tabsUrl)
    }.collectAsState(initial = SubNavStrip.EMPTY)

    // The feed's render sizes for the panel. Without them the panel grows to whatever the tab's
    // page measures, which for a full page (live updates) is the whole document.
    val panelHeight = rememberPanelHeight(uiModel.panelSizes)

    // Tracked by id, not index: the chip list is replaced when the remote strip arrives, and an
    // index would then silently point at a different chip. null means "nothing picked yet".
    var selectedTabId by rememberSaveable(tabsUrl) { mutableStateOf<String?>(null) }

    // Search dropdown children too: picking "Arizona" stores that child's id, which is not in
    // strip.tabs, so a top-level-only lookup would miss it and leave the panel empty.
    val selectedTab = strip.tabs.firstNotNullOfOrNull { tab ->
        tab.takeIf { it.id == selectedTabId }
            ?: tab.children.firstOrNull { it.id == selectedTabId }
    }
    val contentUrl = selectedTab?.contentUrl

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!strip.isEmpty) {
            SubNavStripRow(
                strip = strip,
                selectedTabId = selectedTabId,
                onTabSelected = { tab ->
                    val isDeselect = selectedTabId == tab.id
                    selectedTabId = if (isDeselect) null else tab.id
                    // Tapping the active chip clears the selection; that is not a selection
                    // event, so only the selecting tap is reported.
                    if (!isDeselect) {
                        tab.behavior?.let { Measurement.trackSubNavItemClick(it) }
                    }
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
                    subtype = loadableSubtype(selectedTab?.subtype),
                    // WebEmbedView does not read widthFactor; the strip's panel is always
                    // full width, so there is nothing for the element to supply.
                    widthFactor = null,
                    uiStyle = WebEmbedUiStyle.DEFAULT,
                ),
                onArticleInteractionEvent = onArticleInteractionEvent,
                webEmbedSettings = webEmbedSettings,
                fixedHeight = panelHeight,
            )
        }
    }
}

/**
 * The height the feed asks for at this screen size, in dp.
 *
 * The size is picked with the same nearest-match the section fronts use for their web components,
 * so a feed that lists one size per device class lands on the same entry here. Reading the
 * configuration makes the pick again after a rotation or a resize. null -- no sizes, or a size
 * without a usable height -- leaves the panel measuring its own content.
 */
@Composable
private fun rememberPanelHeight(sizes: List<ComponentSize>): Dp? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(sizes, configuration) {
        sizes.takeIf { it.isNotEmpty() }
            ?.getSize(context)
            ?.height
            ?.takeIf { it > 0 }
            ?.dp
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

@Composable
private fun SubNavStripRow(
    strip: SubNavStrip,
    selectedTabId: String?,
    onTabSelected: (SubNavTabUiModel) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = SubNavTokens.STRIP_VERTICAL_PADDING,
        ),
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

        items(items = strip.tabs, key = { it.id }) { tab ->
            if (tab.isDropdown) {
                SubNavDropdownChip(
                    tab = tab,
                    selectedTabId = selectedTabId,
                    onTabSelected = onTabSelected,
                )
            } else {
                SubNavChip(
                    tab = tab,
                    isSelected = tab.id == selectedTabId,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun SubNavChip(
    tab: SubNavTabUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    role: Role = Role.Tab,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            // selectable reports the selected state to TalkBack, which the bold + underline
            // alone does not. The strip's visible height comes from the LazyRow's padding, so
            // the minimum buys a 48dp touch target without making the strip taller.
            .selectable(
                selected = isSelected,
                enabled = tab.contentUrl != null || tab.isDropdown,
                role = role,
                onClick = onClick,
            )
            .heightIn(min = SubNavTokens.CHIP_MIN_HEIGHT),
    ) {
        SubNavIcon(iconName = tab.iconName)
        Text(
            text = tab.label,
            style = tabStyle(isSelected),
            color = wpdsColors.primary,
        )
        trailing()
    }
}

/**
 * A chip whose feed entry has nested children — the design's "Results by State ⌄". Tapping opens
 * a menu of the children; picking one moves the selection to that child, so the parent itself
 * is never the selection.
 */
@Composable
private fun SubNavDropdownChip(
    tab: SubNavTabUiModel,
    selectedTabId: String?,
    onTabSelected: (SubNavTabUiModel) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Hoisted out of DropdownMenu so the scrollbar can read the same scroll position.
    val menuScrollState = rememberScrollState()
    // The parent reads as selected while any of its children is the selected chip.
    val isSelected = tab.children.any { it.id == selectedTabId }

    Box {
        SubNavChip(
            tab = tab,
            isSelected = isSelected,
            onClick = { expanded = true },
            role = Role.DropdownList,
            trailing = {
                // ArrowDropUp lives in material-icons-extended, which this module does not
                // pull in; rotating the down caret avoids adding the dependency for one glyph.
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = wpdsColors.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = menuScrollState,
            modifier = Modifier
                .background(SubNavTokens.MENU_BACKGROUND)
                .heightIn(max = SubNavTokens.MENU_MAX_HEIGHT)
                .verticalScrollbar(menuScrollState),
        ) {
            tab.children.forEach { child ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = child.label,
                            style = tabStyle(child.id == selectedTabId),
                            color = SubNavTokens.MENU_TEXT,
                        )
                    },
                    onClick = {
                        expanded = false
                        onTabSelected(child)
                    },
                )
            }
        }
    }
}

/** Material3's [DropdownMenu] gives no scroll affordance, so a thumb is drawn over the menu. */
private fun Modifier.verticalScrollbar(state: ScrollState): Modifier = drawWithContent {
    drawContent()

    val max = state.maxValue
    if (max == 0 || max == Int.MAX_VALUE) return@drawWithContent

    val viewportHeight = size.height
    val contentHeight = viewportHeight + max
    val thumbHeight = (viewportHeight / contentHeight) * viewportHeight
    val thumbOffsetY = (state.value.toFloat() / max) * (viewportHeight - thumbHeight)
    val thumbWidth = SubNavTokens.SCROLLBAR_WIDTH.toPx()

    drawRoundRect(
        color = SubNavTokens.SCROLLBAR,
        topLeft = Offset(size.width - thumbWidth, thumbOffsetY),
        size = Size(thumbWidth, thumbHeight),
        cornerRadius = CornerRadius(thumbWidth / 2f),
    )
}

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
        tint = Color.Unspecified,
        modifier = Modifier
            .size(16.dp)
            .padding(end = 6.dp),
    )
}

/** Design tokens for the strip, kept together so the whole look can be read in one place. */
private object SubNavTokens {

    val TEXT_SIZE = 16.sp
    val MENU_BACKGROUND = Color(0xFFF7F7F7)
    val MENU_TEXT = Color(0xFF1A1A1A)
    val MENU_MAX_HEIGHT = 280.dp
    val CHIP_MIN_HEIGHT = 48.dp
    val STRIP_VERTICAL_PADDING = 6.dp
    val SCROLLBAR = Color(0x66000000)
    val SCROLLBAR_WIDTH = 3.dp
}

@Composable
private fun sectionLabelStyle(): TextStyle = TextStyle(
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = SubNavTokens.TEXT_SIZE,
)

@Composable
private fun tabStyle(isSelected: Boolean): TextStyle = TextStyle(
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
    fontSize = SubNavTokens.TEXT_SIZE,
    textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None,
)

