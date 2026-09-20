package com.wapo.flagship.features.settings

import android.content.Context
import androidx.lifecycle.*
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.gifting.events.UserEvent
import com.wapo.flagship.features.preferencesapi.ContentPacksListApiStatus
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.wapo.flagship.features.preferencesapi.models.ContentPacksValueItem
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.UtilsKt
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentPacksViewModel
    @Inject
    constructor(
        private val contentPacksRepo: ContentPacksRepo,
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        var entryPoint = ""

        /**
         * Flag for the Onboarding variant of this feature.
         */
        var isOnboarding = false

        /**
         * In Settings, we must wait for two calls to complete:
         * - Content Packs Data call (what to show)
         * - User's Content Packs Prefs call (which content packs User has selected)
         * If the Data call completes before the Prefs call, we temporarily hold the Success UI state here while we await the result of the other call.
         */
        private var waiting: ContentPacksUiState? = null

        /**
         * Ui state that dictates the UI of the [SettingsContentPacksFragment].
         */
        private val _contentPacksUiState = MediatorLiveData<ContentPacksUiState>()
        val contentPacksUiState: LiveData<ContentPacksUiState> = _contentPacksUiState

        /**
         * Handle user click events.
         */
        private val _userEvent = LiveEvent<UserEvent>()
        val userEvent: LiveData<UserEvent> = _userEvent

        /**
         * List of user's currently selected content packs.
         * Stored as [ContentPacksValueItem] so they are ready to send.
         */
        private val _selectedContentPacks = LiveEvent<MutableList<ContentPacksValueItem>>()
        val selectedContentPacks: LiveEvent<MutableList<ContentPacksValueItem>> = _selectedContentPacks

        /**
         * Check if user has subscribed to any content packs.
         */
        val userContentPacksStatus
            get() = contentPacksRepo.getroContentPackStatus

        init {
            _contentPacksUiState.postValue(ContentPacksUiState.Loading)
            addContentPacksDataRemoteSource()
        }

        /**
         * Makes remote call to fetch content pack UI data.
         */
        fun getContentPackUiData() {
            viewModelScope.launch(dispatcherProvider.io) {
                _contentPacksUiState.postValue(ContentPacksUiState.Loading)
                contentPacksRepo.getContentPackItems(isOnboarding)
            }
        }

        /**
         * Makes remote call to fetch user's content pack selections.
         */
        fun getUserContentPacks() {
            waiting = null
            viewModelScope.launch(dispatcherProvider.io) {
                contentPacksRepo.getUserContentPacks()
            }
        }

        fun storeUserContentPacks(contentPacks: List<ContentPacksValueItem>?) {
            if (contentPacks != null) {
                _selectedContentPacks.value = contentPacks.toMutableList()
                stopWaiting()
            }
        }

        /**
         * Adds or removes content pack from list of selected content packs.
         */
        fun selectClicked(contentPackUiItem: ContentPackUiItem) {
            val contentPacksValueItem =
                ContentPacksValueItem(
                    CONTENT_PACK_SELECTED,
                    contentPackUiItem.id,
                    contentPackUiItem.referenceId,
                )
            val list = selectedContentPacks.value ?: mutableListOf()
            var selected = "selected"
            if (list.contains(contentPacksValueItem)) {
                list.remove(contentPacksValueItem)
                selected = "deselected"
            } else {
                list.add(contentPacksValueItem)
            }
            _selectedContentPacks.value = list
            trackContentPackSelection(contentPackUiItem.heading, selected)
        }

        /**
         * Tracks and assembles value of Miscellany parameter for content pack enroll/disenroll events.
         */
        private fun trackContentPackSelection(
            contentPackTitle: String?,
            selected: String,
        ) {
            val contentPackName =
                contentPackTitle?.let {
                    UtilsKt.toAnalyticsSnakeCase(it)
                } ?: ""
            val source = if (isOnboarding) "onboarding" else "settings"
            val miscellany = "${Measurement.PROFILE_PREFERENCE_CONTENT_PACK};$contentPackName;$selected;$source"
            Measurement.trackOnboardingClick(miscellany)
        }

        /**
         * Submits user's content pack preferences to [contentPacksRepo].
         */
        fun submitUserContentPacks(context: Context) {
            viewModelScope.launch(dispatcherProvider.io) {
                selectedContentPacks.value?.let {
                    PrefUtils.setSelectedContentPacks(context, it)
                    contentPacksRepo.setUserContentPacks(it.toList())
                }
            }
        }

        /**
         * Adds remote source for content pack data from [contentPacksRepo].
         * In Settings, if we are still awaiting the result of [userContentPacksStatus], temporarily hold the Success UI state.
         */
        private fun addContentPacksDataRemoteSource() {
            _contentPacksUiState.addSource(contentPacksRepo.getContentPacksListStatus) {
                when (it) {
                    is ContentPacksListApiStatus.Failure ->
                        _contentPacksUiState.postValue(
                            ContentPacksUiState.Failure,
                        )
                    is ContentPacksListApiStatus.Success -> {
                        if (!isOnboarding && userContentPacksStatus.value == null) {
                            waiting = ContentPacksUiState.Success(it.contentPacks)
                        } else {
                            _contentPacksUiState.postValue(ContentPacksUiState.Success(it.contentPacks))
                        }
                    }
                }
            }
        }

        /**
         * Releases a held [ContentPacksUiState] if there is one.
         */
        private fun stopWaiting() {
            waiting?.let {
                _contentPacksUiState.postValue(it)
            }
            waiting = null
        }

        companion object {
            private const val CONTENT_PACK_SELECTED = true
        }
    }
