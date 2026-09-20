package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.http.SslError
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemArticleEmbedBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EmbedViewHolder(
    private val binding: ItemArticleEmbedBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<SanitizedHtml>(
        binding.root,
        binding.placeholder,
    ) {
    private var webViewUpdates: Job? = null

    init {
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true
    }

    override fun setPlaceHolderData(item: SanitizedHtml): PlaceHolderData {
        val resources = binding.root.context.resources
        return PlaceHolderData(
            message = resources.getString(R.string.low_data_mode_twitter_message),
        ) {
            setWebContent(item)
        }
    }

    override fun onLowDataModeEnable(item: SanitizedHtml) {
        binding.apply {
            webview.isVisible = false
            embedError.isVisible = false
            placeholder.isVisible = true
        }
    }

    override fun onLowDataModeDisable(item: SanitizedHtml) {
        setWebContent(item)
    }

    private fun setWebContent(item: SanitizedHtml) {
        resetUI()
        item.isItemAlreadyShowed = true
        val data = item.oembed
        if (data != null) {
            listenToWebViewUpdates(item)
            val wrappedData =
                TWITTER_WRAPPER
                    .replace("{{{DATA}}}", data)
                    .replace("{{{THEME}}}", getThemeForTwitter(binding.root.context))
                    .replace("{{{BG_COLOR}}}", getBackgroundColorForTwitter(binding.root.context))
            val encodedData = Base64.encodeToString(wrappedData.toByteArray(), Base64.NO_PADDING)
            binding.webview.loadData(encodedData, "text/html", "base64")
        } else {
            showError()
        }
    }

    private fun resetUI() {
        binding.apply {
            val layoutParams = embedRoot.layoutParams
            layoutParams.height =
                root.context.resources.getDimensionPixelOffset(
                    R.dimen.embed_placeholder_height,
                )
            embedRoot.layoutParams = layoutParams
            embedRoot.setBackgroundResource(R.color.embed_background)
            webview.visibility = View.GONE
            embedError.visibility = View.GONE
            placeholder.isVisible = false
        }
    }

    private fun listenToWebViewUpdates(item: SanitizedHtml) {
        webViewUpdates?.cancel()
        val webViewFlow = MutableStateFlow<WebViewState>(WebViewState.Idle)
        webViewUpdates =
            binding.root.findComponentActivity()?.lifecycleScope?.launch {
                webViewFlow
                    .filter { it !is WebViewState.Idle }
                    .timeout(3000.milliseconds)
                    .catch {
                        onItemReady(item)
                        if (it is TimeoutCancellationException) {
                            emit(WebViewState.Ready)
                        } else {
                            emit(WebViewState.Error)
                        }
                    }.collect {
                        onItemReady(item)
                        if (it is WebViewState.Error) {
                            showError()
                        } else {
                            showWebView()
                        }
                    }
            }

        binding.webview.webViewClient = EmbedWebViewClient(webViewFlow)
    }

    private fun showWebView() {
        val layoutParams = binding.embedRoot.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.embedRoot.layoutParams = layoutParams
        binding.webview.visibility = View.VISIBLE
        binding.embedRoot.background = null
    }

    private fun showError() {
        binding.webview.visibility = View.GONE
        binding.embedError.visibility = View.VISIBLE
    }

    override fun unbind() {
        super.unbind()
        webViewUpdates?.cancel()
        webViewUpdates = null
    }

    private fun getThemeForTwitter(context: Context): String =
        when (
            context.resources
                ?.configuration
                ?.uiMode
                ?.and(Configuration.UI_MODE_NIGHT_MASK)
        ) {
            Configuration.UI_MODE_NIGHT_YES -> "dark"
            else -> "light"
        }

    private fun getBackgroundColorForTwitter(context: Context): String {
        val color = ContextCompat.getColor(context, R.color.app_bg)
        return String.format("#%06x", color and 0xffffff)
    }
}

private class EmbedWebViewClient(
    private val webViewFlow: MutableStateFlow<WebViewState>,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest?,
    ): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, request?.url)
        view.context.startActivity(intent)
        return true
    }

    override fun onLoadResource(
        view: WebView?,
        url: String?,
    ) {
        super.onLoadResource(view, url)
        webViewFlow.tryEmit(WebViewState.Loading(url ?: ""))
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        webViewFlow.tryEmit(WebViewState.Error)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        webViewFlow.tryEmit(WebViewState.Error)
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?,
    ) {
        super.onReceivedSslError(view, handler, error)
        webViewFlow.tryEmit(WebViewState.Error)
    }
}

private sealed class WebViewState {
    object Idle : WebViewState()

    class Loading(
        val url: String,
    ) : WebViewState()

    object Error : WebViewState()

    object Ready : WebViewState()
}

private const val TWITTER_WRAPPER = """
    <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no">
                <meta name="twitter:widgets:theme" content="{{{THEME}}}">
                <meta name="twitter:dnt" content="on">
                <style>
                    body { background-color: {{{BG_COLOR}}}; }
                </style>
            </head>
            <body>
            <div id="wapo-app-wrapper" align="center">
                {{{DATA}}}
            </div>
            </body>
    </html>
"""
