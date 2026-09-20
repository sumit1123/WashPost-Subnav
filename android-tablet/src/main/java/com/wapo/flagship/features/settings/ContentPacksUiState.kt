package com.wapo.flagship.features.settings

import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem

sealed class ContentPacksUiState {

    /**
     * Loading state will show when making request to Content Packs API
     * and waiting for response.
     */
    object Loading : ContentPacksUiState()

    /**
     * Failure message will show when request to Content Packs API fails.
     */
    object Failure : ContentPacksUiState()

    /**
     * Displays the content packs.
     */
    data class Success(val contentPacks: List<ContentPackUiItem?>) : ContentPacksUiState()
}
