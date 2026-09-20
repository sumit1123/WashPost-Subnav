package com.wapo.flagship.features.signin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.wapo.android.commons.extensions.containsCaseInsensitive
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.mypost.MyPostActivity
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.auth.AuthHelper.AuthListener
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * LoginRegViewModel handles all auth related flows and posts the results back to webview.
 */
@HiltViewModel
class LoginRegViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel(),
        AuthListener {
        /**
         * Handle webview events
         */
        private val _webviewEvent = LiveEvent<LoginRegWebviewEvent>()
        val webviewEvent: LiveData<LoginRegWebviewEvent> = _webviewEvent

        private var authHelper: AuthHelper? = null
        private val paywallPrefHelper = PaywallPrefHelper.getInstance(context)

        init {
            authHelper = AuthHelper.getInstance(context)
        }

        /**
         * Initialize Auth Request and post full url.
         * If there is a redirectUri (e.g. when user returns from third party auth), post that url.
         */
        fun startLoginFlow(
            isSignUp: Boolean,
            isMagicLinkOrSocialRedirect: Boolean,
            magicLinkOrSocialRedirectResponseData: Uri?,
            additionalParams: Map<String, String?>,
            redirectUri: Uri = Uri.EMPTY,
            extras: Bundle? = null,
        ) {
            if (PaywallService.getInstance().isWpUserLoggedIn) PaywallService.getInstance().logOutCurrentUser()
            PaywallService.getInstance().cookiesService.clearCookies()
            PaywallService.getInstance().cookiesService.setWebViewCookie(context)
            if (redirectUri != Uri.EMPTY) {
                authHelper?.setAuthListener(this)
                _webviewEvent.postValue(LoginRegWebviewEvent.LoadUrl(redirectUri.toString()))
            } else {
                authHelper?.initAuthUriForWebView(
                    this,
                    FlagshipApplication.getInstance().appRedirectScheme,
                    additionalParams,
                    isMagicLinkOrSocialRedirect,
                    magicLinkOrSocialRedirectResponseData,
                    isSignUp,
                    extras,
                )
            }
        }

        /**
         * Handle redirects from webviews when shouldOverrideUrlLoading is called.
         * -If the url contains code param, then we have the auth code and can call to token api to complete the sign in flow.
         * -If the url contains state param which contains wpflow=native-android in the json, we need to trigger the third party auth flow
         * and launch the browser with the third party auth url.
         * -If the url is one of the designated app webview urls (e.g. privacy policy, terms of service), then open the full screen app webview
         * -else don't override the url and return false
         */
        fun handleRedirect(url: String?, extras: Bundle? = null): Boolean {
            url ?: return false
            val uri = Uri.parse(url)
            if (uri.getQueryParameter("code") != null) {
                if (PaywallService.getInstance().isWpUserLoggedIn) {
                    onAuthorized()
                } else {
                    authHelper?.startAuthForWebView(uri, extras)
                }
                return true
            } else if (uri.getQueryParameter("state") != null) {
                val stateEncrypted = uri.getQueryParameter("state")
                if (stateEncrypted?.isNotEmpty() == true) {
                    var decryptedState = base64Decode(stateEncrypted)
                    // if apple is 3rd party provider, auth flow contains a nested state that needs to be decrypted and used in place of the top-level state.
                    if (uri.host.containsCaseInsensitive("apple")) {
                        val jsonObject = decryptedState?.let { JSONObject(it) }
                        val urlParams = jsonObject?.optJSONObject("urlParams")
                        val encryptedState = urlParams?.optString("state")
                        decryptedState = encryptedState?.let { base64Decode(it) } ?: decryptedState
                    }
                    try {
                        val json = decryptedState?.let { JSONObject(it) }
                        if (json?.has("wpflow") == true && json.getString("wpflow") == "native-android") {
                            saveActivity()
                            _webviewEvent.postValue(LoginRegWebviewEvent.LaunchBrowser(url))
                        } else {
                            PaywallService.getConnector().logE(
                                EventLog.Builder().setMessage(
                                    "wpflow missing in state for third party redirect",
                                ),
                            )
                            _webviewEvent.postValue(LoginRegWebviewEvent.Error())
                        }
                    } catch (e: Exception) {
                        PaywallService.getConnector().logE(
                            EventLog
                                .Builder()
                                .setMessage("Error parsing state in redirect")
                                .setErrorMessage(e.message),
                        )
                        _webviewEvent.postValue(LoginRegWebviewEvent.Error())
                    }
                } else {
                    PaywallService.getConnector().logE(
                        EventLog.Builder().setMessage("state empty for third party redirect"),
                    )
                    _webviewEvent.postValue(LoginRegWebviewEvent.Error())
                }
                return true
            } else if (DeepLinksProcessor.shouldDelegateToAppWebView(URLParser(url))) {
                _webviewEvent.postValue(LoginRegWebviewEvent.LaunchAppWebview(url))
                return true
            }
            return false
        }

        /**
         * AuthHelper calls this once its initialization of the service and auth request is done and returns the full url,
         * whether it's a magic link and the associated magic link data.
         */
        override fun onInitialized(
            uri: Uri,
            isMagicLink: Boolean,
            magicLinkOrSocialRedirectResponseData: Uri?,
            extras: Bundle?,
        ) {
            if (isMagicLink && magicLinkOrSocialRedirectResponseData != null) {
                handleRedirect(magicLinkOrSocialRedirectResponseData.toString(), extras)
            } else {
                _webviewEvent.postValue(LoginRegWebviewEvent.LoadUrl(uri.toString()))
            }
        }

        /**
         * User has been successfully authorization and we can post a message back to the webview.
         * Clean up AuthHelper instance
         */
        override fun onAuthorized() {
            _webviewEvent.postValue(LoginRegWebviewEvent.Success("Thank you for signing in!"))
            PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
            PreferencesSyncCoordinator.synchronize(context)
            cleanUp()
        }

        /**
         * Something went wrong during authorization and we can post the error message back to the webview.
         * Clean up AuthHelper instance
         */
        override fun onAuthorizeError(errorMessage: String?) {
            _webviewEvent.postValue(LoginRegWebviewEvent.Error(errorMessage))
            cleanUp()
        }

        /**
         * Clean up AuthHelper instance. onStopped shuts down the ExecutorService and onDestroyed
         * disposes of the AuthorizationService.
         */
        private fun cleanUp() {
            authHelper?.onStopped()
            authHelper?.onDestroyed()
        }

        /**
         *
         */
        private fun base64Decode(string: String): String? =
            try {
                val bytes =
                    Base64.decode(string, Base64.DEFAULT)
                String(bytes)
            } catch (e: Exception) {
                null
            }

        /**
         * If the current activity is an [Articles2Activity]:
         * - Save the current activity's article list to [PaywallPrefHelper.PREF_ARTICLES_META_LIST].
         * - Save the article index to [PaywallPrefHelper.PREF_CURRENT_ARTICLE_URL].
         * If it is not an [Articles2Activity]: saves the previous screen the user was on.
         */
        fun saveActivity() {
            val activity = FlagshipApplication.getInstance().currentActivity
            if (activity is Articles2Activity) {
                val articles2Activity = activity as Articles2Activity
                paywallPrefHelper.articlesMetaList = Gson().toJson(articles2Activity.getArticlesList())
                paywallPrefHelper.currentArticleUrl = articles2Activity.getCurrentArticleUrl()
            }

            if (paywallPrefHelper.previousScreen.isNullOrEmpty()) {
                paywallPrefHelper.previousScreen = activity?.javaClass?.name
            }

            if (activity is MyPostActivity) {
                authHelper?.saveAuthEntryPoint(AuthEntryPoint.MY_POST)
            }
        }
    }
