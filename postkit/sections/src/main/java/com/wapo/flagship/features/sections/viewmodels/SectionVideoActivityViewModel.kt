/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.sections.model.TargetingContentUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SectionVideoActivityViewModel @Inject constructor() : ViewModel() {

    private val _videoClickEvent = LiveEvent<String>()
    val videoClickEvent: LiveData<String> = _videoClickEvent

    private val _targetingContentUIStateMap: HashMap<String, MutableLiveData<TargetingContentUIState?>> =
        HashMap()
    val targetingContentUIStateMap: Map<String, LiveData<TargetingContentUIState?>> =
        _targetingContentUIStateMap

    fun dispatchVideoClickEvent(videoId: String?) {
        val id = videoId ?: return
        if (id.isNotEmpty()) {
            _videoClickEvent.postValue(id)
        }
    }

    fun createContentUIState(videoId: String) {
        if (videoId.isEmpty()) return
        if (_targetingContentUIStateMap.containsKey(videoId)) return
        _targetingContentUIStateMap[videoId] = MutableLiveData()
    }

    fun updateContentUIState(videoId: String,
                             state: TargetingContentUIState?) {
        if (videoId.isEmpty()) return
        if (!_targetingContentUIStateMap.containsKey(videoId)) return
        _targetingContentUIStateMap[videoId]?.value = state
    }

    fun clearContentUIState(videoId: String) {
        if (videoId.isEmpty()) return
        if (!_targetingContentUIStateMap.containsKey(videoId)) return
        _targetingContentUIStateMap.remove(videoId)
    }

    override fun onCleared() {
        _targetingContentUIStateMap.clear()
    }
}