package com.wapo.flagship.features.grid

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.grid.model.Ad
import com.wapo.flagship.features.grid.model.Bleed
import com.wapo.flagship.features.grid.model.Cards
import com.wapo.flagship.features.grid.model.Chain
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.Item
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.wapo.flagship.features.grid.model.Separator
import com.wapo.flagship.features.grid.model.SeparatorSize
import com.wapo.flagship.features.grid.model.Table
import com.wapo.flagship.features.grid.views.carousel.HabitTilesHolder
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.view.stack.FlexibleStackView
import com.washingtonpost.android.sections.R
import kotlin.math.max
import kotlin.math.min

/**
 * Custom layout manager that implements the idea of placing items on the screen according to their
 * layout attributes.
 *
 * Horizontally, the entire viewport is divided into a number columns and some gutters between them.
 * Every column and every gutter has a fixed width.
 *
 * Vertically, the viewport is divided into rows.
 * A row has undetermined height and should be as high as the tallest view in the row.
 *
 * [More info](https://arcpublishing.atlassian.net/wiki/spaces/AA/pages/701006786/Design+examples)
 *
 * The layout attributes consist of:
 * * column - the column index the item should start from horizontally
 * * col_span (column span) - the number of columns the item should take horizontally
 * * row - the row index the item should start from vertically
 * * row_span - the number of rows the item should take vertically.
 */
class SpannableGridLayoutManager(val wpGridView: WPGridView, val context: Context = wpGridView.context) : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {

    private var grid: Grid? = null
    private var chains: List<Chain>? = null
    private var screenSizeLayout: ScreenSizeLayout? = null
    private var newGrid = false
    private val gridProcessor = GridProcessor()
    private var reportedScreenWidth = -1
    private var requestedPosition = -1
    private var screenTypeListener: ScreenTypeListener? = null
    private val viewCache = SparseArray<View>()
    private val gridRenderHelper = GridRenderer()
    private var hasNudged = false
    private var displayContext: String = ""
    private var sectionDisplayName: String = ""
    private fun getIsCardified(): Boolean {
        return grid?.cards?.extraSmall != null && wpGridView.getScreenSizeLayout() == ScreenSizeLayout.XSMALL
    }

    fun setDisplayContext(displayContext: String) {
        this.displayContext = displayContext
    }

    fun setSectionDisplayName(sectionDisplayName: String) {
        this.sectionDisplayName = sectionDisplayName
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(c: Context, attrs: AttributeSet): RecyclerView.LayoutParams {
        return LayoutParams(c, attrs)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
        return if (lp is MarginLayoutParams) {
            LayoutParams((lp as MarginLayoutParams?)!!)
        } else {
            LayoutParams(lp)
        }
    }

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
        return lp is LayoutParams
    }

    fun setGrid(grid: Grid?) {
        this.grid = grid
        newGrid = true
        removeAllViews()
        requestLayout()
    }

    fun getGrid() = grid

    private fun getChains() = chains ?: emptyList()

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        removeAllViews()
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (detectOrientationChange() || newGrid) {
            removeAndRecycleAllViews(recycler)
            val screenSizeLayout = wpGridView.getScreenSizeLayout()
            this.screenSizeLayout = screenSizeLayout
            screenTypeListener?.onScreenTypeChanged(screenSizeLayout)
            grid?.let {
                // the screen size or the grid itself has changed and we need to
                // figure out the new order of the items before passing the grid to the adapter
                val shouldSuppressAds = wpGridView.adapter?.environment?.shouldSuppressAds() ?: false
                val shouldSuppressItem: (Item) -> Boolean = { it is Ad && shouldSuppressAds }
                gridProcessor.updateOnDisplayContext(it, sectionDisplayName, displayContext)
                gridProcessor.process(it, screenSizeLayout, shouldSuppressItem)
                chains = gridProcessor.getChains(it, screenSizeLayout, shouldSuppressItem)
                wpGridView.adapter?.addItems(it, wpGridView, screenSizeLayout, sectionDisplayName)
                if (NewsprintHelper.isNewsprintSection(sectionDisplayName)) {
                    wpGridView.setBackgroundColor(ContextCompat.getColor(context, R.color.newsprint_section_front_background))
                } else if (getIsCardified()) {
                    wpGridView.setBackgroundColor(ContextCompat.getColor(context, R.color.cardified_section_front_background))
                } else {
                    wpGridView.setBackgroundColor(ContextCompat.getColor(context, R.color.section_front_background))
                }
            }
        } else {
            val anchorView = findAnchorView()
            detachAndScrapAttachedViews(recycler)
            fill(anchorView, recycler)
        }
        newGrid = false
    }

    private fun detectOrientationChange(): Boolean {
        //only save scroll position on orientation change
        if (reportedScreenWidth != -1 && width != reportedScreenWidth) {
            saveScrollPosition()
        }

        if (width != reportedScreenWidth) {
            reportedScreenWidth = width
            return true
        }
        return false
    }

    private fun saveScrollPosition() {
        requestedPosition = getScrollPosition()
    }

    /**
     * A helper inner class to scope most of the layout logic into one place.
     * This inner class in only for de-scoping some code from the main class.
     */
    private inner class GridRenderer {

        lateinit var recycler: RecyclerView.Recycler

        var sideMargin: Int = 0
        var columnWidth: Int = 0
        var gutterWidth: Int = 0
        var columnAndGutter: Int = 0
        var columnCount: Int = 0

        var anchorView: View? = null
        var anchorOffset = 0
        var anchorRowIndex = 0
        var anchorTableIndex = 0
        var anchorChainIndex = 0

        var startingRowIndex = 0
        var startingTableIndex = 0
        var startingItemIndex = 0
        var startingChainIndex = 0

        var offset = 0
        var tableOffset = 0
        var shiftDistance = -1

        /**
         * Figure out all the initial parameters depending on the provided anchorView.
         * If there is no anchorView (means initial state) - the grid will start drawing from row=0, column=0, adapterPosition=0
         * If there IS some anchorView (means redrawing the screen after the user has scrolled some content up or down) -
         * the method will figure out what position, column, and row the grid should be drawn from.
         */
        fun prepare(anchorView: View?) {
            sideMargin = wpGridView.getSideMargin()
            columnWidth = wpGridView.getColumnWidth()
            gutterWidth = wpGridView.getGutterWidth()
            columnAndGutter = columnWidth + gutterWidth
            columnCount = wpGridView.getColumnCount()

            this.anchorView = anchorView
            this.anchorOffset = anchorView?.let { getDecoratedTop(anchorView) } ?: 0

            shiftDistance = -1
            offset = 0
            tableOffset = 0

            if (requestedPosition >= 0) {
                prepareByRequestedPosition(requestedPosition)
            } else if (anchorView != null) {
                val anchorTable = getTable(anchorView)!!
                val anchorChain = getChain(anchorView)!!
                anchorTableIndex = anchorChain.items.indexOf(anchorTable).coerceAtLeast(0)
                anchorRowIndex = getRow(anchorView)
                anchorChainIndex = getChains().indexOf(anchorChain).coerceAtLeast(0)
                val anchorTableRow = anchorTable.resolvedRow
                val firstRowInTable = anchorTable.items.firstOrNull()?.resolvedRow ?: 0

                if (anchorOffset > 0 && anchorRowIndex == firstRowInTable) {
                    //we have some space above the anchor view so we need to start drawing before the anchor view
                    // find the appropriate previous table
                    val prevTable = if (anchorTableRow > 0) { // jump to prev table within the chain
                        findTableAbove(anchorChainIndex, anchorTableRow - 1)
                    } else if (anchorChainIndex > 0) { // jump to the last table of the prev chain
                        findTableAbove(anchorChainIndex - 1, -1)
                    } else {
                        null
                    }
                    startingChainIndex = getChains().indexOfFirst { it.items.contains(prevTable) }.coerceAtLeast(0)
                    val tables = getChains()[startingChainIndex].items
                    startingTableIndex = tables.indexOf(prevTable).coerceAtLeast(0)
                    this.startingTableIndex = correctStartingTable(startingTableIndex, tables).coerceAtLeast(0)
                } else {
                    startingTableIndex = correctStartingTable(anchorTableIndex, anchorChain.items).coerceAtLeast(0)
                    startingChainIndex = anchorChainIndex
                }
            } else {
                reset()
            }
            startingRowIndex = 0
            startingItemIndex = 0
        }

        private fun prepareByRequestedPosition(requestedPosition: Int) {
            getChains().forEachIndexed { chainIndex, chain ->
                chain.items.forEachIndexed { tableIndex, table ->
                    table.items.forEachIndexed { itemIndex, item ->
                        if (item.adapterPosition == requestedPosition) {
                            anchorChainIndex = chainIndex
                            startingChainIndex = chainIndex
                            anchorTableIndex = tableIndex
                            startingTableIndex = correctStartingTable(anchorTableIndex, chain.items).coerceAtLeast(0)
                            anchorRowIndex = item.resolvedRow
                            return
                        }
                    }
                }
            }
            reset()
        }

        private fun reset() {
            anchorTableIndex = 0
            anchorRowIndex = 0
            anchorChainIndex = 0
            startingTableIndex = 0
            startingChainIndex = 0
        }

        /**
         * The main rendering method that fill the screen with new views starting with the params
         * that were defined in the *prepare* method
         */
        fun fillGrid(recycler: RecyclerView.Recycler) {
            this.recycler = recycler

            //start rendering items chain by chain until we reach the bottom of the viewport
            for (chainIndex in startingChainIndex until getChains().size) {
                val reachedBottom = renderChain(chainIndex)
                if (reachedBottom) {
                    break
                }
            }
            //shift children back if necessary
            offsetChildrenVertical(-shiftDistance + anchorOffset)
        }

        private fun isLastTableInRow(tableIndex: Int, tables: MutableList<Table>): Boolean {
            if (tableIndex == tables.lastIndex) {
                return true
            }
            val currentTableRow = tables[tableIndex].resolvedRow
            if (tables[tableIndex + 1].resolvedRow > currentTableRow) {
                return true
            }
            return false
        }

        fun renderTable(tableIndex: Int, tables: MutableList<Table>, chainIndex: Int) {
            val chain = getChains()[chainIndex]
            val table = tables[tableIndex]
            val rowsNumber = getRowsNumber(table)

            for (row in 0 until rowsNumber) {
                if (row == anchorRowIndex &&
                        tableIndex == anchorTableIndex &&
                        chainIndex == anchorChainIndex &&
                        shiftDistance == -1) {
                    shiftDistance = offset
                }
                val itemsOfRow = getItemsForRow(table, row)
                for (item in itemsOfRow) {
                    renderItem(item, table, chain, chainIndex)
                }
                offset = max(findBottomOfRow(row, tableIndex, tables), offset)
            }
        }

        private fun renderChain(chainIndex: Int) : Boolean {
            if (chainIndex < 0) return false
            if (chainIndex >= getChains().size) return false
            val chain = getChains()[chainIndex]
            val tables = chain.items

            // render items table by table
            for (tableIndex in startingTableIndex until tables.size) {
                val table = tables[tableIndex]
                val renderingTableRow = table.resolvedRow
                renderTable(tableIndex, tables, chainIndex)
                if (isLastTableInRow(tableIndex, tables)) {
                    //wrap up table
                    tableOffset = max(tableOffset, findTableBottomRow(renderingTableRow, tables))
                }
                if (shiftDistance >= 0 && tableOffset - shiftDistance + anchorOffset > height) {
                    return true
                }
                offset = tableOffset
                startingItemIndex = 0
            }
            startingTableIndex = 0

            return false
        }

        /**
         * Render a single item (view)
         */
        private fun renderItem(item: Item, table: Table, chain: Chain, chainIndex: Int) {
            var view = viewCache[item.adapterPosition]
            val columnStart = item.resolvedColumn + table.resolvedColumn
            val isFullWidth = item.resolvedColumnSpan == wpGridView.getColumnCount()
            val isFullBleed = (getIsCardified() && item.bleed == Bleed.FULL) || (item.forceFullBleed && isFullWidth)
            if (view == null) {
                val exactWidth = if (isFullBleed) {
                    item.resolvedColumnSpan * columnAndGutter - gutterWidth + (wpGridView.getSideMargin() * 2)
                } else {
                    item.resolvedColumnSpan * columnAndGutter - gutterWidth
                }
                
                // Safety check: ensure position is within adapter bounds to avoid IndexOutOfBoundsException
                // This handles the race condition during reactive grid updates.
                if (item.adapterPosition < 0 || item.adapterPosition >= (wpGridView.adapter?.itemCount ?: 0)) {
                    return
                }

                view = recycler.getViewForPosition(item.adapterPosition)
                addView(view)
                view.layoutParams.width = exactWidth
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                (view.layoutParams as LayoutParams).item = item
                (view.layoutParams as LayoutParams).table = table
                (view.layoutParams as LayoutParams).chain = chain
                (view.layoutParams as LayoutParams).chainIndex = chainIndex
                (view.layoutParams as LayoutParams).cards = grid?.cards

                measureChild(view, 0, 0)
                val left = when {
                    isFullBleed -> 0
                    columnCount == 1 -> sideMargin
                    else -> sideMargin + columnStart * columnAndGutter
                }
                layoutDecorated(view, left, offset, left + getDecoratedMeasuredWidth(view), offset + getDecoratedMeasuredHeight(view))

                when (view) {
                    is ViewGroup -> {
                        val child =
                            if (getIsCardified())
                                (view.getChildAt(1) as? ViewGroup)?.getChildAt(0)
                            else
                                view.getChildAt(0)
                        if (!hasNudged && child is FlexibleStackView && isVisible(view)) {
                            child.nudge()
                            hasNudged = true
                        }
                    }
                }
            } else {
                attachView(view)
                if (getDecoratedTop(view) != offset) {
                    val left = when {
                        isFullBleed -> 0
                        columnCount == 1 -> sideMargin
                        else -> sideMargin + columnStart * columnAndGutter
                    }
                    layoutDecorated(view, left, offset, left + getDecoratedMeasuredWidth(view), offset + getDecoratedMeasuredHeight(view))
                }

                viewCache.remove(item.adapterPosition)
            }
        }
    }

    /**
     * If there is another table that has rowspan > 1 we need to start from the row that spanned table starts from
     */
    private fun correctStartingTable(startingTableIndex: Int, tables: MutableList<Table>): Int {
        val table = tables[startingTableIndex]
        val tableRow = table.resolvedRow
        val tablesEndingOnRow = tables.filter { it.resolvedRow + it.resolvedRowSpan - 1 == tableRow}
        val topTableRow = tablesEndingOnRow.minOfOrNull { it.resolvedRow } ?: tableRow
        return tables.indexOfFirst { it.resolvedRow == topTableRow }.takeIf { it >= 0 } ?: startingTableIndex
    }

    /**
     * Find the first table above the given row
     * @param tableRow the row to start from above or -1 to start from the end of the chain
     * @return table's index in its' chain
     */
    private fun findTableAbove(chainIndex: Int, tableRow: Int): Table? {
        val chain = getChains()[chainIndex]
        val tables = chain.items
        val targetTableRow = if (tableRow >= 0) tableRow - 1 else tables.maxByOrNull { it.resolvedRow }?.resolvedRow ?: 0
        val table = tables.firstOrNull { it.resolvedRow == targetTableRow && it.items.isNotEmpty() }
        return when {
            table != null -> {
                table
            }
            tableRow > 0 -> {
                findTableAbove(chainIndex, tableRow - 1)
            }
            chainIndex > 0 -> {
                // keep searching up for the table
                findTableAbove(chainIndex - 1, -1)
            }
            else -> null
        }
    }

    private fun getItemsForRow(table: Table, row: Int): List<Item> {
        return table.items.filter { it.resolvedRow == row }
    }

    /**
     * Get the number of rows in the table
     */
    private fun getRowsNumber(table: Table): Int {
        var rowsNumber = 0
        for (item in table.items) {
            if (item.resolvedRow >= 0) {
                val rowEnd = item.resolvedRow + item.resolvedRowSpan
                if (rowEnd > rowsNumber) {
                    rowsNumber = rowEnd
                }
            }
        }
        return rowsNumber
    }

    private fun fill(anchorView: View?, recycler: RecyclerView.Recycler) {
        viewCache.clear()

        stashViews()

        if (itemCount > 0 && getChains().isNotEmpty()) {
            gridRenderHelper.prepare(anchorView)
            gridRenderHelper.fillGrid(recycler)
        }

        unStashAndRecycleViews(recycler)

        requestedPosition = -1
    }

    private fun unStashAndRecycleViews(recycler: RecyclerView.Recycler) {
        for (i in 0 until viewCache.size()) {
            recycler.recycleView(viewCache.valueAt(i))
        }
        viewCache.clear()
    }

    private fun findTableBottomRow(tableRow: Int, tables: MutableList<Table>): Int {
        val tablesOnRow = tables.filter { it.resolvedRow + it.resolvedRowSpan - 1 == tableRow }
        return tablesOnRow.maxOfOrNull { findBottomOfTable(it) } ?: 0
    }

    private fun findBottomOfTable(table: Table): Int {
        //TODO optimize this
        val viewsOfTable = mutableListOf<View>()
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null && getTable(view) == table) {
                viewsOfTable.add(view)
            }
        }

        return viewsOfTable.maxOfOrNull { getDecoratedBottom(it) } ?: 0
    }

    private fun stashViews() {
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null) {
                val pos = getPosition(view)
                viewCache.put(pos, view)
            }
        }

        for (i in 0 until viewCache.size()) {
            detachView(viewCache.valueAt(i))
        }
    }

    /**
     * Finds first partially or fully visible view in the layout
     */
    private fun findAnchorView(): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != null && isVisible(child)) {
                // assuming views are laid out in straight order
                return child
            }
        }
        if (childCount > 0) {
            return getChildAt(0)
        }
        return null
    }

    internal fun getScrollPosition(): Int {
        val firstVisibleAnchorView = findAnchorView()
        return if (firstVisibleAnchorView != null) {
            getPosition(firstVisibleAnchorView)
        } else 0
    }

    private fun isVisible(view: View): Boolean {
        val top = getDecoratedTop(view)
        val bottom = getDecoratedBottom(view)
        return top <= height && bottom >= 0
    }

    private fun findBottomOfRow(row: Int, tableIndex: Int, tables: MutableList<Table>): Int {
        var bottom = 0
        val parentTable = tables[tableIndex]
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val item = (child?.layoutParams as? LayoutParams)?.item
            val table = (child?.layoutParams as? LayoutParams)?.table
            if (child != null && item != null && table == parentTable) {
                val itemRowEnd = item.resolvedRow + item.resolvedRowSpan - 1
                if (itemRowEnd == row) {
                    bottom = max (getDecoratedBottom(child), bottom)
                }
            }
        }
        return if (bottom > 0) {
            bottom
        } else {
            if (row > 0) {
                findBottomOfRow(row - 1, tableIndex, tables)
            } else {
                if (tableIndex > 0) {
                    val prevTable = tables[tableIndex - 1]
                    val rowsNumber = getRowsNumber(prevTable)
                    findBottomOfRow(rowsNumber - 1, tableIndex - 1, tables)
                } else {
                    bottom
                }
            }
        }
    }

    private fun findTopOfRow(row: Int, tableIndex: Int, chainIndex: Int): Int {
        val viewsOfRow = mutableListOf<View>()
        val viewsOfTable = mutableListOf<View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val item = (child?.layoutParams as? LayoutParams)?.item
            val table = (child?.layoutParams as? LayoutParams)?.table
            val chain = (child?.layoutParams as? LayoutParams)?.chain
            val parentChainIndex = getChains().indexOf(chain)
            val parentTableIndex = chain!!.items.indexOf(table)
            if (item != null && tableIndex == parentTableIndex && chainIndex == parentChainIndex) {
                if (item.resolvedRow == row) {
                    viewsOfRow.add(child)
                }
                viewsOfTable.add(child)
            }
        }
        val viewsToSearch = if (viewsOfRow.isEmpty()) {
            // For some reason items in the table star with non 0 row.
            // Just find the topmost view among all table views.
            viewsOfTable
        } else {
            viewsOfRow
        }

        val maxTop = viewsToSearch.maxOfOrNull { getDecoratedTop(it) }
        if (maxTop != null) return maxTop

        return -1
    }

    private fun getRow(view: View?): Int {
        return (view?.layoutParams as? LayoutParams)?.item?.resolvedRow ?: 0
    }

    private fun getTable(view: View?): Table? {
        return (view?.layoutParams as? LayoutParams)?.table
    }

    private fun getChain(view: View?): Chain? {
        return (view?.layoutParams as? LayoutParams)?.chain
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val delta = checkAvailableVerticalScroll(dy)

        if (delta != 0) {
            offsetChildrenVertical(-delta)
            val anchorView = findAnchorView()
            detachAndScrapAttachedViews(recycler)
            fill(anchorView, recycler)
        }
        fixOverScrollDown()
        fixOverScrollUp()
        return delta
    }

    override fun scrollToPosition(position: Int) {
        requestedPosition = position
        requestLayout()
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val linearSmoothScroller = LinearSmoothScroller(recyclerView.context)
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1 else 1
        return PointF(0f, direction.toFloat())
    }

    /**
     * If user scrolled up too much and we have nothing to render, we need to fix the gap
     */
    private fun fixOverScrollUp() {
        if (getChains().isEmpty()) return
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null) {
                val table = getTable(view)
                val firstChain = getChains().firstOrNull()
                val tables = firstChain?.items ?: emptyList()
                if (tables.isNotEmpty() && table == tables[0]) {
                    val row = getRow(view)
                    if (row == 0) {
                        val decoratedTop = getDecoratedTop(view)
                        if (decoratedTop > 0) {
                            offsetChildrenVertical(-decoratedTop)
                            return
                        }
                    }
                }
            }
        }
    }

    /**
     * If user scrolled down too much and we have nothing to render, we need to fix the gap
     */
    private fun fixOverScrollDown() {
        var maxBottom = 0
        var lastItemOnScreen = false
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null) {
                val bottom = getDecoratedBottom(view)
                maxBottom = max(maxBottom, bottom)
                if (getPosition(view) == itemCount - 1) {
                    lastItemOnScreen = true
                }
            }
        }
        if (maxBottom != 0 && lastItemOnScreen) {
            val delta = height - maxBottom
            if (delta > 0) {
                offsetChildrenVertical(delta)
            }
        }
    }

    private fun checkAvailableVerticalScroll(dy: Int): Int {
        if (childCount == 0) {
            return 0
        }

        if (dy < 0) {
            val firstItemView = hasFirstItem()
            if (firstItemView != null) {
                // the first adapter view might not be the topmost view in the table
                // we need to find the top edge of the view's parent table
                val table = (firstItemView.layoutParams as? LayoutParams)?.table
                val chain = (firstItemView.layoutParams as? LayoutParams)?.chain
                val parentChainIndex = getChains().indexOf(chain).coerceAtLeast(0)
                val parentTableIndex = chain?.items?.indexOf(table)?.coerceAtLeast(0) ?: 0
                val topEdge = findTopOfRow(0, parentTableIndex, parentChainIndex)
                return max(topEdge, dy)
            } else {
                return dy
            }
        } else {
            if (hasLastItem()) {
                val lastChain = getChains().lastOrNull()
                val lastTable = lastChain?.items?.lastOrNull()
                if (lastTable != null) {
                    val bottomEdge =
                        if (lastTable.isSeparator) {
                            val size = getSeparatorSize(lastTable)
                            val lastNonSeparatorTable = getLastTableWithoutSeparator()
                            if (lastNonSeparatorTable != null) {
                                findBottomOfTable(lastNonSeparatorTable) + size.toInt()
                            } else {
                                findBottomOfTable(lastTable)
                            }
                        } else {
                            findBottomOfTable(lastTable)
                        }
                    return max(0, min(bottomEdge - height, dy))
                } else {
                    return dy
                }
            } else {
                return dy
            }
        }
    }

    private fun getSeparatorSize(table: Table): Float {
        if (table.items.isEmpty()) return 0f
        return table.items[0]
                .run { if (this is Separator) this.size else null }
                .run { when(this) {
                    SeparatorSize.XSMALL -> context.resources.getDimension(R.dimen.grid_chain_separator_xsmall)
                    SeparatorSize.SMALL -> context.resources.getDimension(R.dimen.grid_chain_separator_small)
                    SeparatorSize.LARGE -> context.resources.getDimension(R.dimen.grid_chain_separator_large)
                    else -> 0f
                } }
    }

    private fun getLastTableWithoutSeparator(): Table? {
        val lastChain = getChains().lastOrNull() ?: return null
        val tables = lastChain.items
        for (i in tables.size - 1 downTo 0) {
            //exclude separator from being last table because it's decorated bottom is way off
            if (tables[i].isSeparator) {
                continue
            }
            return tables[i]
        }
        return null
    }

    private fun hasFirstItem(): View? {
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val position = getPosition(view!!)
            if (position == 0) {
                return view
            }
        }
        return null
    }

    private fun hasLastItem(): Boolean {
        for (i in 0 until childCount) {
            if (getPosition(getChildAt(i)!!) == itemCount - 1) {
                return true
            }
        }
        return false
    }

    fun setScreenTypeListener(screenTypeListener: ScreenTypeListener) {
        this.screenTypeListener = screenTypeListener
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        if (childCount > 0) {
            event.fromIndex = findFirstVisibleItemPosition()
            event.toIndex = findLastVisibleItemPosition()
        }
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        return if (childCount == 0) 0 else 1
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        val view = findAnchorView()
        if (view != null) {
            return getPosition(view)
        }
        return 0
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        return wpGridView.adapter?.itemCount ?: 0
    }

    private fun findFirstVisibleItemPosition(): Int {
        val view = findFirstVisibleView()
        if (view != null) {
            return getPosition(view)
        }
        return 0
    }

    private fun findLastVisibleItemPosition(): Int {
        val view = findLastVisibleView()
        if (view != null) {
            return getPosition(view)
        }
        return 0
    }

    override fun onFocusSearchFailed(
        focused: View,
        direction: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): View {
        when (direction) {
            View.FOCUS_BACKWARD,
            View.FOCUS_UP -> {
                wpGridView.scrollBy(0, -height / 2)
            }
            View.FOCUS_FORWARD,
            View.FOCUS_DOWN -> {
                wpGridView.scrollBy(0, height / 2)
            }
        }
        return focused
    }

    private fun findLastVisibleView() : View? {
        for (i in (childCount - 1) downTo 0) {
            val view = getChildAt(i)
            if (view != null && isVisible(view)) {
                return view
            }
        }
        return null
    }

    private fun findFirstVisibleView() : View? {
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null && isVisible(view)) {
                return view
            }
        }
        return null
    }

    class LayoutParams : RecyclerView.LayoutParams {
        var chainIndex: Int = -1
        var cards: Cards? = null
        var chain: Chain? = null
        var table: Table? = null
        var item: Item? = null

        constructor(c: Context, attr: AttributeSet) : super(c, attr)
        constructor(w: Int, h: Int) : super(w, h)
        constructor(source: MarginLayoutParams) : super(source)
        constructor(source: ViewGroup.LayoutParams) : super(source)
        constructor(source: RecyclerView.LayoutParams) : super(source)
    }
}