package com.wapo.flagship.features.sections

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapo.android.commons.util.px
import com.wapo.flagship.features.grid.toDp
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel
import com.washingtonpost.android.sections.R
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

private val INDICATOR_HEIGHT = Dp(4.0F)
private const val NEWSPRINT_BUNDLE_FANCY = "/classic-apps/classic-app-newsprint"

@Composable
fun SectionsRibbon(
    viewModel: SectionsRibbonViewModel,
    onSectionTapped: (Int) -> Unit,
    onCustomNavTapped: () -> Unit
) {
    val ribbonBgColor = wpdsColors.appBarBg
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isVisible && !uiState.isLowDataModeEnable) {
        Box( // Wrapper to allow Custom Nav button to float over Ribbon
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.background(ribbonBgColor)
        ) {
            Divider(
                color = wpdsColors.gray400,
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            LazyRow( // Horizontally-scrollable Ribbon
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 12.dp)
            ) {
                itemsIndexed(uiState.sections) { index, section ->
                    val widthPixels = determineSectionTextWidth(section.displayName ?: section.name)

                    val isSelected = uiState.selectedSectionIndex == index

                    Box {
                        SectionItem(
                            index,
                            section,
                            isSelected,
                            widthPixels,
                            uiState.visitedNewsprintSection,
                            onSectionTapped
                        )

                        if (isSelected) {
                            SelectionIndicator(
                                section,
                                widthPixels
                            )
                        }
                    }
                }

                if (uiState.shouldShowCustomNav) {
                    item {
                        CustomizeNavItem(onCustomNavTapped)
                    }
                }
            }
        }

        LaunchedEffect(key1 = uiState.selectedSectionIndex) {
            uiState.selectedSectionIndex.let {
                listState.animateScrollAndCenterItem(it)
            }
        }
    }
}

@Composable
private fun SectionItem(
    index: Int,
    section: Section,
    isSelected: Boolean,
    widthPixels: Float,
    viewedNewsprintSection: Boolean,
    onSectionTapped: (Int) -> Unit
) {
    val bottomPadding = 15.dp

    Tab(
        content = {
            Text(
                text = section.displayName ?: section.name,
                style = TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 16.sp,
                    letterSpacing = 0.25.sp,
                    brush = determineGradient(section.id, widthPixels, viewedNewsprintSection)
                ),
                fontWeight = determineFontWeight(isSelected, section.id),
                color = if (isSelected) wpdsColors.gray0 else wpdsColors.sectionRibbonNonSelectedText,
                modifier = Modifier
                    .padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = bottomPadding)
            )
        },
        selected = isSelected,
        onClick = { onSectionTapped(index) }
    )
}

@Composable
private fun BoxScope.SelectionIndicator(section: Section, widthPixels: Float) {
    Box(
        modifier = Modifier
            .background(wpdsColors.gray40)
            .width(with(LocalDensity.current) { widthPixels.toDp() })
            .height(INDICATOR_HEIGHT)
            .align(Alignment.BottomCenter)
    ) {
        val brush = determineGradient(section.id, widthPixels)
        if (brush != null) {
            Canvas(
                modifier = Modifier.matchParentSize(),
                onDraw = {
                    drawRect(brush)
                }
            )
        }
    }
}

@Composable
private fun CustomizeNavItem(onCustomNavTapped: () -> Unit) {
    Box(
        modifier = Modifier.padding(
            top = with(LocalDensity.current) { 10.sp.toDp() },
            end = 24.dp
        )
    ) {
        val onePixelAsDp = 1.px.toDp(LocalDensity.current.density).dp
        OutlinedButton(
            onClick = { onCustomNavTapped() },
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
            border = BorderStroke(
                onePixelAsDp,
                wpdsColors.gray0
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(17.dp),
            contentPadding = PaddingValues(all = 0.dp)
        ) {
            Icon(
                painter = painterResource(id = com.wpds.wpds.R.drawable.add),
                contentDescription = "Menu Preferences",
                tint = wpdsColors.gray0,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun determineFontWeight(isSelected: Boolean, sectionId: String): FontWeight {
    return if (isSelected || sectionId == NEWSPRINT_BUNDLE_FANCY) {
        FontWeight.Bold
    } else {
        FontWeight.Normal
    }
}

@Composable
private fun determineGradient(
    sectionId: String,
    textWidth: Float,
    viewedNewsprintSection: Boolean = true
): Brush? {
    if (sectionId != NEWSPRINT_BUNDLE_FANCY) return null

    return if (!viewedNewsprintSection) {
        // Animated gradient
        val transition = rememberInfiniteTransition(label = "NewsprintAnimation")
        val offset by transition.animateFloat(
            initialValue = -textWidth,
            targetValue = textWidth,
            animationSpec = infiniteRepeatable(
                tween(
                    durationMillis = 3400, // Animation duration
                    easing = LinearEasing
                )
            ),
            label = "NewsprintAnimation"
        )

        Brush.linearGradient(
            colors = listOf(
                wpdsColors.blue100Static,
                wpdsColors.purple100Static,
                wpdsColors.purple100Static,
                wpdsColors.blue100Static
            ),
            start = Offset(x = offset, y = 0f),
            end = Offset(x = textWidth + offset, y = 0f)
        )
    } else {
        // Static gradient
        Brush.linearGradient(
            colors = listOf(
                wpdsColors.blue100Static,
                wpdsColors.purple100Static
            )
        )
    }
}

@Composable
private fun determineSectionTextWidth(sectionName: String): Float {
    val textMeasurer = rememberTextMeasurer()
    val selectedTextStyle = TextStyle(
        fontFamily = FranklinItcStandardFontFamily,
        fontSize = 16.sp,
        letterSpacing = 0.25.sp,
        fontWeight = FontWeight.Bold
    )
    return textMeasurer.measure(
        sectionName,
        selectedTextStyle
    ).size.width.toFloat()
}

/**
 * Scrolls to the selected item and centers it.
 *  Overrides the default behavior which is to scroll to the selected item at the start of the row (left edge).
 * If visibleItemsInfo is empty, i.e. during initialization, do not animate.
 *  Avoids hiding what's left of Top Stories on app launch.
 */
private suspend fun LazyListState.animateScrollAndCenterItem(index: Int) {
    if (index < 0) return

    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo != null) {
        val center = layoutInfo.viewportEndOffset / 2
        val childCenter = itemInfo.offset + itemInfo.size / 2
        animateScrollBy((childCenter - center).toFloat())
    } else if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
        animateScrollToItem(index)
    }
}

fun sectionsRibbonSetContent(
    composeView: ComposeView,
    viewModel: SectionsRibbonViewModel,
    onSectionTapped: (Int) -> Unit,
    onCustomNavTapped: () -> Unit
) {
    composeView.setContent {
        SectionsRibbon(
            viewModel = viewModel,
            onSectionTapped = onSectionTapped,
            onCustomNavTapped = onCustomNavTapped
        )
    }
}

@Composable
@Preview(showBackground = true)
fun CustomizeNavItemPreview() {
    CustomizeNavItem {}
}
