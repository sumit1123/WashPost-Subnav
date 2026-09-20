// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.features.grid.viewmodel.sectionsHabitTiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.grid.domain.model.SectionHabitTile
import com.wapo.flagship.features.grid.domain.model.SectionHabitTilesData
import com.wapo.flagship.features.grid.domain.repository.SectionHabitTilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SectionsHabitTilesViewModel @Inject constructor(
    private val sectionHabitTilesRepository: SectionHabitTilesRepository,
    @CoroutineScopeCommonsModule.IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionHabitTilesData())
    val uiState: StateFlow<SectionHabitTilesData> = _uiState

    fun fetchData(skipCache: Boolean = false) {
        if (!sectionHabitTilesRepository.canRequestPersonalizedData()) {
            Logger.d(TAG, "Skip Habit Tiles API call!")
        } else {
            viewModelScope.launch {
                withContext(ioDispatcher) {
                    sectionHabitTilesRepository.getHabitTilesFeed(skipCache)?.let { result ->
                        _uiState.update {
                            it.copy(
                                tiles = result.tiles,
                                requestId = result.requestId,
                                testGroup = result.testGroup
                            )
                        }
                    } ?: run {
                        Logger.d(TAG, "Error getting getHabitTilesFeed")
                    }
                }
            }
        }
    }

    fun getHabitTiles(): List<SectionHabitTile> = _uiState.value.tiles ?: emptyList()

    fun getRequestId(): String? = _uiState.value.requestId

    fun getTestGroup(): String? = _uiState.value.testGroup

    companion object {
        private const val TAG = "SectionsHabitTilesViewModel"
    }
}
