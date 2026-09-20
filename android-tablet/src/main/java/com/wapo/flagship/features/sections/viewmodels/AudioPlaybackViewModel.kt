// Copyright (c) 2023 The Washington Post. All rights reserved.
package com.wapo.flagship.features.sections.viewmodels

import androidx.lifecycle.ViewModel
import com.washingtonpost.android.paywall.PaywallService

/**
 * ViewModel to handle Audio
 */
class AudioPlaybackViewModel() : ViewModel() {
    fun hasAccessToAudioArticle(): Boolean =
        PaywallService.getInstance()?.let {
            it.isPremiumUser || it.isWpUserLoggedIn
        } ?: true
}
