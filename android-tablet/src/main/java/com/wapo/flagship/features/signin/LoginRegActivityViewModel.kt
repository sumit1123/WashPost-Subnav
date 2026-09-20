package com.wapo.flagship.features.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginRegActivityViewModel
    @Inject
    constructor() : ViewModel() {
        private val _shouldReloadWebView = LiveEvent<Boolean>()
        val shouldReloadWebView: LiveData<Boolean> = _shouldReloadWebView

        fun reloadWebView() {
            _shouldReloadWebView.postValue(true)
        }
    }
