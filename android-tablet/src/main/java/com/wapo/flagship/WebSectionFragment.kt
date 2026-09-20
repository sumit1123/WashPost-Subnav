package com.wapo.flagship

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.washingtonpost.android.R

/**
 * Single section fragment with WebView. You need to provide the section name and a valid full URL to a web page
 */
open class WebSectionFragment : BaseSectionFragment() {
    open lateinit var webView: WebView
    open lateinit var pullToRefreshView: SwipeRefreshLayout
    open lateinit var progressBar: ProgressBar

    /**
     * @url - Full URL to a web page. This is the Bundle Name for a Web Section.
     * @displayName - Section Display Name
     */
    fun create(
        url: String?,
        displayName: String?,
    ): WebSectionFragment {
        val arg = arguments ?: Bundle()
        arg.putString(ARG_URL, url)
        arg.putString(ARG_DISPLAY_NAME, displayName)
        arguments = arg
        return this
    }

    override fun getSectionDisplayName(): String = arguments?.getString(ARG_DISPLAY_NAME) ?: ""

    override fun getAdKey(): String? = null

    private fun getUrl(): String = arguments?.getString(ARG_URL) ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_web_section, container, false)
        progressBar = view.findViewById(R.id.progressBar)
        progressBar.progress = 0
        pullToRefreshView = view.findViewById(R.id.pullToRefreshView)
        pullToRefreshView.setOnRefreshListener {
            webView.reload()
            webView.postDelayed({ pullToRefreshView.isRefreshing = false }, 10_000)
        }
        webView = view.findViewById(R.id.web_view)
        onWebViewReady(view, webView)
        return view
    }

    open fun onWebViewReady(
        view: View,
        webView: WebView,
    ) {
        webView.webChromeClient = createWebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.loadUrl(getUrl())
    }

    open fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            var customView: View? = null
            private var originalSystemUiVisibility: Int = 0
            private var originalOrientation: Int = 0
            private var customViewCallback: CustomViewCallback? = null

            override fun onProgressChanged(
                view: WebView?,
                newProgress: Int,
            ) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress)
            }

            override fun onShowCustomView(
                view: View,
                callback: CustomViewCallback,
            ) {
                super.onShowCustomView(view, callback)
                customView = view
                val activity = activity ?: return

                originalSystemUiVisibility = activity.window.decorView.systemUiVisibility
                originalOrientation = activity.requestedOrientation
                customViewCallback = callback
                (activity.window.decorView as FrameLayout).addView(
                    customView,
                    FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT),
                )
                activity.window.decorView.systemUiVisibility =
                    SYSTEM_UI_FLAG_IMMERSIVE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                val activity = activity ?: return
                (activity.window.decorView as FrameLayout).removeView(customView)
                customView = null
                activity.window.decorView.systemUiVisibility = this.originalSystemUiVisibility
                activity.requestedOrientation = this.originalOrientation
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
        }
    }

    open fun onProgressChanged(newProgress: Int) {
        progressBar.progress = newProgress
        if (newProgress == 100) {
            progressBar.visibility = View.GONE
            pullToRefreshView.isRefreshing = false
        } else {
            progressBar.visibility = View.VISIBLE
        }
    }

    override fun scrollToTop() {
        // do nothing
    }

    override fun smoothScrollToTop() {
        // do nothing
    }

    override fun getTracking(): Tracking? = null

    /**
     * URL and Bundle Name are the same for a Web Section
     */
    override fun getBundleName(): String? = getUrl()

    companion object {
        const val ARG_URL = "ARG_URL"
        const val ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME"
    }
}
