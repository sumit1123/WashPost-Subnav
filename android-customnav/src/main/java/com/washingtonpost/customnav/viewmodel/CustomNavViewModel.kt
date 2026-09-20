package com.washingtonpost.customnav.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.sections.model.Section
import com.washingtonpost.customnav.data.CustomNavSection
import com.washingtonpost.customnav.repo.CustomNavRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomNavViewModel @Inject constructor(
    private val repo: CustomNavRepository
): ViewModel() {

    private val _activeSections = MediatorLiveData<List<CustomNavSection>>()
    val activeSections: LiveData<List<CustomNavSection>> = _activeSections

    private val _displayedRecommendedSections = MediatorLiveData<List<CustomNavSection>>()
    val displayedRecommendedSections: LiveData<List<CustomNavSection>> = _displayedRecommendedSections

    private var lastTrackedSections: List<CustomNavSection> = listOf()

    init {
        _activeSections.value = repo.lockedSections.value?.plus(repo.selectedSections.value ?: listOf()) ?: listOf()
        _displayedRecommendedSections.value = repo.recommendedSections.value ?: listOf()
        lastTrackedSections = repo.selectedSections.value?.toList() ?: listOf()
        addRemoteSource()
    }

    private fun addRemoteSource() {
        _activeSections.addSource(repo.lockedSections) {
            _activeSections.value = it.plus(repo.selectedSections.value ?: listOf())
        }
        _activeSections.addSource(repo.selectedSections) {
            _activeSections.value = repo.lockedSections.value?.plus(it) ?: listOf()
        }
        _displayedRecommendedSections.addSource(repo.recommendedSections) {
            _displayedRecommendedSections.value = it ?: listOf()
        }
    }

    fun removeSection(sectionToBeRemoved: CustomNavSection) {
        reloadRequired = true
        repo.removeSection(sectionToBeRemoved.id)
    }

    fun addSection(sectionToBeAdded: CustomNavSection) {
        reloadRequired = true
        repo.addSection(sectionToBeAdded.id)
    }

    fun reorderSections(fromPosition: Int, toPosition: Int): Boolean {
        val shouldReorder = repo.reorderSections(fromPosition, toPosition)
        reloadRequired = shouldReorder
        return shouldReorder
    }

    fun resetTopics() {
        reloadRequired = true
        repo.resetTopics()
    }

    fun getActiveSections(): List<Section> {
        repo.lockedSections.value?.plus(repo.selectedSections.value?: listOf())?.let {
            _activeSections.value?.let { actives ->
                if (actives != it) {
                    _activeSections.value = it
                }
            }
        }
        return _activeSections.value?.map { it.toSection() } ?: listOf()
    }

    fun reload(): Boolean {
        if (reloadRequired) {
            repo.reload()
            reloadRequired = false
            return true
        }
        return false
    }

    private fun CustomNavSection.toSection(): Section {
        return Section(
            id = this.id,
            bundleName = this.bundleName,
            name = this.displayName,
            title = this.displayName,
            sectionType = this.sectionType
        )
    }

    fun trackCustomNavPageView() {
        repo.trackCustomNavPageView()
    }

    fun trackEnrollDisenroll() {
        lastTrackedSections = repo.trackEnrollDisenroll(lastTrackedSections)
    }

    companion object {
        private var reloadRequired = false
    }
}