package com.wapo.flagship.navigation.viewmodel.sectionnav

import com.wapo.flagship.navigation.ui.TopBarState
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

data class SectionNavUiState(
    val topBarState: TopBarState = TopBarState.EXPANDED,
    val sectionTitle: String? = null,
    val bottomSheetPrompt: BannerPaywallMessage? = null,
)
