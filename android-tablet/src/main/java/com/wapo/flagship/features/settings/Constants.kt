package com.wapo.flagship.features.settings

import com.wapo.flagship.features.ask.models.VoiceOption

/**
 * Open specific preference from settings.
 */
const val OPEN_PREF = "open_preference"

/**
 * Open contact us from settings
 */
const val CONTACT_US = "contact_us"

/**
 * Action name for various actions within settings
 */
const val SETTING_ACTION = "settings_action"

/**
 * Caller for the setting action
 */
const val SETTING_ACTION_CALLER = "settings_action_caller"

/**
 * Daily Read segment
 */
const val DAILY_READ = "daily_read"

const val ASK_SAM_JUNIPER_ID = "juniper"
const val ASK_SAM_JUNIPER_DISPLAY_NAME = "Voice 1"
const val ASK_SAM_JEFF_ID = "jeff"
const val ASK_SAM_JEFF_DISPLAY_NAME = "Voice 2"

val ASK_SAM_VOICE_OPTIONS = listOf(
    VoiceOption(
        ASK_SAM_JUNIPER_ID,
        ASK_SAM_JUNIPER_DISPLAY_NAME
    ),
    VoiceOption(
        ASK_SAM_JEFF_ID,
        ASK_SAM_JEFF_DISPLAY_NAME
    )
)

/**
 * Different callers that might call the settings action.
 */
enum class SettingsActionCaller(
    val callerName: String,
) {
    CA_SETTLEMENT_DIALOG("CA_SETTLEMENT_DIALOG"),
}
