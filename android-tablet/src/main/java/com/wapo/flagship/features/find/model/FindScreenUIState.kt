package com.wapo.flagship.features.find.model

sealed interface FindScreenUIState {
    object Loading : FindScreenUIState

    object Failure : FindScreenUIState

    class Success(
        val list: List<FindItem>,
        val isAzDouble: Boolean = false,
    ) : FindScreenUIState
}
