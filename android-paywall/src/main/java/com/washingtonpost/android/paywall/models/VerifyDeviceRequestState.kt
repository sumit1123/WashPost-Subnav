package com.washingtonpost.android.paywall.models

/**
 * This is the state of the Verify Device Request when called in PaywallService.initialize
 */
sealed class VerifyDeviceRequestState {
    /**
     * Verify Device Request failed for any reason e.g. network issues etc.
     */
    object Failure: VerifyDeviceRequestState()

    /**
     * Verify Device Request is in progress
     */
    object InProgress: VerifyDeviceRequestState()

    /**
     * Verify Device Request succeeded
     */
    object Success: VerifyDeviceRequestState()
}
