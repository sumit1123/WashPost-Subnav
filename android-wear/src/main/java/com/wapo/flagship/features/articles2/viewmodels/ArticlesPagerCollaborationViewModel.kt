/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.models.UserEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArticlesPagerCollaborationViewModel : ViewModel() {

    private val _dotVisibility = MutableStateFlow(true)
    val dotVisibility = _dotVisibility.asStateFlow()

    private val _userEvent = Channel<UserEvent>()
    val userEvent = _userEvent.receiveAsFlow()

    fun setDotVisibility(value: Boolean) {
        _dotVisibility.value = value
    }

    fun sendUserEvent(event: UserEvent) {
        viewModelScope.launch {
            _userEvent.send(event)
        }
    }

}