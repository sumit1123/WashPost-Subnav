// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ads.targeting.ui

import com.wapo.flagship.features.ads.targeting.models.ContentItem

sealed class ContentUIState(val id: String) {
    class Content(id: String, val items: List<ContentItem>) : ContentUIState(id)

    class Loading(id: String) : ContentUIState(id)

    class Error(id: String) : ContentUIState(id)

    class Cancelled(id: String) : ContentUIState(id)

    class UITimeout(id: String) : ContentUIState(id)
}