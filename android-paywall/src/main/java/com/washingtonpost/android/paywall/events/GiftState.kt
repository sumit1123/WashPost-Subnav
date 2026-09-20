package com.washingtonpost.android.paywall.events

/**
 * These states will be set based on response from Tetro when sending Gift Token (pwapi_token)
 * in the request
 */
sealed class GiftState {
    /**
     * Article url contains token and should be processed
     */
    object None : GiftState()

    /**
     * Token is valid and has not expired
     */
    object ValidNotExpired : GiftState()

    /**
     * Token is invalid
     */
    object NotGift : GiftState()

    /**
     * Token has expired
     */
    object Expired : GiftState()

    /**
     * Article url contains token and should be processed
     */
    class Failure(val message: String?) : GiftState()

    companion object {
        private const val VALID_NOT_EXPIRED_CODE = "w_1610"
        private const val EXPIRED_CODE = "w_1698"

        /**
         * Handle conversion from Action Codes to GiftState.
         */
        @JvmStatic
        fun getGiftStateFromActionCode(codes: List<String>?): GiftState {
            return when {
                codes?.contains(VALID_NOT_EXPIRED_CODE) == true -> ValidNotExpired
                codes?.contains(EXPIRED_CODE) == true -> Expired
                else -> NotGift
            }
        }
    }
}