package com.wapo.flagship.features.signin

import android.content.Intent

sealed class LoginRegWebviewEvent {
    class LoadUrl(
        val url: String,
    ) : LoginRegWebviewEvent()

    class LaunchBrowser(
        val url: String,
    ) : LoginRegWebviewEvent()

    class LaunchAppWebview(
        val url: String,
    ) : LoginRegWebviewEvent()

    class Success(
        val message: String? = null,
    ) : LoginRegWebviewEvent()

    class Error(
        val message: String? = null,
    ) : LoginRegWebviewEvent()

    class LaunchActivity(
        val intent: Intent,
    ) : LoginRegWebviewEvent()
}
