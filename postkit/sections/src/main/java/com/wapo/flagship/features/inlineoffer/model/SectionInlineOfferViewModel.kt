package com.wapo.flagship.features.inlineoffer.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SectionInlineOfferViewModel @Inject constructor() : ViewModel() {
    private val _sectionMessageData: MutableLiveData<SectionInlineMessage?> = MutableLiveData()
    val sectionMessageData: LiveData<SectionInlineMessage?> = _sectionMessageData

    fun setSectionMessageData(offerData: SectionInlineMessage?) {
        _sectionMessageData.value = offerData
    }
}
