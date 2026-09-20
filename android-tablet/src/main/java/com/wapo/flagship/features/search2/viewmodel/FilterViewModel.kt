package com.wapo.flagship.features.search2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.model.FilterCheckItem
import com.wapo.flagship.features.search2.model.FilterHeaderItem
import com.wapo.flagship.features.search2.model.FilterItem
import com.wapo.flagship.features.search2.model.FilterRadioItem
import com.wapo.flagship.features.search2.model.QueryFilter
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.RecipesConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class FilterViewModel
    @Inject
    constructor(
        val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        private val _filterMap = MutableLiveData<Map<FilterHeaderItem, List<FilterItem>>>()
        val filterMap: LiveData<Map<FilterHeaderItem, List<FilterItem>>> = _filterMap

        private val _filterEvent = LiveEvent<FilterEvent>()
        val filterEvent: LiveData<FilterEvent> = _filterEvent

        var filterMapCache = mutableMapOf<FilterHeaderItem, List<FilterItem>>()

        var defaultSearchModeChanged = false

        val recipesFilterMapCache by lazy {
            recipesConfig.toMap()
        }

        private val recipesConfig: RecipesConfig
            get() = ConfigManager.getInstance().config.recipesConfig

        private fun RecipesConfig.toMap(): Map<FilterHeaderItem, List<FilterItem>> {
            val map = mutableMapOf<FilterHeaderItem, List<FilterItem>>()
            filters.forEach {
                val group = FilterHeaderItem(it.group, it.queryName, it.isQuickFilter, true)
                group.let { grp ->
                    val list =
                        it.items.map { item ->
                            when {
                                it.isMultiSelect -> FilterCheckItem(item.label, item.queryId, grp)
                                else -> FilterRadioItem(item.label, item.queryId, grp)
                            }
                        }
                    map[grp] = list
                }
            }
            return map
        }

        var queryFilters: QueryFilter = QueryFilter("")

        companion object {
            private const val NONE = "None"
            private val TAG = FilterViewModel::class.simpleName
        }

        fun isQuickFilterActive(tag: String): Boolean =
            queryFilters.filters.containsKey(tag) && queryFilters.filters[tag]?.isNotEmpty() == true

        fun resetFilters() {
            filterMapCache.forEach {
                it.value.forEach { filter ->
                    when (filter) {
                        is FilterCheckItem -> filter.isChecked = filter.isDefault
                        is FilterRadioItem -> filter.isChecked = filter.isDefault
                        else -> {}
                    }
                }
            }
            queryFilters.resetFilters()
            updateFilters(filterMapCache)
        }

        fun updateRadioItemActiveFilter(item: FilterRadioItem) {
            filterMapCache[item.group]?.forEach {
                if (it is FilterRadioItem) {
                    it.isChecked = item.id == it.id
                }
            }
            updateQueryFiltersObject(item.group, item.queryId)
        }

        fun updateCheckItemActiveFilters(item: FilterCheckItem) {
            val key = filterMapCache.keys.find { it.queryName == item.group.queryName }
            filterMapCache[key]?.find { it is FilterCheckItem && it.label == item.label }?.apply {
                if (this is FilterCheckItem) {
                    this.isChecked = item.isChecked
                }
            }
            val queryId =
                filterMapCache[key]
                    ?.filter { it is FilterCheckItem && it.isChecked }
                    ?.joinToString {
                        when (it) {
                            is FilterCheckItem -> it.queryId ?: ""
                            else -> ""
                        }
                    }?.replace(" ", "")
            updateQueryFiltersObject(item.group, queryId)
        }

        fun updateActiveFiltersById(
            groupName: String,
            filterIds: List<String>,
        ) {
            val group = filterMapCache.keys.find { it.queryName == groupName }
            if (group != null) {
                val queryIds: MutableList<String> = mutableListOf()
                filterMapCache[group]?.forEach { item ->
                    if (item is FilterCheckItem) {
                        item.queryId?.let {
                            if (filterIds.contains(it)) {
                                item.isChecked = true
                                queryIds.add(it)
                            }
                        }
                    } else if (item is FilterRadioItem) {
                        if (filterIds.contains(item.queryId)) {
                            item.isChecked = true
                            updateQueryFiltersObject(group, item.queryId)
                        }
                    }
                }
                if (queryIds.isNotEmpty()) {
                    updateQueryFiltersObject(group, queryIds.joinToString(","))
                }
            }
        }

        private fun updateQueryFiltersObject(
            group: FilterHeaderItem,
            queryId: String?,
        ) {
            queryFilters.setFilter(group.queryName, queryId)
            updateFilters(filterMapCache)
        }

        fun filterClicked(event: FilterEvent) {
            _filterEvent.postValue(event)
        }

        fun loadFilters(
            filterMap: Map<FilterHeaderItem, List<FilterItem>>?,
            loadDefaults: Boolean = true,
        ) {
            if (filterMap != null) {
                selectRadioFilter(filterMap)
            }

            if (loadDefaults) {
                filterMapCache.putAll(loadSortBy())
                // Temporarily removing date category in search filter
//            filterMapCache.putAll(loadDate())
                filterMap?.let {
                    filterMapCache.putAll(
                        checkForAuthorSectionSelection(it, FilterHeaderItem.AUTHORS.queryName),
                    )
                }
                filterMap?.let {
                    filterMapCache.putAll(
                        checkForAuthorSectionSelection(it, FilterHeaderItem.SECTIONS.queryName),
                    )
                }
            }
            filterMap?.let {
                filterMapCache.putAll(it)
            }
            updateFilters(filterMapCache)
        }

    /*
    finds items from cached filter that are true for isChecked and updates the
    received map with those values.
     */
        private fun selectRadioFilter(map: Map<FilterHeaderItem, List<FilterItem>>) {
            filterMapCache.values
                .flatten()
                .filter {
                    (it as? FilterRadioItem)?.isChecked == true
                }.forEach {
                    val filterRadioItem = it as? FilterRadioItem
                    map[filterRadioItem?.group]?.apply {
                        this
                            .find { item ->
                                (it as? FilterRadioItem)?.queryId == (item as? FilterRadioItem)?.queryId
                            }?.apply { (this as? FilterRadioItem)?.isChecked = true }
                    }
                }
        }

        fun updateExpandCollapse(filterHeaderItem: FilterHeaderItem) {
            filterMapCache.keys.find { it.label == filterHeaderItem.label }?.apply {
                isCollapsed = filterHeaderItem.isCollapsed
            }
            updateFilters(filterMapCache)
        }

        private fun updateFilters(map: Map<FilterHeaderItem, List<FilterItem>>) {
            _filterMap.postValue(map)
        }

    /*
    used to check and see if any author or section has been selected. If not
    defaults to first item.
     */
        private fun checkForAuthorSectionSelection(
            map: Map<FilterHeaderItem, List<FilterItem>>,
            category: String,
        ): Map<FilterHeaderItem, List<FilterItem>> {
            val subject =
                map.values.flatten().filter {
                    (it as? FilterRadioItem)?.isChecked == true && (it as? FilterRadioItem)?.group?.queryName == category
                }

            if (subject.isEmpty()) {
                try {
                    map.values
                        .flatten()
                        .first {
                            (it as? FilterRadioItem)?.group?.queryName == category
                        }.apply {
                            (this as? FilterRadioItem)?.isChecked = true
                            (this as? FilterRadioItem)?.isDefault = true
                        }
                } catch (exception: NoSuchElementException) {
                    return map
                }
            }
            return map
        }

        private fun loadSortBy(): Map<FilterHeaderItem, List<FilterRadioItem>> {
            val group = FilterHeaderItem.SORT_BY
            val sort =
                listOf(
                    FilterRadioItem("Date", "date", group, false, true),
                    FilterRadioItem("Relevance", "relevancy", group),
                )

            return mapOf(
                group to loadSelectionHelper(sort, group.queryName),
            )
        }

        private fun loadDate(): Map<FilterHeaderItem, List<FilterRadioItem>> {
            val group = FilterHeaderItem.DATE
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val lastYear = currentYear - 1
            val days =
                listOf(
                    FilterRadioItem("Any Time", "all", group, false, true),
                    FilterRadioItem("Last 7 Days", "last7", group),
                    FilterRadioItem("Last 30 Days", "last30", group),
                    FilterRadioItem("Last 90 Days", "last90", group),
                    FilterRadioItem(currentYear.toString(), "currentYear", group),
                    FilterRadioItem(lastYear.toString(), "lastYear", group),
                )

            return mapOf(
                group to loadSelectionHelper(days, group.queryName),
            )
        }

    /*
    sets true to inChecked in the list of FilterRadioItems based on category and what is in the filter cache
     */
        private fun loadSelectionHelper(
            list: List<FilterRadioItem>,
            category: String,
        ): List<FilterRadioItem> {
            filterMapCache.values
                .flatten()
                .filter { item ->
                    (item as FilterRadioItem).isChecked && item.group.queryName == category
                }.forEach { filtered ->
                    list.forEach {
                        if ((filtered as FilterRadioItem).queryId == it.queryId) {
                            it.isChecked = true
                        }
                    }
                }

            if (filterMapCache.values
                    .flatten()
                    .filter {
                        ((it as FilterRadioItem).isChecked && it.group.queryName == category)
                    }.isEmpty()
            ) {
                list[0].isChecked = true
            }
            return list
        }
    }
