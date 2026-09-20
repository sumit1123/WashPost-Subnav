/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.wpvideos.models

sealed class WatchVideosEvent {

    data object WatchVideosReady: WatchVideosEvent()
}
