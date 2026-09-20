package com.wapo.flagship.features.nightmode

/**
 * Night mode status class that defines 3 different states of night mode / dark mode for app.
 * [value] is basically a [String] representation of [Integer] value that is bound with the drop down in the the settings preferences.
 */
enum class NightModeStatus(val value: String) {
    /**
     * This indicates a night mode status which is an explicitly turned OFF dark mode by the user
     */
    LIGHT_MODE(value = "0"),
    /**
     * This indicates a night mode status which is an explicitly turned ON dark mode by the user
     */
    DARK_MODE(value = "1"),
    /**
     * This indicates a night mode status which is not explicit i.e. depending on the system setting the night mode / dark mode will be
     * activated / deactivated.
     */
    SYSTEM_SETTING(value = "2");

    companion object {
        /**
         * This function returns the night mode status provided the [value] of the status in [String].
         */
        @JvmStatic
        fun getNightModeStatusByValue(value: String?): NightModeStatus {
            return values().firstOrNull { value == it.value } ?: SYSTEM_SETTING
        }
    }
}


