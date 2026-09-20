package com.wapo.flagship.features.sections.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.washingtonpost.android.paywall.util.PaywallConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SectionWallHelperViewModel : ViewModel() {

    private val _paywallEvent: MutableSharedFlow<WallUiEvent?> = MutableSharedFlow(replay = 0)
    val paywallEvent: SharedFlow<WallUiEvent?> = _paywallEvent

    fun dispatchShowRegwall(
        wallName: String,
        wallType: PaywallConstants.WallType,
    ) {
        viewModelScope.launch {
            _paywallEvent.emit(
                WallUiEvent.ShowRegwall(wallName, wallType)
            )
        }
    }
}