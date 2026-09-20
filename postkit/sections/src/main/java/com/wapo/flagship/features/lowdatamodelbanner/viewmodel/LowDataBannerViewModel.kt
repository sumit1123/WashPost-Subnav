/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.lowdatamodelbanner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LowDataBannerViewModel @Inject constructor() : ViewModel() {

    private val _lowDataBannerState: MutableLiveData<LowDataBanner> = MutableLiveData(LowDataBanner(false))
    val lowDataBannerState: LiveData<LowDataBanner> = _lowDataBannerState

    fun updateLowDataBanner(enable: Boolean) {
        _lowDataBannerState.postValue(LowDataBanner(enable))
    }
}
