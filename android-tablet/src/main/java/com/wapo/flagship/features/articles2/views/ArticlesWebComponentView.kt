package com.wapo.flagship.features.articles2.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import com.wapo.android.commons.util.Utils
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.view.NestedScrollWebView
import com.wapo.view.web_embeds.EmbedJSInterface
import com.wapo.view.web_embeds.addEmbedJsInterface
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
@Deprecated("Use com.wapo.flagship.features.articles3.views.WebEmbedView to render web embeds on articles")
open class ArticlesWebComponentView : NestedScrollWebView {
    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr,
    )

    var isLoaded = false

    lateinit var componentUrl: String

    private var downX: Float = 0.0f
    private var downY: Float = 0.0f

    @Inject
    lateinit var embedJSInterface: EmbedJSInterface

    fun init(progressBar: ProgressBar) {
        initWebView()
        settings.setCacheMode(WebSettings.LOAD_DEFAULT)
        isNestedScrollingEnabled = false

        setPageLoadingListener(
            object : PageLoadingListener {
                override fun onProgressChanged(newProgress: Int) {
                }

                override fun onPageStarted(url: String?) {
                    progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(url: String?) {
                    progressBar.visibility = View.GONE
                    isLoaded = true
                    val params = LayoutParams(layoutParams.width, ViewGroup.LayoutParams.WRAP_CONTENT)
                    layoutParams = params
                }

                override fun onReceiveError(
                    errorCode: Int,
                    description: String?,
                ) {
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?,
                ) {
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    incomingUrl: String?,
                    isRedirect: Boolean,
                ): Boolean {
                    if (!isRedirect && isLoaded && incomingUrl != null && componentUrl != incomingUrl) {
                        context.findActivityOfType<Articles2Activity>()?.openWebEmbed(incomingUrl)
                        return true
                    }
                    return false
                }
            },
        )

        addEmbedJsInterface(embedJSInterface)
    }

    fun loadComponent(url: String): Boolean {
        if (!Utils.isConnectedOrConnecting(context)) {
            // Display error if required.
            return false
        } else {
            componentUrl = url
            loadUrl(url)
            return true
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        parent.parent.parent.requestDisallowInterceptTouchEvent(true)
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - downX
                val deltaY = event.y - downY
                val swipeThreshold = 100f
                if (abs(deltaX) > swipeThreshold && abs(deltaX) > abs(deltaY)) {
                    parent.parent.parent.requestDisallowInterceptTouchEvent(true)
                } else if (abs(deltaY) > swipeThreshold && abs(deltaY) > abs(deltaX)) {
                    parent.parent.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
