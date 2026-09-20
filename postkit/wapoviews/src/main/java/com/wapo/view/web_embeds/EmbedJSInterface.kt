package com.wapo.view.web_embeds

import android.webkit.JavascriptInterface
import android.webkit.WebView

interface EmbedJSInterface {
    @JavascriptInterface
    fun triggerDisplayEmbed()

    @JavascriptInterface
    fun trackEvent(name: String, webViewDump: String)

    companion object {
        const val JS_ID = "EmbedJSInterface"
    }
}

fun WebView.addEmbedJsInterface(impl: EmbedJSInterface) {
    addJavascriptInterface(impl, EmbedJSInterface.JS_ID)
}