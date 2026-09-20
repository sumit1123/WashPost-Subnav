package com.wapo.android.commons.appsFlyer

/**
 * This interface is used to communicate with OneLink (AppsFlyer) to register a deeplink listener [OneLinkListener] in AppsFlyer initialization.
 */
interface OneLinkInterface {
    /**
     * Called when a valid OneLink deeplink request is ready to be dispatched to the app's general [DeepLinksProcessor].
     */
    fun onDeepLink(deepLink: String)

    /**
     * Called when a valid OneLink deeplink request contains an authentication token which we can log the user in with.
     */
    fun onAuthentication(oneLinkToken: String)

    /**
     * Called in various OneLink error states when attempting to resolve a deeplink.
     */
    fun onError(msg: String?)
}