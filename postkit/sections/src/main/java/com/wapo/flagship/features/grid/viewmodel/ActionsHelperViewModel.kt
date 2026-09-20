/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.grid.events.ActionButtonEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Helper viewmodel to pass data from AudioStyleCarouselViewHolder -> FusionSectionFragment -> EllipsisMenuFragment
 */
@HiltViewModel
class ActionsHelperViewModel @Inject constructor() : ViewModel() {

    companion object {
        val tag: String = ActionsHelperViewModel::class.java.simpleName
    }

    /**
     * LiveEvent that triggers the opening of the Menu
     */
    private val _actionEvent = LiveEvent<ActionButtonEvent>()
    val actionEvent: LiveData<ActionButtonEvent> = _actionEvent

    fun handleActionClick(actionItem: ActionButtonEvent) {
        _actionEvent.value = actionItem
    }
}