package com.wapo.flagship.features.gifting.states

/**
 * States to handle bottom sheet dialog UI when Gift Toolbar icon is clicked
 */
sealed class GiftSendUiState {
    /**
     * Loading state will show when making request to Gift API
     * and waiting for response.
     */
    object Startup : GiftSendUiState()

    /**
     * Loading state will show when making request to Gift API
     * and waiting for response.
     */
    object SignedInSub : GiftSendUiState()

    /**
     * Loading state will show when making request to Gift API
     * and waiting for response.
     */
    object Loading : GiftSendUiState()

    /**
     * Failure message will show when requet to Gift API fails.
     */
    object Failure : GiftSendUiState()

    /**
     * Send message will show when gifting a NEW article.
     * [giftCount] - remaining gifts left
     */
    class Gift(
        val giftCount: Int,
    ) : GiftSendUiState()

    /**
     * Resend message will show when re-gifting and article.
     * * [giftCount] - remaining gifts left
     */
    class GiftAgain(
        val giftCount: Int,
    ) : GiftSendUiState()

    /**
     * No Gifts left message will show when Subscribe has 0 gifts to share.
     */
    object NoGifts : GiftSendUiState()

    /**
     * No Subscription message will show when user is not subscribed.
     */
    object NoSub : GiftSendUiState()

    /**
     * Not Signed In message will show when user is not subscribed.
     */
    object NotSignedIn : GiftSendUiState()

    /**
     * This will be posted when Url is retrieved from API for gifting
     * * [url] - bitly url of article with token param
     */
    class GiftTokenUrl(
        val url: String,
    ) : GiftSendUiState()
}
