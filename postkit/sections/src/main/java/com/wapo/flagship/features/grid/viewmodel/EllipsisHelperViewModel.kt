/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

/**
 * Helper viewmodel to pass data from AudioStyleCarouselViewHolder -> FusionSectionFragment -> EllipsisMenuFragment
 */
@HiltViewModel
class EllipsisHelperViewModel @Inject constructor() : ViewModel() {

    companion object {
        val tag: String = EllipsisHelperViewModel::class.java.simpleName
    }

    /**
     * LiveEvent that triggers the opening of the Menu
     */
    private val _ellipsisClickEvent = LiveEvent<EllipsisActionItem>()
    val ellipsisClickEvent: LiveData<EllipsisActionItem> = _ellipsisClickEvent

    private val _backToFrontEvent = LiveEvent<Boolean>()
    val backToFrontEvent: LiveData<Boolean> = _backToFrontEvent

    fun handleEllipsisClick(actionItem: EllipsisActionItem) {
        _ellipsisClickEvent.value = actionItem
    }

    fun setBackToFront() {
        _backToFrontEvent.value = true
    }

    /**
     * LiveEvent to listen to menu item being clicked
     */
    private val _choiceClickEvent = LiveEvent<EllipsisHelperAction>()
    val choiceClickEvent: LiveData<EllipsisHelperAction> = _choiceClickEvent

    fun handleActionShare() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionShare
    }

    fun handleActionSave() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionSaveStory
    }

    fun handleActionRemove() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionRemoveSavedStory
    }

    fun handleActionRead() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionRead
    }

    fun handleActionGift() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionGift
    }

    fun handleActionAddToPlayList() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionAddToPlayList
    }

    fun handleActionRemoveFromPlaylist() {
        _choiceClickEvent.value = EllipsisHelperAction.ActionRemoveFromPlaylist
    }
}