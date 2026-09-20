package com.washingtonpost.android.paywall.bottomsheet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PostIterableEventViewModel : ViewModel() {
    private val _postSignInOrSubscribeIterableEvent = MutableLiveData<PostIterableEventType?>(null)
    val postSignInOrSubscribeIterableEvent: LiveData<PostIterableEventType?> = _postSignInOrSubscribeIterableEvent

    fun postSignInOrSubscribeEvent(eventType: PostIterableEventType) {
        _postSignInOrSubscribeIterableEvent.value = eventType
    }

    fun resetPostSignInOrSubscribeEvent() {
        _postSignInOrSubscribeIterableEvent.value = null
    }
}

enum class PostIterableEventType {
    SIGN_IN_OR_REGISTER,
    SUBSCRIBE
}