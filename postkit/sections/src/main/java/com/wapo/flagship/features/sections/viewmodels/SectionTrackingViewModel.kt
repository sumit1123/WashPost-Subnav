package com.wapo.flagship.features.sections.viewmodels

import com.wapo.android.commons.util.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.sections.repo.SectionTrackingRepository
import com.wapo.flagship.features.sections.tracking.SectionTrackEvent
import com.washingtonpost.userhistory.domain.UserHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class SectionTrackingViewModel @Inject constructor(
    private val userHistoryManager: UserHistoryManager,
    private val sectionTrackingRepository: SectionTrackingRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<SectionTrackEvent>()
    val event = _event.asSharedFlow()

    fun getShouldTrackBackToFront(): Boolean = sectionTrackingRepository.shouldTrackBackToFront

    fun setShouldTrackBackToFront(value: Boolean) {
        sectionTrackingRepository.shouldTrackBackToFront = value
    }

    fun setCurrentSectionBundle(value: String?) {
        sectionTrackingRepository.setCurrentSectionBundle(value)
    }

    fun getNavigationBehavior(): String? = sectionTrackingRepository.getNavigationBehavior()

    fun setNavigationBehavior(value: String?) {
        sectionTrackingRepository.setNavigationBehavior(value)
    }

    fun getMiscellany(): String? = sectionTrackingRepository.getMiscellany()

    fun setMiscellany(value: String?) {
        sectionTrackingRepository.setMiscellany(value)
    }

    fun trackEvent(event: SectionTrackEvent, pos: Int, from: String = "") {
        when (event) {
            is SectionTrackEvent.PageView -> {
                val hasBeenTracked = event == sectionTrackingRepository.getLastPageViewEvent()
                        && abs(System.currentTimeMillis() - sectionTrackingRepository.getLastPageViewEventTimestamp()) < 800
                if (sectionTrackingRepository.getNavigating() != SectionNavigation.REFRESH && !hasBeenTracked) { // do not fire a page_view in REFRESH case
                    setNavBehavior(pos)
                    viewModelScope.launch {
                        _event.emit(event)
                    }

                    sectionTrackingRepository.setLastPageViewEvent(event)
                    sectionTrackingRepository.setLastPageViewEventTimestamp(System.currentTimeMillis())
                    (event as? SectionTrackEvent.PageView)?.let {
                        Logger.d("SECTION_TRACK", "$from -> ${it.displayName} | ${it.tracking?.pageTitle}")
                        Logger.d("SECTION_DETAIL", it.tracking.toString())
                    }
                }
                resetNavigating() // Reset the navigating value after it is used
            }

            is SectionTrackEvent.OnpageTap -> {
                if (from == NEWSPRINT_TOP_CARD) {
                    setMiscellany("sf_newsprint_type${pos + 1}")
                }
                viewModelScope.launch {
                    _event.emit(event)
                }
            }

            is SectionTrackEvent.AudioInteraction, is SectionTrackEvent.AudioStart -> {
                viewModelScope.launch {
                    _event.emit(event)
                }
            }
        }
    }
    fun trackEvent(event: SectionTrackEvent, from: String = "") {
        trackEvent(event, -1, from)
    }

    fun setNavigating(nav: SectionNavigation) {
        sectionTrackingRepository.setNavigating(nav)
        if (sectionTrackingRepository.getNavigating() != SectionNavigation.NONE) {
            viewModelScope.launch {
                userHistoryManager.postUserHistoryEvents()
            }
        }
    }

    fun resetNavigating() {
        sectionTrackingRepository.resetNavigating()
    }

    /**
     * This is a workaround to avoid duplicate tracking due to sections inserted
     * at the front of the list during runtime, e.g. For You
     */
    fun shouldTrackSection(bundleName: String): Boolean {
        return bundleName == sectionTrackingRepository.getCurrentSectionBundle()
                || sectionTrackingRepository.getCurrentSectionBundle() == null
    }

    /**
     * Sets navigation_behavior tracking based on how we navigated to the section.
     */
    private fun setNavBehavior(pos: Int) {
        val position = if (pos >= 0) { "${pos + 1}" } else { "" }
        val navBehavior = when (sectionTrackingRepository.getNavigating()) {
            SectionNavigation.RIBBON_TAP -> "top_ribbon_$position"
            SectionNavigation.SWIPE -> "swipe_$position"
            SectionNavigation.LABEL_TAP -> "label"
            SectionNavigation.DEEP_LINK -> "deep_link"
            SectionNavigation.TAB -> "change_tab"
            SectionNavigation.BACK -> "back_to_front"
            else -> sectionTrackingRepository.getNavigationBehavior()
        }
        sectionTrackingRepository.setNavigationBehavior(navBehavior)
    }

    companion object {
        const val NEWSPRINT_TOP_CARD = "NEWSPRINT_TOP_CARD"
    }
}

/**
 * Used for tracking how we navigate to the section.
 */
enum class SectionNavigation {
    NONE,
    RIBBON_TAP,
    SWIPE,
    LABEL_TAP,
    DEEP_LINK,
    TAB,
    BACK,
    REFRESH
}