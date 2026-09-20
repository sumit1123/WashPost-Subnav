package com.wapo.flagship.features.find.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.interleave
import com.wapo.flagship.data.RecentSection
import com.wapo.flagship.domain.repository.FindRepository
import com.wapo.flagship.features.find.events.FindClickEvent
import com.wapo.flagship.features.find.model.FindItem
import com.wapo.flagship.features.find.model.FindScreenUIState
import com.wapo.flagship.features.find.model.HeaderItem
import com.wapo.flagship.features.find.model.SectionBarItem
import com.wapo.flagship.features.find.model.SectionBoxItem
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FindViewModel
    @Inject
    constructor(
        private val repo: FindRepository,
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<FindScreenUIState>(FindScreenUIState.Loading)
        val uiState: StateFlow<FindScreenUIState> = _uiState

        private val _clickEvent = LiveEvent<FindClickEvent>()
        val clickEvent: LiveData<FindClickEvent> = _clickEvent

        private val highlights = mutableListOf<FindItem>()
        private var recents = mutableListOf<FindItem>()
        private var featuredSections = mutableListOf<FindItem>()
        private var azSections = mutableListOf<FindItem>()
        private var azSectionsInterleaved = mutableListOf<FindItem>()
        private var isAZTwoColumns: Boolean = false
        private var isAZEvenCount: Boolean = false
        private var lastItems: List<SectionBarItem?> = listOf()

        private var isInitialLoadFinished = false

        fun loadPage() {
            if (isInitialLoadFinished) return

            _uiState.value = FindScreenUIState.Loading
            viewModelScope.launch(dispatcherProvider.io) {
                updateHighlights()
                updateRecents()
                updateFeaturedSections()
                updateAZSections()
                loadAllSections()
                isInitialLoadFinished = true
            }
        }

        fun updateAZ(isTwoColumns: Boolean) {
            if (isAZTwoColumns != isTwoColumns) {
                isAZTwoColumns = isTwoColumns
                loadAllSections()
            }
        }

        fun onClick(findClickEvent: FindClickEvent) {
            _clickEvent.value = findClickEvent
        }

        fun reloadRecentsAndHighlights() {
            if (!isInitialLoadFinished) return
            viewModelScope.launch {
                val isRecentsChanged = updateRecents()
                val isHighlightsChanged = updateHighlights()
                if (isRecentsChanged || isHighlightsChanged) {
                    loadAllSections()
                }
            }
        }

        private fun loadAllSections() {
            mutableListOf<FindItem>()
                .apply {
                    addAll(highlights)
                    addAll(recents)
                    addAll(featuredSections)
                    if (isAZTwoColumns) {
                        addAll(azSectionsInterleaved)
                    } else {
                        addAll(azSections)
                    }
                    updateDividerVisibility()
                }.toList()
                .apply {
                    _uiState.value = FindScreenUIState.Success(this, isAZTwoColumns)
                }
        }

        private fun updateHighlights(): Boolean {
            var isUnchanged = false
            repo.getHighlightItems().let {
                isUnchanged = highlights == it
                if(it.isNotEmpty() && !isUnchanged) {
                    highlights.clear()
                    highlights.addAll(it)
                }
            }
            return !isUnchanged
        }

        private suspend fun updateRecents(): Boolean {
            var isUnchanged = false
            repo.getRecentSections().map { it.toSectionBoxItem(NavigationBehavior.FIND_TAB_RECENT) }.let { sb ->
                isUnchanged =
                    recents.filterIsInstance<SectionBoxItem>().map { it.bundleId } == sb.map { it.bundleId }
                if (sb.isNotEmpty() && !isUnchanged) {
                    recents.clear()
                    recents.add(HeaderItem("Recently visited"))
                    recents.addAll(sb)
                }
            }
            return !isUnchanged
        }

        private fun updateFeaturedSections() {
            featuredSections.clear()
            repo.getFeaturedSections().map { it.toSectionBoxItem(NavigationBehavior.FIND_TAB_FEATURED) }.let {
                if (it.isNotEmpty()) {
                    featuredSections.add(
                        HeaderItem("Featured").apply {
                            navType = NavigationBehavior.FIND_TAB_FEATURED
                        },
                    )
                    featuredSections.addAll(it)
                }
            }
        }

        private fun updateAZSections() {
            azSections.clear()
            repo.getAZSections().map { it.toSectionBarItem(NavigationBehavior.FIND_TAB_AZ) }.let {
                if (it.isNotEmpty()) {
                    // Setup for single column list
                    azSections.add(HeaderItem("A-Z").apply { navType = NavigationBehavior.FIND_TAB_AZ })
                    azSections.addAll(it)

                    // When AZ sections appear in two columns, we want the items to be interleaved.
                    azSectionsInterleaved.add(
                        HeaderItem("A-Z").apply { navType = NavigationBehavior.FIND_TAB_AZ },
                    )
                    it.chunked((it.size + 1) / 2).interleave().let { newList ->
                        azSectionsInterleaved.addAll(newList.toMutableList())

                        // We want to remove divider for last 2 items if list is even and last if odd
                        isAZEvenCount = newList.size % 2 == 0
                        azSectionsInterleaved
                            .takeLast(2)
                            .map { item ->
                                item as? SectionBarItem
                            }.apply {
                                lastItems = this
                            }
                    }
                }
            }
        }

        private fun updateDividerVisibility() {
            if (lastItems.isNotEmpty()) {
                when {
                    isAZTwoColumns && isAZEvenCount ->
                        lastItems.forEach {
                            it?.hasDivider = false
                        }

                    isAZTwoColumns || isAZEvenCount -> {
                        lastItems.first()?.hasDivider = true
                        lastItems.last()?.hasDivider = false
                    }

                    else -> {
                        lastItems.first()?.hasDivider = false
                        lastItems.last()?.hasDivider = true
                    }
                }
            }
        }

        private fun RecentSection.toSectionBoxItem(navType: NavigationBehavior? = null): SectionBoxItem =
            SectionBoxItem(this.name, this.menuItemId).apply {
                this.navType = navType
            }

        private fun MenuSection.toSectionBoxItem(navType: NavigationBehavior? = null): SectionBoxItem =
            SectionBoxItem(this.displayName, this.databaseId).apply {
                this.navType = navType
            }

        private fun MenuSection.toSectionBarItem(navType: NavigationBehavior? = null): SectionBarItem =
            SectionBarItem(this.displayName, this.databaseId).apply {
                this.navType = navType
            }
    }
