/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv.util

import androidx.lifecycle.ViewModel

/**
 * ViewModel to maintain [com.wapo.flagship.features.posttv.players.PostTvPlayer2] state.
 * [com.wapo.flagship.features.posttv.PostTvPlayer2Manager] maintains the player and its corresponding
 * view model classes to save and restore the its state in life cycle callback methods.
 * Activities are still the owners of these view models and set these while creating the
 * managers classes.
 */
open class Player2ViewModel: ViewModel() {
    // Video id
    var id: String? = null
    // to store player's players position while pausing and resuming the player
    var playbackPosition = 0L
    // to maintain Player's current window
    var currentWindow = 0
    // to maintain Player's current play state
    var playWhenReady = true
    // to maintain Player's error
    var errorState = false
    // FullScreen State
    var isInFullScreen = false
}