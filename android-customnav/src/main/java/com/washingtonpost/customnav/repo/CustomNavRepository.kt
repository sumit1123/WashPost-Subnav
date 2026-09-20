package com.washingtonpost.customnav.repo

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.model.SectionType
import com.washingtonpost.customnav.CustomNavProvider
import com.washingtonpost.customnav.data.CustomNavCellType
import com.washingtonpost.customnav.data.CustomNavSection
import java.lang.reflect.Type
import java.util.Collections
import javax.inject.Inject

class CustomNavRepository @Inject constructor(
    val context: Context,
    val customNavProvider: CustomNavProvider
) {

    val lockedSections = MutableLiveData<List<CustomNavSection>>()
    val selectedSections = MutableLiveData<List<CustomNavSection>>()
    val recommendedSections = MutableLiveData<List<CustomNavSection>>()

    private var selectedSectionsCache = mutableListOf<CustomNavSection>()
    private var recommendedSectionCache = mutableListOf<CustomNavSection>()
    private var newSections = listOf<Section>()
    private var removedSections = listOf<Section>()

    init {
        syncWithRemote()
        initActiveSections()
        initRecommendedSections()
    }

    fun reload() {
        initActiveSections()
        initRecommendedSections()
    }

    /**
     * Checks for diffs between stored and remote default lists.
     * If a customized list exists, gets newly added and newly removed sections.
     * New sections will be prepended to the customized list.
     * Removed sections will be removed from the customized list.
     */
    private fun syncWithRemote() {
        val remoteDefaultSections = customNavProvider.getDefaultBarSections()
        val localDefaultSections = getStoredDefaultSections()
        val isCustomizedList = getStoredSelectedSections() != null

        if (isCustomizedList && localDefaultSections.isNotEmpty() && remoteDefaultSections.isNotEmpty()) {
            // check for any new sections that are in remote but not local
            newSections = remoteDefaultSections.filter { section ->
                !localDefaultSections.any { section.id == it.id }
            }
            // check for any sections that have been removed from remote
            removedSections = localDefaultSections.filter { section ->
                !remoteDefaultSections.any { section.id == it.id }
            }
        }
        val newDefaultSections = handleForYouSection(remoteDefaultSections)
        storeDefaultSections(newDefaultSections)
    }

    /**
     * Manually inserts the For You section in the second position.
     * TODO: Remove this function when For You comes from remote.
     */
    private fun handleForYouSection(remoteDefaultSections: List<Section>): List<Section> {
        val forYouSection = Section(
            FOR_YOU_BUNDLE_NAME,
            FOR_YOU_BUNDLE_NAME,
            FOR_YOU_DISPLAY_NAME,
            FOR_YOU_DISPLAY_NAME,
            ArrayList<Section>(),
            FOR_YOU_DISPLAY_NAME,
            SectionType.SECTION
        )
        val forYouIndex = if (remoteDefaultSections.isNotEmpty()) { 1 } else { 0 }
        val newDefaultSections = remoteDefaultSections.toMutableList()
        newDefaultSections.add(forYouIndex, forYouSection)
        return newDefaultSections.toList()
    }

    private fun getForYouAiXpSection(): Section {
        return Section(
            FOR_YOU_AI_XP_BUNDLE_NAME,
            FOR_YOU_AI_XP_BUNDLE_NAME,
            FOR_YOU_AI_XP_DISPLAY_NAME,
            FOR_YOU_AI_XP_DISPLAY_NAME,
            ArrayList<Section>(),
            FOR_YOU_AI_XP_DISPLAY_NAME,
            SectionType.SECTION
        )
    }

    /**
     * Initializes Active RecyclerView sections:
     * - Locked + Selected sections.
     */
    private fun initActiveSections() {
        val defaultActiveSections = getStoredDefaultSections()
        val lockedSections = initLockedSections(defaultActiveSections)
        initSelectedSections(defaultActiveSections, lockedSections)
        this.lockedSections.value = toCustomNavSections(lockedSections, CustomNavCellType.LOCKED)
    }

    /**
     * Matches list of Locked ids against updated list of Bar sections from remote.
     * Orders Locked sections as they appear in Config.
     */
    private fun initLockedSections(defaultActiveSections: List<Section>): List<Section> {
        val lockedSectionsConfig = customNavProvider.getLockedSections()

        val lockedSectionsUnordered = defaultActiveSections.filter { defaultSection ->
            lockedSectionsConfig.any { defaultSection.id == it }
        }.toMutableList()

        val lockedSectionsOrdered = lockedSectionsConfig.mapNotNull { sectionId ->
            lockedSectionsUnordered.firstOrNull { it.id == sectionId }
        }
        // [AWA-10387] Disabling AI_XP section for now.
        //if (AppContextUtils.isBetaBuild() == true) {
        //    return mutableListOf<Section>().apply {
        //        addAll(lockedSectionsOrdered)
        //        add(if (size > 0) 1 else 0, getForYouAiXpSection())
        //    }.toList()
        //} else
        return lockedSectionsOrdered.toList()
    }

    /**
     * Gets stored Selected list from Prefs.
     * If stored list is not null, sets and updates it with changes from remote:
     * - New sections coming from remote are prepended to the stored Selected list.
     * - Sections removed from remote are removed from the stored Selected list.
     * If no stored Selected list exists, sets default and does not store anything.
     * - Default is latest SiteService Bar sections minus Locked sections.
     */
    private fun initSelectedSections(
        defaultActiveSections: List<Section>,
        lockedSections: List<Section>
    ) {
        val storedSelectedSections = getStoredSelectedSections()

        if (storedSelectedSections != null) {
            val selectedSectionsToBeInserted = toCustomNavSections(
                newSections.minus(lockedSections.toSet()),
                CustomNavCellType.SELECTED
            ).filter { section ->
                !storedSelectedSections.any { section.id == it.id }
            }

            val selectedSectionsToBeRemoved = storedSelectedSections.filter { section ->
                removedSections.any { section.id == it.id }
            }
            newSections = listOf()
            removedSections = listOf()

            selectedSectionsCache = selectedSectionsToBeInserted.plus(storedSelectedSections).minus(selectedSectionsToBeRemoved.toSet()).toMutableList()
            updateStoredSelectedList()
        } else {
            val defaultSelectedSections = defaultActiveSections.minus(lockedSections.toSet())
            selectedSectionsCache = toCustomNavSections(defaultSelectedSections, CustomNavCellType.SELECTED).toMutableList()
            selectedSections.value = selectedSectionsCache
        }
    }

    /**
     * Initialize Recommended sections.
     * Assemble list from Recently Visited + SiteService Recommended + SiteService Bar sections.
     * Then filter out currently active sections, i.e. those already in Locked or Selected.
     */
    private fun initRecommendedSections() {
        val recommendedSections = customNavProvider.getRecentSections()
            .plus(customNavProvider.getRecommendedSections())
            .plus(getStoredDefaultSections())

        toCustomNavSections(
            recommendedSections,
            CustomNavCellType.RECOMMENDED
        ).filter { recommended ->
            lockedSections.value?.any { recommended.id == it.id } != true
        }.filter { recommended ->
            !selectedSectionsCache.any { recommended.id == it.id }
        }.toMutableList().let {
            recommendedSectionCache = it
            this.recommendedSections.value = it
        }
    }

    private fun toCustomNavSections(
        sectionsList: List<Section>,
        type: CustomNavCellType
    ): List<CustomNavSection> {
        return sectionsList.map { section ->
            CustomNavSection(
                id = section.id,
                bundleName = section.bundleName,
                displayName = section.name,
                sectionType = section.sectionType,
                cellType = type
            )
        }
    }

    /**
     * Resets Selected topics to default.
     */
    fun resetTopics() {
        customNavProvider.trackCustomNavReset()
        storeSelectedSections(null)
        reload()
    }

    /**
     * Adds a new section to the end of the Selected list.
     * Removes that section from the Recommended list.
     */
    fun addSection(id: String) {
        recommendedSectionCache.find { it.id == id }?.let {
            recommendedSectionCache.remove(it)
            selectedSectionsCache.add(it.copy(cellType = CustomNavCellType.SELECTED))
        }
        updateStoredSelectedList()
        updateRecommendedList()
    }

    /**
     * Removes a section from the Selected list.
     * Adds that section to the end of the Recommended list.
     */
    fun removeSection(id: String) {
        selectedSectionsCache.find { it.id == id }?.let {
            selectedSectionsCache.remove(it)
            recommendedSectionCache.add(it.copy(cellType = CustomNavCellType.RECOMMENDED))
        }
        updateStoredSelectedList()
        updateRecommendedList()
    }

    /**
     * Returns false if dragged item's origin or destination are among the Locked sections.
     * Otherwise reorders the Selected sections and returns true.
     */
    fun reorderSections(fromPosition: Int, toPosition: Int): Boolean {
        val numLockedSections = lockedSections.value?.size ?: 0
        if (fromPosition < numLockedSections || toPosition < numLockedSections) {
            return false
        }
        val from = fromPosition - numLockedSections
        val to = toPosition - numLockedSections

        if (from < to) {
            for (i in from until to) {
                Collections.swap(selectedSectionsCache, i, i + 1)
            }
        } else {
            for (i in from downTo to + 1) {
                Collections.swap(selectedSectionsCache, i, i - 1)
            }
        }
        updateStoredSelectedList()
        return true
    }

    /**
     * Common code for storing the cached list in shared prefs.
     */
    private fun updateStoredSelectedList() {
        // Store in shared pref
        storeSelectedSections(selectedSectionsCache)
        // Update live data value
        selectedSections.value = selectedSectionsCache
    }

    private fun updateRecommendedList() {
        recommendedSections.value = recommendedSectionCache
    }

    /**
     * Stores Selected list in shared preferences.
     * Can store list as null in Reset flow.
     */
    private fun storeSelectedSections(list: List<CustomNavSection>?) {
        val json = if (list == null) null else Gson().toJson(list)
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(CUSTOM_SECTIONS_KEY, json)
            .apply()
    }

    /**
     * Loads list of selected sections from shared preferences.
     * If a section's cellType is null, sets its cellType to SELECTED.
     * Returns null if no stored list exists. This lets us know the user has not customized their list.
     */
    private fun getStoredSelectedSections(): List<CustomNavSection>? {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val json = prefs.getString(CUSTOM_SECTIONS_KEY, null) ?: return null
        val type: Type = object : TypeToken<List<CustomNavSection?>?>() {}.type
        val storedSelectedSections: List<CustomNavSection>? = Gson().fromJson(json, type)
        storedSelectedSections?.forEach { selectedSection ->
            if (selectedSection.cellType == null) {
                selectedSection.cellType = CustomNavCellType.SELECTED
            }
        }
        return storedSelectedSections
    }

    /**
     * Stores default list of sections in shared preferences.
     */
    private fun storeDefaultSections(list: List<Section>) {
        val json = Gson().toJson(list)
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(DEFAULT_SECTIONS_KEY, json)
            .apply()
    }

    /**
     * Loads default list of sections from shared preferences.
     * Returns an empty list if no stored list exists.
     */
    private fun getStoredDefaultSections(): List<Section> {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val json = prefs.getString(DEFAULT_SECTIONS_KEY, "")
        if (json.isNullOrEmpty()) return listOf()
        val type: Type = object : TypeToken<List<Section?>?>() {}.type
        return Gson().fromJson(json, type) ?: listOf()
    }

    fun trackCustomNavPageView() {
        customNavProvider.trackCustomNavPageView()
    }

    /**
     * Analytics wants us to match iOS in tracking enroll/disenroll events only when we leave the Custom Nav menu.
     * Checks for diffs between currently Selected sections and last tracked Selected sections.
     */
    fun trackEnrollDisenroll(lastTrackedSections: List<CustomNavSection>): List<CustomNavSection> {
        selectedSections.value?.let {
            val enrolledSections = it.minus(lastTrackedSections.toSet())
            val disenrolledSections = lastTrackedSections.minus(it.toSet())
            enrolledSections.forEach { section ->
                customNavProvider.trackCustomNavEnroll(section.displayName, true)
            }
            disenrolledSections.forEach { section ->
                customNavProvider.trackCustomNavEnroll(section.displayName, false)
            }
            return selectedSections.value ?: listOf()
        }
        return lastTrackedSections
    }

    companion object {
        const val PREFS_FILE = "custom-nav-prefs"
        const val CUSTOM_SECTIONS_KEY = "custom-sections"
        private const val DEFAULT_SECTIONS_KEY = "default-sections"
        private const val FOR_YOU_DISPLAY_NAME = "For You"
        private const val FOR_YOU_BUNDLE_NAME = "for-you"
        private const val FOR_YOU_AI_XP_DISPLAY_NAME = "AI-XP"
        private const val FOR_YOU_AI_XP_BUNDLE_NAME = "ai-xp"
    }
}