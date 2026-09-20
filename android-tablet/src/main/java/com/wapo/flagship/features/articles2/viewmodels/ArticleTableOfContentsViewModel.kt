package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.models.TableOfContentsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleTableOfContentsViewModel
    @Inject
    constructor() : ViewModel() {
        private val _showTableOfContentsEvent: LiveEvent<Boolean> = LiveEvent()
        val showTableOfContentsEvent: LiveData<Boolean> = _showTableOfContentsEvent

        private val _scrollToAnchorEvent: LiveEvent<String?> = LiveEvent()
        val scrollToAnchorEvent: LiveData<String?> = _scrollToAnchorEvent

        private val _restoreShowTableOfContentsEvent: LiveEvent<String> = LiveEvent()
        val restoreShowTableOfContentsEvent: LiveData<String> = _restoreShowTableOfContentsEvent

        private val _tableOfContentsEvent: MutableLiveData<TableOfContentsEvent> = MutableLiveData()
        val tableOfContentsEvent: LiveData<TableOfContentsEvent> = _tableOfContentsEvent

        fun dispatchShowTableOfContentsEvent(show: Boolean) {
            resetPreviousEvents()
            _showTableOfContentsEvent.value = show
        }

        fun dispatchScrollToAnchorEvent(anchorId: String?) {
            _scrollToAnchorEvent.value = anchorId
        }

        fun setTableOfContentsEvent(tableOfContentsEvent: TableOfContentsEvent) {
            _tableOfContentsEvent.value = tableOfContentsEvent
        }

        fun resetPreviousEvents() {
            _scrollToAnchorEvent.value = null
        }

        fun getAnchorPos(): Int {
            val tableOfContents = _tableOfContentsEvent.value?.tableOfContents
            val anchorId =
                _scrollToAnchorEvent.value ?: _tableOfContentsEvent.value?.currentAnchorId
            return tableOfContents?.children?.indexOfFirst { it.anchor == anchorId } ?: -1
        }

        fun restoreShowTableOfContentsEvent(metaId: String) {
            viewModelScope.launch {
                // delay is for StickyNav to be ready for current anchor id.
                delay(500)
                _restoreShowTableOfContentsEvent.value = metaId
            }
        }
    }
