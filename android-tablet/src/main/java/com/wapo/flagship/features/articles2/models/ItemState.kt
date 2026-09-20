package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.deserialized.Truncate

@JsonClass(generateAdapter = true)
class ItemState(
    val truncate: Truncate,
    var truncateState: ItemTruncateState,
    val truncateType: ItemTruncateType,
    var truncatedLabelItem: Item? = null,
    var truncatedItems: MutableList<Item> = mutableListOf(),
    var parentItemState: ItemState? = null,
) {
    fun isTruncated(): Boolean = truncateState == ItemTruncateState.COLLAPSED

    fun isTruncatedLabelItem(item: Item): Boolean = truncatedLabelItem === item

    fun isExpandedLabelItem(item: Item): Boolean = truncatedItems.lastOrNull() === item

    override fun toString(): String =
        "ItemState," +
            " truncate=$truncate," +
            " state=$truncateState," +
            " type=$truncateType," +
            " parentState=$parentItemState," +
            " itemsCount=${truncatedItems.size}," +
            " labelItemType=${truncatedLabelItem?.type}"
}
