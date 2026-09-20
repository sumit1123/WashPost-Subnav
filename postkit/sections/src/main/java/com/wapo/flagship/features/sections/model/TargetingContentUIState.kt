/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.model

/**
 * UI State class to handle ads targeting content api response state from app module
 */
sealed class TargetingContentUIState(val id: String) {
    class Loading(id: String) : TargetingContentUIState(id)
    class Content(id: String, val items: List<TargetingContent>) : TargetingContentUIState(id)
    class Error(id: String) : TargetingContentUIState(id)
    class UITimeout(id: String) : TargetingContentUIState(id)
    class Cancelled(id: String) : TargetingContentUIState(id)
}