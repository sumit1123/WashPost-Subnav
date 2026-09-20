/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts

import android.content.Context
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.tts.domain.TtsNotificationAction

class TtsNotificationActionImpl: TtsNotificationAction {

    override fun openIntent(context: Context) = IntentHelper.getMainActivityIntent(context)
}
