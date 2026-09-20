// Copyright (c) 2020 The Washington Post. All rights reserved.

package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.DividerStyle
import com.wapo.flagship.features.grid.Tracking

data class Grid(
    val regions: MutableList<Region> = mutableListOf(),
    val tracking: Tracking? = null,
    val checksum: String? = null,
    val cards: Cards? = null,
)

data class Region(
    var location: String?,
    var items: MutableList<Chain> = mutableListOf(),
)

class Table : Cell() {
    var id: String? = null
    var resolvedColumn = -1
    var resolvedRow = -1
    var resolvedRowSpan = 1
    var resolvedColumnSpan = 1
    var isSeparator: Boolean = false

    var items: MutableList<Item> = mutableListOf()
    var label: CompoundLabel? = null
    var cta: CompoundLabel? = null

    var dividers: MutableMap<String, Dividers> = mutableMapOf()
    var cardSegmentType: CardSegmentType = CardSegmentType.UNASSIGNED

    fun getDividers(screenSizeLayout: ScreenSizeLayout): Dividers? =
        when (screenSizeLayout) {
            ScreenSizeLayout.XLARGE -> dividers["xlarge"]
            ScreenSizeLayout.LARGE -> dividers["large"]
            ScreenSizeLayout.MEDIUM -> dividers["medium"]
            ScreenSizeLayout.SMALL -> dividers["small"]
            ScreenSizeLayout.XSMALL -> dividers["xsmall"]
        }

    fun reset() {
        resolvedColumn = -1
        resolvedRow = -1
        resolvedColumnSpan = -1
        resolvedRowSpan = -1
        resolvedColumnSpan = 1
        resolvedRowSpan = 1
        isSeparator = false
        cardSegmentType = CardSegmentType.UNASSIGNED
    }
}

class Chain {
    var id: String? = null
    var layoutAttributes: LayoutAttributes? = null
    var items: MutableList<Table> = mutableListOf()
    var label: CompoundLabel? = null
    var cta: CompoundLabel? = null
    var dividers: MutableMap<String, Dividers> = mutableMapOf()
    var cardSegmentType: CardSegmentType = CardSegmentType.UNASSIGNED
    var isSeparator: Boolean = false
    var displayContext: MutableList<String> = mutableListOf()

    fun getDividers(screenSizeLayout: ScreenSizeLayout): Dividers? =
        when (screenSizeLayout) {
            ScreenSizeLayout.XLARGE -> dividers["xlarge"]
            ScreenSizeLayout.LARGE -> dividers["large"]
            ScreenSizeLayout.MEDIUM -> dividers["medium"]
            ScreenSizeLayout.SMALL -> dividers["small"]
            ScreenSizeLayout.XSMALL -> dividers["xsmall"]
        }
}

open class Item(
    var id: String? = null,
    var tableId: String? = null,
    var chainId: String? = null,
    /**
     * Column assigned according to the current screen type
     */
    var resolvedColumn: Int = -1,
    /**
     * Row assigned according to the current screen type
     */

    var resolvedRow: Int = -1,
    /**
     * Column assigned according to the current screen type
     */
    /**
     * Column span assigned according to the current screen type
     */
    var resolvedColumnSpan: Int = -1,
    /**
     * Row span assigned according to the current screen type
     */
    var resolvedRowSpan: Int = -1,
    var adapterPosition: Int = -1,
    var cardSegmentType: CardSegmentType = CardSegmentType.UNASSIGNED,
    var gradientColors: Pair<Int, Int>? = null,
    var cardIndex: Int? = null,
    var isNewsprint: Boolean = false,
) : Cell() {
    fun reset() {
        resolvedColumn = -1
        resolvedRow = -1
        resolvedColumnSpan = -1
        resolvedRowSpan = -1
        adapterPosition = -1
        cardSegmentType = CardSegmentType.UNASSIGNED
    }
}

/**
 * Wrapper class to put label into table items
 */
class LabelItem(
    val compoundLabel: CompoundLabel,
    val forceLeftAlignOnExtraSmall: Boolean = false,
) : Item()

/**
 * Wrapper class to put form into table items
 */
class FormWrapperItem(
    val form: Form,
) : Item()

open class Feature(
    var item: List<Item> = mutableListOf(),
) : Item()

abstract class Cell {
    lateinit var layoutAttributes: LayoutAttributes

    var bleed: Bleed = Bleed.NONE
    var bleedItemType: BleedItemType = BleedItemType.NONE
    var forceFullBleed = false

    fun gridLocation(screenWidth: Int): GridLocation? = layoutAttributes.mapScreenToLocation(BreakPoints.getScreenSizeLayout(screenWidth))

    fun getLocation(screenSizeLayout: ScreenSizeLayout): GridLocation? = layoutAttributes.mapScreenToLocation(screenSizeLayout)
}

class Dividers {
    val vertical: MutableList<DividerLayout> = mutableListOf()
    val horizontal: MutableList<DividerLayout> = mutableListOf()
}

class DividerLayout {
    var column = -1
    var row = -1
    var span = -1
    var rowSpan = -1
    var style: DividerStyle? = null
}

data class GridLocation(
    var span: Int = 0,
    var row: Int = 0,
    var column: Int = 0,
    var rowSpan: Int = 1,
)

data class Separator(
    var size: SeparatorSize,
    var line: Boolean = false,
    var betweenCards: Boolean = false,
    var insideCard: Boolean = false,
) : Item()

enum class SeparatorSize {
    XSMALL,
    SMALL,
    LARGE,
}

class Card(
    var items: MutableList<Item> = mutableListOf(),
)

data class Cards(
    val extraSmall: CardLayout?,
)

data class CardLayout(
    val grids: List<List<String>>?,
    val tables: List<List<String>>?,
    val features: List<List<String>>?,
)

class LayoutAttributes {
    var extraLarge: GridLocation? = null
    var large: GridLocation? = null
    var medium: GridLocation? = null
    var small: GridLocation? = null
    var extraSmall: GridLocation? = null

    fun mapScreenToLocation(screenSizeLayout: ScreenSizeLayout): GridLocation? =
        when (screenSizeLayout) {
            ScreenSizeLayout.XLARGE -> extraLarge
            ScreenSizeLayout.LARGE -> large
            ScreenSizeLayout.MEDIUM -> medium
            ScreenSizeLayout.SMALL -> small
            ScreenSizeLayout.XSMALL -> extraSmall
        }
}

/**
 * Screen break points map describing what screen width falls into which category (XSMALL, SMALL, MEDIUM, LARGE, or XLARGE)
 */
object BreakPoints {
    private val breakPointMap =
        listOf(
            GridSpec(1352, Int.MAX_VALUE, 20, ScreenSizeLayout.XLARGE), // contentWidth(1288)+margin(64)=1352
            GridSpec(1088, 1351, 16, ScreenSizeLayout.LARGE), // contentWidth(1024)+margin(64)=1088
            GridSpec(824, 1087, 12, ScreenSizeLayout.MEDIUM), // contentWidth(760)+margin(64)=824
            GridSpec(676, 823, 10, ScreenSizeLayout.SMALL), // contentWidth(628)+margin(48)=676
            GridSpec(0, 675, 1, ScreenSizeLayout.XSMALL),
        )

    private const val singleColumnMargin = 16
    private const val columnWidth = 34
    private const val gutterWidth = 32

    fun getGridSpec(width: Int): GridSpec =
        breakPointMap.find {
            width >= it.lowerScreenWidth && width <= it.upperScreenWidth
        } ?: breakPointMap[0]

    fun getColumnCount(width: Int): Int = getGridSpec(width).columnCount

    fun getColumnCount(screenSizeLayout: ScreenSizeLayout): Int =
        breakPointMap
            .find {
                screenSizeLayout == it.layout
            }?.columnCount ?: breakPointMap[0].columnCount

    /**
     * Return the current screen type in terms of the grid system
     */
    fun getScreenSizeLayout(width: Int): ScreenSizeLayout = getGridSpec(width).layout

    fun getSideMargin(width: Int): Int {
        val columnCount = getColumnCount(width)
        return if (columnCount == 1) {
            singleColumnMargin
        } else {
            val contentWidth = columnCount * columnWidth + (columnCount - 1) * gutterWidth // N columns and N-1 gutters in between
            (width - contentWidth) / 2
        }
    }

    fun getColumnWidth(width: Int): Int =
        if (getColumnCount(width) == 1) {
            width - singleColumnMargin * 2
        } else {
            columnWidth
        }

    fun getGutterWidth(width: Int): Int =
        if (getColumnCount(width) == 1) {
            0
        } else {
            gutterWidth
        }

    fun getContentWidth(screenWidth: Int): Int {
        val columnCount = getGridSpec(screenWidth).columnCount
        return if (columnCount == 1) {
            screenWidth - singleColumnMargin * 2
        } else {
            columnCount * columnWidth + (columnCount - 1) * gutterWidth // N columns and N-1 gutters in between
        }
    }

    /**
     * Default vertical space between items
     */
    fun getGutterHeight(): Int = gutterWidth
}

class GridSpec(
    val lowerScreenWidth: Int,
    val upperScreenWidth: Int,
    val columnCount: Int,
    val layout: ScreenSizeLayout,
)

/**
 * Screen types in terms of the grid system
 */
enum class ScreenSizeLayout {
    XLARGE,
    LARGE,
    MEDIUM,
    SMALL,
    XSMALL,
}

enum class CardSegmentType {
    // Rounded corners on top and bottom
    FULL_CARD,

    // Rounded corners on top, square on bottom
    TOP_CARD,

    // Square corners on top and bottom
    MIDDLE_CARD,

    // Square corners on top, rounded on bottom
    BOTTOM_CARD,

    // No card
    NO_CARD,

    // Type has not been processed yet
    UNASSIGNED,
}
