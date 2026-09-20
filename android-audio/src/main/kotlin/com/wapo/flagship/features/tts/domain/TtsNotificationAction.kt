/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import android.content.Context
import android.content.Intent

interface TtsNotificationAction {

    fun openIntent(context: Context): Intent
}
