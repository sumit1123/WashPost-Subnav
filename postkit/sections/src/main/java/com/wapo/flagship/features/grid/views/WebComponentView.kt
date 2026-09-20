/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Utils
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.view.NestedScrollWebView
import com.wapo.view.web_embeds.EmbedJSInterface
import com.wapo.view.web_embeds.addEmbedJsInterface
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebComponentView : NestedScrollWebView {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    )

    var isLoaded = false

    lateinit var componentUrl: String

    @Inject
    lateinit var embedJSInterface: EmbedJSInterface

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    fun init() {
        initWebView(
            /* setJavaScriptEnabled = */ true,
            /* setAllowContentAccess = */ true,
            /* setDomStorageEnabled = */ true,
            /* setLoadWithOverviewMode = */ false,
            /* setUseWideViewPort = */ false,
            /* setUseTextZoom = */ true
        )
        isNestedScrollingEnabled = false
        isScrollContainer = false
        isFocusableInTouchMode = true

        setPageLoadingListener(object : PageLoadingListener {
            override fun onProgressChanged(newProgress: Int) {
            }

            override fun onPageStarted(url: String?) {
            }

            override fun onPageFinished(url: String?) {
                isLoaded = true
                val params = LayoutParams(layoutParams.width, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams = params
            }

            override fun onReceiveError(errorCode: Int, description: String?) {}

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                incomingUrl: String?,
                isRedirect: Boolean
            ): Boolean {
                if (!isRedirect && isLoaded && incomingUrl != null && componentUrl != incomingUrl) {
                    context.findActivityOfType<SectionActivity>()?.openWebEmbed(incomingUrl)
                    EventLog.Builder().apply {
                        setMessage("WebEmbed tap through")
                        setModule(LogModules.SECTIONS)
                        set("web_embed_url",componentUrl)
                        set("cta",incomingUrl)
                    }.run {
                        RemoteLog.d(context, build())
                    }
                    return true
                }
                return false
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                errorResponse ?: return
                // Ensure the request is not null and is for the main frame
                request?.let {
                    if (it.isForMainFrame) {
                        // Reload if not loaded
                        if (!isLoaded) {
                            reload()
                        }
                        // Set visibility based on error status code
                        this@WebComponentView.visibility =
                            if (errorResponse.statusCode >= 400) View.INVISIBLE else View.VISIBLE
                        // Splunk log error event
                        if (!isLoaded) {
                            EventLog.Builder().apply {
                                setMessage("WebEmbed Error")
                                setModule(LogModules.SECTIONS)
                                setContentUrl(it.url.toString())
                                setErrorCode(errorResponse.statusCode)
                            }.run {
                                RemoteLog.e(context, build())
                            }
                        }
                    }
                }
            }
        })

        addEmbedJsInterface(embedJSInterface)
    }

    fun loadComponent(url: String) {
        componentUrl = url
        if (!Utils.isConnectedOrConnecting(context)) {
            // Display error if required.
        } else {
            loadUrl(url)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        isLoaded = false
        super.onConfigurationChanged(newConfig)
    }
}
