package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.ItemState
import com.wapo.flagship.features.articles2.models.ItemTruncateState
import com.wapo.flagship.features.articles2.models.ItemTruncateType
import com.wapo.flagship.features.articles2.models.deserialized.Ad
import com.wapo.flagship.features.articles2.models.deserialized.Divider
import com.wapo.flagship.features.articles2.models.deserialized.ExpandCollapseCard
import com.wapo.flagship.features.articles2.models.deserialized.GalleryExpandCollapse
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.models.deserialized.Truncate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class Article2ItemsViewModel
    @Inject
    constructor() : ViewModel() {
        data class ItemsChangedState(
            val fromIndex: Int,
            val toIndex: Int,
            val items: List<Item>,
            val changeType: ItemChangeType,
        )

        enum class ItemChangeType {
            ADDED,
            REMOVED,
            CHANGED,
        }

        private val _itemsChangedEvent = LiveEvent<ItemsChangedState>()
        val itemsChangedEvent: LiveData<ItemsChangedState> = _itemsChangedEvent

        fun processItemsState(items: MutableList<Item>?) {
            items ?: return
            items.forEach { it.state = null }
            processGalleryItems(items)
            processTruncateItems(items)
            processExpandCardItems(items)
            updateItemsVisibility(items)
        }

        private fun processGalleryItems(items: MutableList<Item>?) {
            items ?: return
            var index = 0
            while (index < items.size) {
                val item = items[index]
                if (item is GalleryExpandCollapse) {
                    var currentIndex = index + 1
                    val truncateItemsList = mutableListOf<Item>()
                    while (currentIndex < items.size &&
                        (items[currentIndex] as? Image)?.galleryId == item.galleryId
                    ) {
                        truncateItemsList.add(items.removeAt(currentIndex))
                    }
                    item.state =
                        ItemState(
                            Truncate(
                                truncatedLabel = item.truncatedLabel,
                                expandedLabel = item.expandedLabel,
                                truncateItemsList.size,
                            ),
                            ItemTruncateState.COLLAPSED,
                            ItemTruncateType.GALLERY,
                            item,
                            truncateItemsList,
                        )
                    truncateItemsList.forEach { it.state = item.state }
                    println("ProcessingItems, Gallery, state=${item.state}")
                }
                index++
            }
        }

        private fun processTruncateItems(items: MutableList<Item>?) {
            items ?: return
            var index = 0
            while (index < items.size) {
                val item = items[index]
                if (item is SanitizedHtml && item.truncate != null) {
                    val truncateItemsCount = item.truncate.itemsCount ?: 0
                    var currentIndex = index + 1
                    val truncateItemsList = mutableListOf<Item>()
                    if (index + truncateItemsCount < items.size) {
                        repeat(truncateItemsCount) {
                            if (items[currentIndex] is GalleryExpandCollapse) {
                                truncateItemsList.add(items.removeAt(currentIndex))
                            }
                            truncateItemsList.add(items.removeAt(currentIndex))
                        }
                    }
                    item.state =
                        ItemState(
                            Truncate(
                                truncatedLabel = item.truncate.truncatedLabel,
                                expandedLabel = item.truncate.expandedLabel,
                                truncateItemsList.size,
                            ),
                            ItemTruncateState.COLLAPSED,
                            ItemTruncateType.PARAGRAPH,
                            item,
                            truncateItemsList,
                        )
                    truncateItemsList.forEach {
                        if (it.state != null) {
                            it.state?.parentItemState = item.state
                        } else {
                            it.state = item.state
                        }
                    }
                    println("ProcessingItems, Truncate, state=${item.state}")
                }
                index++
            }
        }

        private fun processExpandCardItems(items: MutableList<Item>?) {
            items ?: return
            var index = 0
            while (index < items.size) {
                val item = items[index]
                if (item is ExpandCollapseCard) {
                    var currentIndex = index + 1
                    val truncateItemsList = mutableListOf<Item>()
                    while (currentIndex < items.size && items[currentIndex].group == item.group) {
                        truncateItemsList.add(items.removeAt(currentIndex))
                    }
                    item.state =
                        ItemState(
                            Truncate(
                                truncatedLabel = item.truncatedLabel,
                                expandedLabel = item.expandedLabel,
                                truncateItemsList.size,
                            ),
                            ItemTruncateState.COLLAPSED,
                            ItemTruncateType.GROUP,
                            item,
                            truncateItemsList,
                        )
                    truncateItemsList.forEach {
                        if (it.state != null) {
                            it.state?.parentItemState = item.state
                        } else {
                            it.state = item.state
                        }
                    }
                    println("ProcessingItems, Group, state=${item.state}")
                }
                index++
            }
        }

        fun onItemClick(
            item: Item,
            items: MutableList<Item>?,
        ) {
            items ?: return
            item.state?.truncatedLabelItem ?: return
            val truncatedLabelIndex =
                items.indexOfFirst {
                    it.state?.truncatedLabelItem != null &&
                        it.state?.truncatedLabelItem === item.state?.truncatedLabelItem
                }
            if (truncatedLabelIndex == -1) return
            if (item.state?.truncateState == ItemTruncateState.COLLAPSED) {
                val truncatedItems = item.state?.truncatedItems
                if (!truncatedItems.isNullOrEmpty()) {
                    repeat(truncatedItems.size) {
                        items.add(truncatedLabelIndex + 1 + it, truncatedItems[it])
                    }
                    item.state?.truncateState = ItemTruncateState.EXPANDED
                    dispatchItemsChangedEvent(
                        truncatedLabelIndex,
                        truncatedLabelIndex + truncatedItems.size,
                        items,
                        ItemChangeType.ADDED,
                    )
                }
            } else if (item.state?.truncateState == ItemTruncateState.EXPANDED) {
                val truncatedItems = item.state?.truncatedItems
                if (!truncatedItems.isNullOrEmpty()) {
                    repeat(truncatedItems.size) {
                        if (items[truncatedLabelIndex + 1].state?.truncateState == ItemTruncateState.EXPANDED &&
                            items[truncatedLabelIndex + 1].state?.truncatedLabelItem != item.state?.truncatedLabelItem
                        ) {
                            onItemClick(items[truncatedLabelIndex + 1], items)
                        }
                        items.removeAt(truncatedLabelIndex + 1)
                    }
                    item.state?.truncateState = ItemTruncateState.COLLAPSED
                    dispatchItemsChangedEvent(
                        truncatedLabelIndex,
                        truncatedLabelIndex + truncatedItems.size,
                        items,
                        ItemChangeType.REMOVED,
                    )
                }
            }
        }

        private fun dispatchItemsChangedEvent(
            startPosition: Int,
            endPosition: Int,
            items: List<Item>,
            type: ItemChangeType,
        ) {
            _itemsChangedEvent.value = ItemsChangedState(startPosition, endPosition, items, type)
        }

        fun updateItemsVisibility(items: List<Item>?) {
            items?.forEachIndexed { index, item ->
                item.invisible = isItemInvisible(item, index)
            }
        }

        private fun isItemInvisible(
            item: Item,
            position: Int,
        ): Boolean =
            when {
                // Filter items to be displayed. Display When item's breakpoints
                // 1. is null - means item should be displayed on all breakpoints
                // 2. contain current rv's layoutSpec
                (
                    (item is Divider || item is Ad) &&
                        item.breakpoints != null &&
                        item.breakpoints?.contains(item.layoutSpec?.layout?.value) == false
                ) -> true

                else -> isTruncatedItemInvisible(item)
            }

        /**
         * Item's state has the same parent's state
         */
        private fun isTruncatedItemInvisible(item: Item): Boolean {
            val itemState = item.state ?: return false
            return (
                item is ExpandCollapseCard &&
                    item === itemState.truncatedLabelItem &&
                    item.state?.truncateState == ItemTruncateState.EXPANDED
            ) ||
                (
                    item is GalleryExpandCollapse &&
                        itemState.truncateState == ItemTruncateState.EXPANDED
                )
        }
    }
