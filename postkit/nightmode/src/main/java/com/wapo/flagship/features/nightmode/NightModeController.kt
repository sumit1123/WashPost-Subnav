package com.wapo.flagship.features.nightmode

import androidx.appcompat.app.AppCompatActivity

interface NightModeController {
    fun isNightModeEnabled(): Boolean
    fun handleNightMode(activity: AppCompatActivity?)
}