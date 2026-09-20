package com.wapo.flagship.features.grid

import com.wapo.flagship.features.grid.model.CardLayout
import com.wapo.flagship.features.grid.model.CardSegmentType
import com.wapo.flagship.features.grid.model.Chain
import com.wapo.flagship.features.grid.model.Item
import com.wapo.flagship.features.grid.model.Separator
import com.wapo.flagship.features.grid.model.Table

/**
 * Helper class to determine card UI view types based on the chain, table, features and the card layout.
 */
object CardificationUtils {

    /**
     * Assign a card type to a chain if cardLayout contains a corresponding chain id(s) to render a card around.
     * If it's the first chain in the card -> CardType.TOP_CARD
     * If it's the last chain in the card -> CardType.BOTTOM_CARD
     * If it's the only chain in the card -> CardType.FULL_CARD
     * If it's a chain in the middle of the card -> CardType.MIDDLE_CARD
     */
    fun assignCardType(chain: Chain, cardLayout: CardLayout?, lastCardSegmentType: CardSegmentType) {
        if (chain.isSeparator) {
            val separator = chain.items[0].items[0] as? Separator
            if (lastCardSegmentType == CardSegmentType.FULL_CARD || lastCardSegmentType == CardSegmentType.BOTTOM_CARD) separator?.betweenCards = true
            if (lastCardSegmentType == CardSegmentType.TOP_CARD || lastCardSegmentType == CardSegmentType.MIDDLE_CARD) separator?.insideCard = true

            return
        }
        if (chain.id == null || cardLayout == null || cardLayout.grids == null) {
            chain.cardSegmentType = CardSegmentType.NO_CARD
            return
        }

        for (cards in cardLayout.grids) {
            val itemIndex = cards.indexOf(chain.id)
            chain.cardSegmentType =
                when (itemIndex) {
                    0 -> if (cards.size == 1) CardSegmentType.FULL_CARD else CardSegmentType.TOP_CARD
                    cards.size - 1 -> CardSegmentType.BOTTOM_CARD
                    -1 -> CardSegmentType.NO_CARD
                    else -> CardSegmentType.MIDDLE_CARD
                }
            if (itemIndex >= 0) return
        }
    }

    /**
     * Assign a card type to a table if cardLayout contains a corresponding table id(s) to render a card around.
     * If it's the first table in the card -> CardType.TOP_CARD
     * If it's the last table in the card -> CardType.BOTTOM_CARD
     * If it's the only table in the card -> CardType.FULL_CARD
     * If it's a table in the middle of the card -> CardType.MIDDLE_CARD
     */
    fun assignCardType(table: Table, cardLayout: CardLayout?) {
        if (table.id == null || cardLayout == null || cardLayout.tables == null) {
            table.cardSegmentType = CardSegmentType.NO_CARD
            return
        }

        for (cards in cardLayout.tables) {
            val itemIndex = cards.indexOf(table.id)
            table.cardSegmentType =
                when (itemIndex) {
                    0 -> if (cards.size == 1) CardSegmentType.FULL_CARD else CardSegmentType.TOP_CARD
                    cards.size - 1 -> CardSegmentType.BOTTOM_CARD
                    -1 -> CardSegmentType.NO_CARD
                    else -> CardSegmentType.MIDDLE_CARD
                }
            if (itemIndex >= 0) return
        }
    }

    /**
     * Assign a card type to a feature item if it's in a table or chain which has a valid card type OR
     * cardLayout contains a corresponding feature id(s) to render a card around.
     *
     * Based on the chain's card type, determine whether a feature item is at the top, bottom, middle, or it's the only item.
     * Based on the table's card type, determine whether a feature item is at the top, bottom, middle, or it's the only item.
     * OR, if the feature is cardified, determine where it is in the card
     * If it's the first feature in the card -> CardType.TOP_CARD
     * If it's the last feature in the card -> CardType.BOTTOM_CARD
     * If it's the only feature in the card -> CardType.FULL_CARD
     * If it's a feature in the middle of the card -> CardType.MIDDLE_CARD
     */
    fun assignCardType(item: Item, cardLayout: CardLayout?, chain: Chain, table: Table) {
        if (item.resolvedColumn == -1 || cardLayout == null || (cardLayout.features == null && chain.cardSegmentType == CardSegmentType.NO_CARD && table.cardSegmentType == CardSegmentType.NO_CARD)) {
            item.cardSegmentType = CardSegmentType.NO_CARD
            return
        }

        if (chain.cardSegmentType != CardSegmentType.NO_CARD || table.cardSegmentType != CardSegmentType.NO_CARD) {
            val chainItemsSize = chain.items.size
            val resolvedTableItems = table.items.filter { it.resolvedColumn != -1 }
            val tableItemsSize = resolvedTableItems.size
            val tableIndex = chain.items.indexOf(table)
            val itemIndex = resolvedTableItems.indexOf(item)
            when (chain.cardSegmentType) {
                CardSegmentType.FULL_CARD -> {
                    item.cardSegmentType = if (chainItemsSize == 1 && tableItemsSize == 1) {
                                        CardSegmentType.FULL_CARD
                                    } else if (tableIndex == 0 && itemIndex == 0) {
                                        CardSegmentType.TOP_CARD
                                    } else if (tableIndex == chainItemsSize - 1 && itemIndex == tableItemsSize - 1) {
                                        CardSegmentType.BOTTOM_CARD
                                    } else {
                                        CardSegmentType.MIDDLE_CARD
                                    }
                }
                CardSegmentType.TOP_CARD -> item.cardSegmentType = if (tableIndex == 0 && itemIndex == 0) CardSegmentType.TOP_CARD else CardSegmentType.MIDDLE_CARD
                CardSegmentType.MIDDLE_CARD -> item.cardSegmentType = CardSegmentType.MIDDLE_CARD
                CardSegmentType.BOTTOM_CARD -> item.cardSegmentType = if (tableIndex == chainItemsSize - 1 && itemIndex == tableItemsSize - 1) CardSegmentType.BOTTOM_CARD else CardSegmentType.MIDDLE_CARD
                else -> {}
            }

            when (table.cardSegmentType) {
                CardSegmentType.FULL_CARD -> {
                    item.cardSegmentType = if (tableItemsSize == 1) {
                        CardSegmentType.FULL_CARD
                    } else if (itemIndex == 0) {
                        CardSegmentType.TOP_CARD
                    } else if (itemIndex == tableItemsSize - 1) {
                        CardSegmentType.BOTTOM_CARD
                    } else {
                        CardSegmentType.MIDDLE_CARD
                    }
                }
                CardSegmentType.TOP_CARD -> item.cardSegmentType = if (itemIndex == 0) CardSegmentType.TOP_CARD else CardSegmentType.MIDDLE_CARD
                CardSegmentType.MIDDLE_CARD -> item.cardSegmentType = CardSegmentType.MIDDLE_CARD
                CardSegmentType.BOTTOM_CARD -> item.cardSegmentType = if (itemIndex == tableItemsSize - 1) CardSegmentType.BOTTOM_CARD else CardSegmentType.MIDDLE_CARD
                else -> {}
            }
        } else if (cardLayout.features != null) {
                for (cards in cardLayout.features) {
                    val itemIndex = cards.indexOf(item.id)
                    item.cardSegmentType =
                        when (itemIndex) {
                            0 -> if (cards.size == 1) CardSegmentType.FULL_CARD else CardSegmentType.TOP_CARD
                            cards.size - 1 -> CardSegmentType.BOTTOM_CARD
                            -1 -> CardSegmentType.NO_CARD
                            else -> CardSegmentType.MIDDLE_CARD
                        }
                    if (itemIndex >= 0) return
                }
        }
    }
}