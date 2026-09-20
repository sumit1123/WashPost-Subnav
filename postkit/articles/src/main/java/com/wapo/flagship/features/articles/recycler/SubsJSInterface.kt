package com.wapo.flagship.features.articles.recycler

import android.webkit.JavascriptInterface

interface SubsJSInterface {
    @JavascriptInterface
    fun showPaywall(url: String, response: String)

    @JavascriptInterface
    fun showSignIn()

    @JavascriptInterface
    fun showProductPage()

    @JavascriptInterface
    fun printContent()

    @JavascriptInterface
    fun triggerProfileRefresh()

    @JavascriptInterface
    fun showSignUp()

    @JavascriptInterface
    fun triggerSavedStoriesRefresh()

    @JavascriptInterface
    fun triggerFollowAuthorsRefresh()

    @JavascriptInterface
    fun triggerNewslettersRefresh()

    @JavascriptInterface
    fun triggerPreferencesRefresh()

    @JavascriptInterface
    fun closePage()

    @JavascriptInterface
    fun share(shareUrl: String? = null, shareText: String? = null, shareTitle: String? = null, base64Image: String? = null)

    @JavascriptInterface
    fun fetchProductOffer(product: String, offer: String? = null)

    @JavascriptInterface
    fun purchaseProductOffer(product: String, offer: String? = null)

    @JavascriptInterface
    fun enablePush()

    @JavascriptInterface
    fun playNativeAudio(jsonPayload: String)

    companion object {
        const val JS_ID = "SubsJSInterface"
        const val NA_JS_ID = "NativeJSInterface"
    }
}
