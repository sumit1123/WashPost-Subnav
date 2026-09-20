package com.wapo.zendesk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


/**
 * Activity scoped view model that can be used to send events between fragments and activity
 */
class ZendeskSharedViewModel: ViewModel() {

    private val _actions = MutableLiveData<Action>()
    val actions: LiveData<Action> = _actions

    fun dispatchAction(action: Action) {
        _actions.postValue(action)
    }
}

sealed class Action {
    object ContactUsClick : Action()
}