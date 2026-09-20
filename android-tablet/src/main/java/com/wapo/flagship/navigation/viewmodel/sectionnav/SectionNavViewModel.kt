package com.wapo.flagship.navigation.viewmodel.sectionnav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.domain.AppContextUtilsRepo
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.config.deepLinkArbitrarySections
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.data.RecentSection
import com.wapo.flagship.domain.repository.FindRepository
import com.wapo.flagship.domain.repository.MenuSectionRepo
import com.wapo.flagship.features.find.model.HighlightBoxType
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.ui.BottomTabFragment
import com.wapo.flagship.navigation.ui.TopBarState
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

@HiltViewModel
class SectionNavViewModel
@Inject
constructor(
    private val findRepository: FindRepository,
    private val intentHelper: IntentHelper,
    private val menuSectionRepo: MenuSectionRepo,
    private val appContextUtilsRepo: AppContextUtilsRepo,
    private val cacheManager: CacheManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SectionNavUiState()
    )
    val uiState: StateFlow<SectionNavUiState> = _uiState.asStateFlow()

    private val _sectionNavEvent: MutableSharedFlow<SectionNavEvent?> =
        MutableSharedFlow(replay = 0)
    val sectionNavEvent: SharedFlow<SectionNavEvent?> = _sectionNavEvent

    private var currentMenuSection: MenuSection? = null

    private var lastToggleTime = 0L

    private var finishSectionOnBack = false

    fun getSectionTitle(): String = _uiState.value.sectionTitle ?: ""

    fun setSectionTitle(title: String?) {
        _uiState.update {
            it.copy(
                sectionTitle = title
            )
        }
    }

    fun setTopBarState(state: TopBarState) {
        _uiState.update {
            it.copy(
                topBarState = state
            )
        }
    }

    fun reloadTitle() {
        viewModelScope.launch {
            _sectionNavEvent.emit(SectionNavEvent.ReloadTitle)
        }
    }

    fun setSectionTitle() {
        val newTitle = currentMenuSection?.let {
            if (it.sectionInfo != null && it.sectionInfo.size > 1) {
                it.title
            } else {
                null
            }
        }

        _uiState.update {
            it.copy(
                sectionTitle = newTitle
            )
        }
    }

    fun shouldFinishSectionOnBack(should: Boolean) {
        finishSectionOnBack = should
    }

    fun getFinishSectionOnBack(): Boolean {
        return finishSectionOnBack
    }

    fun openSection(
        id: String,
        navType: NavigationBehavior?
    ) {
        findRepository.getMenuSection(id)?.let {
            currentMenuSection = it
            viewModelScope.launch {
                _sectionNavEvent.emit(
                    SectionNavEvent.OpenSection(
                        menuSection = it,
                        navType = navType?.value
                    )
                )
            }
            setTopBarState(TopBarState.EXPANDED)
        }
    }

    fun resetOpenSection() {
        currentMenuSection = null
    }

    fun openSectionByUrl(url: String) {
        val menuSections = menuSectionRepo.getMenuSections()
        val menuSection = menuSectionRepo.getMenuSectionFromUrl(url, intentHelper, menuSections)
            ?: getArbitrarySection(url)

        // If no matching section found, open in webview
        if (menuSection == null) {
            viewModelScope.launch {
                _sectionNavEvent.emit(SectionNavEvent.OpenInWebView(url))
            }
        } else {
            menuSection.let {
                currentMenuSection = menuSection
                viewModelScope.launch {
                    _sectionNavEvent.emit(
                        SectionNavEvent.OpenSection(
                            menuSection = it
                        )
                    )
                }
                setTopBarState(TopBarState.EXPANDED)
            }
        }
    }

    /**
     * Used only for debuggable builds
     */
    private fun getArbitrarySection(url: String): MenuSection? {
        return if (!appContextUtilsRepo.isDebuggableBuild()) {
            null
        } else {
            val sectionUrl = url.trim('/')
            // create a new MenuSection for this arbitrary json url
            val sectionName = url.toUri()
                .lastPathSegment
                ?.uppercase()
                ?.replace("-", " ") ?: ""

            with(deepLinkArbitrarySections) {
                clear()
                this[sectionName] = sectionUrl
            }

            MenuSection(
                sectionName,
                sectionName,
                MenuSection.SECTION_TYPE_FUSION,
                sectionUrl,
                sectionUrl,
                null,
                0,
                emptyList(),
                false,
            )
        }
    }

    fun openComics() {
        openSection(HighlightBoxType.COMICS.link, NavigationBehavior.FIND_TAB_HIGHLIGHT)
        Measurement.trackComicsSection(NavigationBehavior.FIND_TAB_HIGHLIGHT.value)
    }

    fun openPrint() {
        viewModelScope.launch {
            _sectionNavEvent.emit(SectionNavEvent.OpenPrint)
        }
    }

    fun openPoliticsFromShortcut() {
        openSection(POLITICS_PATH, null) // TODO: navigation behavior
    }

    fun openHoroscope() {
        viewModelScope.launch {
            _sectionNavEvent.emit(
                SectionNavEvent.OpenInWebView(HighlightBoxType.HOROSCOPES.link)
            )
        }
        Measurement.trackHoroscopesSection(NavigationBehavior.FIND_TAB_HIGHLIGHT.value)
    }

    fun openRipple() {
        viewModelScope.launch {
            _sectionNavEvent.emit(
                SectionNavEvent.OpenInWebView(HighlightBoxType.RIPPLE.link)
            )
        }
    }

    fun updateRecentSections(menuSection: MenuSection) {
        if (menuSection.isUnlisted) return // don't update recent list with an unlisted section
        val itemType = 1
        val recentSections = cacheManager.recentSections
        val recentSectionsToUpdate = mutableListOf<RecentSection>()
        var same = false

        recentSections.forEach {
            if (it.name == menuSection.displayName) {
                val oldRecentSection = it
                oldRecentSection.setUpdateStatusDelete()
                recentSectionsToUpdate.add(oldRecentSection)
                val recentSection =
                    RecentSection(
                        menuSection.databaseId,
                        menuSection.displayName,
                        menuSection.bundleName,
                        itemType,
                        menuSection.type,
                    )
                recentSection.setUpdateStatusInsert()
                recentSectionsToUpdate.add(recentSection)
                cacheManager.updateRecentSections(recentSectionsToUpdate)
                same = true
            }
        }

        if (!same &&
            menuSection.displayName.trim() != TOP_STORIES_NAME &&
            menuSection.databaseId.trim() !in RECENTS_EXCLUDE_IDS &&
            menuSection.displayName.trim() !in RECENTS_EXCLUDE_NAMES
        ) {
            recentSectionsToUpdate.clear()
            if (cacheManager.recentSectionsSize >= BottomTabFragment.Companion.LIMIT_RECENT_LIST) {
                val oldRecentSection = cacheManager.recentSections[0]
                oldRecentSection.setUpdateStatusDelete()
                recentSectionsToUpdate.add(oldRecentSection)
            }
            val recentSection =
                RecentSection(
                    menuSection.databaseId,
                    menuSection.displayName,
                    menuSection.bundleName,
                    itemType,
                    menuSection.type,
                )
            recentSection.setUpdateStatusInsert()
            recentSectionsToUpdate.add(recentSection)
            cacheManager.updateRecentSections(recentSectionsToUpdate)
        }
    }

    fun updateRecentSections(sectionId: String) {
        if (sectionId.isNotEmpty() && !RECENTS_EXCLUDE_IDS.contains(sectionId)) {
            findRepository.getMenuSection(sectionId)?.let {
                updateRecentSections(it)
            }
        }
    }

    fun determineTopBarState(
        y: Float,
        hasSectionTitle: Boolean,
    ) {
        when {
            y < -TOGGLE_THRESHOLD_PIXELS -> toggleTopBarState(TopBarState.COLLAPSED)
            y > TOGGLE_THRESHOLD_PIXELS && hasSectionTitle ->
                toggleTopBarState(
                    TopBarState.EXPANDED,
                )

            y > TOGGLE_THRESHOLD_PIXELS -> toggleTopBarState(TopBarState.EXPANDED)

            //  TODO:   Check if this case is still needed
            //   It makes nav bars reappear after horizontal scrolls in carousels, which we don't want,
            //   but may be covering other edge cases – needs careful review.
            y == 0f -> toggleTopBarState(TopBarState.EXPANDED)

            else -> {}
        }
    }

    private fun toggleTopBarState(state: TopBarState) {
        if (_uiState.value.topBarState != state) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastToggle = currentTime - lastToggleTime
            if (timeSinceLastToggle > TOGGLE_TIME_THRESHOLD_MILLIS) {
                setTopBarState(state)
                lastToggleTime = currentTime
            }
        }
    }

    fun showBottomSheetPrompt(message: BannerPaywallMessage?) {
        _uiState.update {
            it.copy(
                bottomSheetPrompt = message
            )
        }
    }

    companion object {
        private const val TOGGLE_TIME_THRESHOLD_MILLIS = 250
        private const val TOGGLE_THRESHOLD_PIXELS = 20
        private const val TOP_STORIES_NAME = "Top Stories"
        private val RECENTS_EXCLUDE_IDS =
            listOf(
                "/classic-apps/classic-app-top-stories"
            )
        private val RECENTS_EXCLUDE_NAMES =
            listOf(
                "Top Stories"
            )
        const val POLITICS_PATH = "/politics"
    }
}
