package com.wapo.flagship.features.grid

import com.wapo.flagship.features.grid.model.*
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.wapo.flagship.features.grid.model.Table

/**
 * Helper class to perform operations on Grid model
 */
class GridProcessor {

    /**
     * The Grid model itself contains a lot if information for all screen types and the items are
     * our of order.
     * Before submitting the grid model to the adapter, the grid model must be "groomed".
     *
     * This method:
     * * assigns the proper adapter position
     * * assigns the proper col/row/spans for each item
     * reorders the tables/items
     *
     * according to the current screen type
     */
    fun process(grid: Grid, screenSizeLayout: ScreenSizeLayout, shouldSuppressItem: (Item) -> Boolean = { false }) {
        reorderToScreenSize(screenSizeLayout, grid, shouldSuppressItem)
    }

    /**
     * Get Items only available for a provided screenSizeLayout
     */
    fun getItems(grid: Grid, screenSizeLayout: ScreenSizeLayout, shouldSuppressItem: (Item) -> Boolean = { false }): List<Item> {
        // assume the grid is processed at this moment
        return grid.regions
                .flatMap { it.items }
                .flatMap { it.items }
                .flatMap { it.items }
                .filter { it.layoutAttributes.mapScreenToLocation(screenSizeLayout) != null }
                .filter { !shouldSuppressItem(it) }
    }

    /**
     * Get Tables only available for a provided screenSizeLayout
     */
    fun getTables(grid: Grid, screenSizeLayout: ScreenSizeLayout, shouldSuppressItem: (Item) -> Boolean = { false }): List<Table> {
        // assume the grid is processed at this moment
        val tables = mutableListOf<Table>()
        tables.addAll(grid.regions
                .flatMap { it.items }
                .flatMap { it.items }
                .filter { it.layoutAttributes.mapScreenToLocation(screenSizeLayout) != null }
                .map { table ->
                    table.apply {
                        // Warning: this modified the items list in place. 
                        // But we are returning it as a filtered list.
                        items = table.items.filter { 
                            it.layoutAttributes.mapScreenToLocation(screenSizeLayout) != null && !shouldSuppressItem(it)
                        }.toMutableList()
                    }
                })

        return tables
    }

    /**
     * Get Chains only available for a provided screenSizeLayout.
     * Note: This method returns a list of chains but does NOT modify the grid structural regions.
     * However, the Chain objects themselves will have their 'items' list updated to match the screen size and ad settings.
     */
    fun getChains(grid: Grid, screenSizeLayout: ScreenSizeLayout, shouldSuppressItem: (Item) -> Boolean = { false }): List<Chain> {
        return grid.regions.flatMap { region ->
            region.items.filter {
                //keeping this condition flexible here. if chains don't have layout attributes object just include it
                it.layoutAttributes == null || it.layoutAttributes!!.mapScreenToLocation(screenSizeLayout) != null
            }.mapNotNull { chain ->
                // We update the chain's tables list to reflect the current screen size and ad settings.
                // This is used by SpannableGridLayoutManager for rendering.
                val filteredTables = chain.items.mapNotNull { table ->
                    val filteredItems = table.items.filter { 
                        it.layoutAttributes.mapScreenToLocation(screenSizeLayout) != null && !shouldSuppressItem(it)
                    }.toMutableList()
                    
                    if (filteredItems.isNotEmpty() || table.isSeparator) {
                        table.apply { items = filteredItems }
                    } else {
                        null
                    }
                }.toMutableList()
                
                if (filteredTables.isNotEmpty()) {
                    chain.apply { items = filteredTables }
                } else {
                    null
                }
            }
        }
    }

    fun updateOnDisplayContext(grid: Grid, sectionDisplayName: String, displayContext: String) {
        if (!sectionDisplayName.equals("Recipes", ignoreCase = true)) {
            return
        }
        grid.regions.forEach { region ->
            val filteredItems = region.items
                .filter { item ->
                    val displayContextValue = item.displayContext
                    displayContextValue.isEmpty() || displayContextValue.contains(displayContext)
                }
            region.items = filteredItems.toMutableList()
        }
    }

    private fun reorderToScreenSize(screenSizeLayout: ScreenSizeLayout, grid: Grid, shouldSuppressItem: (Item) -> Boolean = { false }) {
        resetGrid(grid)

        val comparator = GridComparator(screenSizeLayout)
        // Sort items globally within their respective tables/chains before assigning positions
        for (region in grid.regions) {
            for (chain in region.items) {
                chain.items.sortWith(comparator)
                for (table in chain.items) {
                    table.items.sortWith(comparator)
                }
            }
        }

        var adapterPosition = 0

        // Get the list of chains that should be visible on this screen with current ad settings
        val chains = getChains(grid, screenSizeLayout, shouldSuppressItem)
        for (i in 0 until chains.size) {
            val chain = chains[i]
            // Tables are already filtered in getChains, but we verify location here for position assignment
            val tables = chain.items.filter { it.layoutAttributes.mapScreenToLocation(screenSizeLayout) != null }.toMutableList()
            for (j in 0 until tables.size) {
                val table = tables[j]
                // Items are already filtered in getChains
                val items = table.items
                
                table.resolvedColumn = table.getLocation(screenSizeLayout)?.column ?: -1
                table.resolvedRow = table.getLocation(screenSizeLayout)?.row ?: -1
                table.resolvedRowSpan = table.getLocation(screenSizeLayout)?.rowSpan ?: 1
                table.resolvedColumnSpan = table.getLocation(screenSizeLayout)?.span ?: -1
                
                for (item in items) {
                    item.tableId = table.id
                    item.chainId = chain.id
                    item.resolvedColumn = item.getLocation(screenSizeLayout)?.column ?: -1
                    if (item.resolvedColumn == -1) continue
                    item.resolvedRow = item.getLocation(screenSizeLayout)?.row ?: -1
                    item.resolvedColumnSpan = item.getLocation(screenSizeLayout)?.span ?: -1
                    item.resolvedRowSpan = item.getLocation(screenSizeLayout)?.rowSpan ?: -1
                    item.adapterPosition = adapterPosition
                    adapterPosition++
                }
            }
            // Update chain with resolved tables
            chain.items = tables.filter { it.resolvedRow != -1 }.toMutableList()
        }
    }

    private fun resetGrid(grid: Grid) {
        for (region in grid.regions) {
            for (chain in region.items) {
                for (table in chain.items) {
                    table.reset()
                    for (item in table.items) {
                        item.reset()
                    }
                }
            }
        }
    }
}

/**
 * Comparator that orders items from top to bottom, left to right
 */
private class GridComparator(private val screenSizeLayout: ScreenSizeLayout) : Comparator<Cell> {

    override fun compare(o1: Cell, o2: Cell): Int {
        val gridLocation1 = o1.layoutAttributes.mapScreenToLocation(screenSizeLayout)
        val gridLocation2 = o2.layoutAttributes.mapScreenToLocation(screenSizeLayout)
        if (gridLocation1 == null && gridLocation2 == null) return 0
        if (gridLocation1 != null && gridLocation2 == null) return -1
        if (gridLocation1 == null && gridLocation2 != null) return 1
        return when {
            gridLocation1!!.row < gridLocation2!!.row -> -1
            gridLocation1.row > gridLocation2.row -> 1
            else -> when {
                gridLocation1.column < gridLocation2.column -> -1
                gridLocation1.column > gridLocation2.column -> 1
                else -> 0
            }
        }
    }
}
