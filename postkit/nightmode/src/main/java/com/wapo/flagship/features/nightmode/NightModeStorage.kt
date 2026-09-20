package com.wapo.flagship.features.nightmode

interface NightModeStorage {
    fun setNightMode(status: Boolean)

    @Deprecated("Use isNightModeEnabled()")
    fun readNightModeStatus(): Boolean

    fun isNightModeEnabled(): Boolean

    /**
     * Sets if user made an explicit action of enabling/disabling night mode.
     */
    fun setUserExplicitlySelectedAMode()

    /**
     * Resets if user made a selection of "system setting" in night mode dropdown.
     */
    fun resetUserExplicitlySelectedAMode()

    /**
     * Returns true if user explicitly enabled/disabled night mode, false otherwise.
     */
    fun hasUserExplicitlySelectedAMode(): Boolean

    /**
     * Sets if user's legacy version (prior to 4.45) has already been respected and need not be considered
     * again in the future.
     */
    fun setLegacyVersionSettingRespected()

    /**
     * Returns true if the legacy version (prior to 4.45) setting has been respected in newer implementation.
     * False otherwise.
     */
    fun hasLegacyVersionSettingRespected(): Boolean
}