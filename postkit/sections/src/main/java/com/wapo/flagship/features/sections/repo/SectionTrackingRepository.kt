package com.wapo.flagship.features.sections.repo

import com.wapo.flagship.features.sections.tracking.SectionTrackEvent
import com.wapo.flagship.features.sections.viewmodels.SectionNavigation

class SectionTrackingRepository {

    private var navigating = SectionNavigation.NONE
    private var currentSectionBundle: String? = null
    private var lastPageViewEvent: SectionTrackEvent? = null
    private var lastPageViewEventTimestamp: Long = -1
    private var navigationBehavior: String? = "app_open"
    private var miscellany: String? = null
    var shouldTrackBackToFront = true


    fun setNavigating(nav: SectionNavigation) {
        navigating = nav
    }

    fun getNavigating() = navigating

    fun resetNavigating() {
        navigating = SectionNavigation.NONE
    }

    fun setCurrentSectionBundle(value: String?) {
        currentSectionBundle = value
    }

    fun getCurrentSectionBundle() = currentSectionBundle

    fun setLastPageViewEvent(event: SectionTrackEvent) {
        lastPageViewEvent = event
    }

    fun getLastPageViewEvent() = lastPageViewEvent

    fun setLastPageViewEventTimestamp(timestamp: Long) {
        lastPageViewEventTimestamp = timestamp
    }

    fun getLastPageViewEventTimestamp() = lastPageViewEventTimestamp

    fun getNavigationBehavior(): String? = navigationBehavior

    fun setNavigationBehavior(value: String?) {
        navigationBehavior = value
    }

    fun getMiscellany(): String? = miscellany

    fun setMiscellany(value: String?) {
        miscellany = value
    }
}