package com.washingtonpost.android.paywall.models

/**
 * This is the state of the the promo code request made using the verify device subscription call/shared prefs.
 */
sealed class PromoCodeRequestState{
    /**
     * Request failed for any reason e.g. network issues etc.
     */
    object Failure: PromoCodeRequestState()

    /**
     * Request succeeded.
     * [promocode] Retrieved promocode from the request or shared prefs.
     */
    class Success(val promocode: PromoCode): PromoCodeRequestState()
}
