package com.wapo.flagship.features.search2.model

/**
 * Base Filter UI Item model
 */
open class FilterItem(
    var id: String = "",
) : java.io.Serializable

/**
 * Filter header UI model
 * [group] - Group this header belongs to
 */
data class FilterHeaderItem(
    val label: String = "",
    val queryName: String = "",
    val isQuickFilter: Boolean = false,
    var isCollapsed: Boolean? = null,
) : FilterItem(label) {
    companion object {
        val SORT_BY = FilterHeaderItem("Sort By", "sort")

        val DATE = FilterHeaderItem("Date", "date")

        val SECTIONS = FilterHeaderItem("Sections", "section")

        val AUTHORS = FilterHeaderItem("Authors", "author")
    }

    /**
     * Overriding this so that we can use it in a Map
     */
    override fun hashCode(): Int = queryName.hashCode()

    /**
     * Overriding this for use in Map
     */
    override fun equals(other: Any?): Boolean {
        val otherFilterHeader = other as? FilterHeaderItem
        return otherFilterHeader != null && label == otherFilterHeader.label && queryName == otherFilterHeader.queryName
    }
}

/**
 * Radio item UI model
 * [label] - text shown on UI
 * [group] - Group radio item belongs to
 */
data class FilterRadioItem(
    val label: String = "",
    val queryId: String? = null,
    val group: FilterHeaderItem,
    var isChecked: Boolean = false,
    var isDefault: Boolean = false,
) : FilterItem(label)

/**
 * Radio item UI model
 * [label] - text shown on UI
 * [group] - Group radio item belongs to
 */
data class FilterCheckItem(
    val label: String = "",
    val queryId: String? = null,
    val group: FilterHeaderItem,
    var isChecked: Boolean = false,
    var isDefault: Boolean = false,
) : FilterItem(label)
