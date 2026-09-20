/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.viewmodels.sectionsribbon

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.sections.domein.SectionRibbonRepo
import com.wapo.flagship.features.sections.model.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionsRibbonViewModel @Inject constructor(
    private val sectionRibbonRepo: SectionRibbonRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SectionsRibbonUiState()
    )
    val uiState: StateFlow<SectionsRibbonUiState> = _uiState.asStateFlow()

    private val _sectionsRibbonEvent: MutableSharedFlow<SectionsRibbonEvents?> = MutableSharedFlow(replay = 0)
    val sectionsRibbonEvent: SharedFlow<SectionsRibbonEvents?> = _sectionsRibbonEvent
    val sectionsRibbonEventLiveData: LiveData<SectionsRibbonEvents?> = _sectionsRibbonEvent.asLiveData()

    private var ribbonReady: Boolean = false

    private val FOR_YOU_SECTION_ID = "for-you"

    fun setSections(newSections: List<Section>) {
        if (newSections != _uiState.value.sections) {
            _uiState.update {
                it.copy(
                    sections = newSections
                )
            }

            handleState()
        }
    }

    fun getSections() = _uiState.value.sections

    private fun handleState() {
        initSelectedSection()
        if (getSections().size > 1) {
            setIsVisible(true)
        } else {
            setIsVisible(false)
        }
    }

    fun sendTrackingData(tracking: Tracking) {
        viewModelScope.launch {
            _sectionsRibbonEvent.emit(
                SectionsRibbonEvents.SendTrackingInfoEvent(
                    tracking
                )
            )
        }
    }

    private fun initSelectedSection() {
        // Check if the last viewed section was "For You"
        val lastViewed = sectionRibbonRepo.getLastViewed()
        if (FOR_YOU_SECTION_ID == lastViewed) {
            val forYouIndex = getSectionIndex(FOR_YOU_SECTION_ID)
            if (forYouIndex != -1) {
                viewModelScope.launch {
                    _sectionsRibbonEvent.emit(SectionsRibbonEvents.OpenSectionOnLaunchEvent(true))
                }
                appOpenedOnSection(FOR_YOU_SECTION_ID)
                setSelectedSectionIndex(forYouIndex)
            } else {
                reset() // "For You" section not found, revert to default
            }
        } else {
            reset() // "For You" was not the last viewed
        }
    }

    fun setSelectedSectionIndex(index: Int) {
        _uiState.update {
            it.copy(
                selectedSectionIndex = if (_uiState.value.isLowDataModeEnable || index < 0) {
                    getDefaultIndex()
                } else {
                    index
                }
            )
        }
        val currentSection = getSection(_uiState.value.selectedSectionIndex)
        sectionRibbonRepo.setLastViewed(currentSection?.id)

        currentSection?.let {
            viewModelScope.launch {
                _sectionsRibbonEvent.emit(SectionsRibbonEvents.SelectedSectionIndexEvent(currentSection.id))
            }
        }
    }

    // Used for going to Tabs and My Post
    fun removeLastViewedSection() {
        sectionRibbonRepo.removeLastViewed()
    }

    private fun setIsVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                isVisible = visible
            )
        }
    }

    fun setIsLowDataModeEnable(enable: Boolean) {
        _uiState.update {
            it.copy(
                isLowDataModeEnable = enable
            )
        }
    }

    fun setShouldShowCustomNav(show: Boolean) {
        _uiState.update {
            it.copy(
                shouldShowCustomNav = show
            )
        }
    }

    fun setRibbonReady(ready: Boolean) {
        if (ribbonReady != ready) {
            ribbonReady = ready
            viewModelScope.launch {
                _sectionsRibbonEvent.emit(SectionsRibbonEvents.RibbonReadyEvent)
            }
        }
    }

    fun isRibbonReady() = ribbonReady

    fun reset() {
        setSelectedSectionIndex(getDefaultIndex())
    }

    fun getDefaultIndex(): Int {
        val topStoriesIndex = getSections().indexOfFirst {
            it.bundleName == "/." || it.name == "Top Stories"
        }
        return if (topStoriesIndex > 0) topStoriesIndex else 0
    }

    fun getSection(index: Int): Section? {
        getSections().let {
            if (index >= 0 && index < it.size) {
                return it[index]
            }
        }
        return null
    }

    fun getSectionIndex(sectionId: String): Int {
        return getSections().indexOfFirst { it.id == sectionId }
    }

    fun getSelectedSectionIndex(): Int {
        return _uiState.value.selectedSectionIndex
    }

    fun getCurrentSection(): Section? {
        return getSection(_uiState.value.selectedSectionIndex)
    }

    fun initVisitedNewsprintSection(visited: Boolean) {
        if (visited) {
            setVisitedNewsprintSection()
        }
    }

    fun checkNewsprintVisited() {
        if (getCurrentSection()?.id?.contains("newsprint") == true) {
            setVisitedNewsprintSection()
        }
    }

    private fun setVisitedNewsprintSection() {
        _uiState.update {
            it.copy(
                visitedNewsprintSection = true
            )
        }

        viewModelScope.launch {
            _sectionsRibbonEvent.emit(SectionsRibbonEvents.VisitedNewsprintSectionEvent(true))
        }
    }

    fun saveCurrentSection() {
        val currentSection: Section? = getSection(_uiState.value.selectedSectionIndex)
        if (currentSection != null) {
            saveLastViewedSection(currentSection.id)
        }
    }

    // Helper method to save the last viewed section ID to SharedPreferences
    private fun saveLastViewedSection(sectionId: String) {
        sectionRibbonRepo.setLastViewed(sectionId)
    }

    @JvmOverloads
    fun appOpenedOnSection(sectionId: String? = null) {
        if (sectionId != null) {
            sectionRibbonRepo.setOpenedOnKey(sectionId)
        } else {
            sectionRibbonRepo.removeOpenedOnKey()
            viewModelScope.launch {
                _sectionsRibbonEvent.emit(SectionsRibbonEvents.OpenSectionOnLaunchEvent(false))
            }
        }
    }
}
