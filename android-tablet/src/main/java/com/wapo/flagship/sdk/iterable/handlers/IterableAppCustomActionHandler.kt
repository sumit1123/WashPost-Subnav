// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.handlers

import com.iterable.iterableapi.IterableAction
import com.iterable.iterableapi.IterableActionContext
import com.iterable.iterableapi.IterableActionSource
import com.iterable.iterableapi.IterableCustomActionHandler
import com.wapo.android.commons.util.Logger

class IterableAppCustomActionHandler(
    private val inAppHandler: IterableAppInAppHandler
) : IterableCustomActionHandler {

    override fun handleIterableCustomAction(
        action: IterableAction,
        actionContext: IterableActionContext
    ): Boolean {
        Logger.d(TAG, "Iterable, CustomAction, type=${action.type}")
        if (actionContext.source == IterableActionSource.IN_APP) {
            inAppHandler.onMessageFinished(action.type)
            // Return true instead of letting Iterable handle the action.
            // Using custom actions to dismiss/close the messages. So deep linking is not required and
            // revisit the requirements if needed later.
            return true
        } else {
            return false
        }
    }

    companion object {
        private const val TAG = "IterableAppCustomActionHandler"
    }
}
