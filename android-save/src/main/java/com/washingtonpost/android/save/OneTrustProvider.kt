package com.washingtonpost.android.save

/**
 * This helper interface provides necessary OT info from the component that implements this interface (Preferably an activity)
 * to other components that require this info.
 */
interface OneTrustProvider {
    /**
     * Returns true if consent for the targeting preference is provided by the user
     * false otherwise.
     */
    fun isTargetingConsentProvided(): Boolean

    /**
     * Shows the OT consent dialog.
     */
    fun showOneTrustPreferenceDialog()
}