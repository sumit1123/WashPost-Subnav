package com.wapo.flagship.features.articles3.views

import android.content.Context
import android.content.res.Configuration
import android.util.Base64
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles3.models.ui.WebEmbedUiModel
import com.wapo.view.web_embeds.EmbedJSInterface
import com.wapo.view.web_embeds.addEmbedJsInterface
import com.washingtonpost.android.R

// ============================================================================
// STYLE ENUM
// ============================================================================

enum class WebEmbedUiStyle {
    DEFAULT
}

// ============================================================================
// WEB VIEW POOL - Manages WebView instances across article lifetime
// ============================================================================

/**
 * Manages WebView instances for embeds across the article screen lifetime.
 *
 * WebViews are created once and kept alive even when scrolled off-screen,
 * preventing reload flicker and scroll jumping issues.
 *
 * Usage:
 * ```
 * ProvideWebViewPool {
 *     ArticleContentView(...)
 * }
 * ```
 */
class WebViewPool {

    // -- Storage --
    private val pool = mutableMapOf<String, WebView>()
    private val loadedEmbeds = mutableSetOf<String>()
    private val subtypes = mutableMapOf<String, String?>()
    private val heightCallbacks = mutableMapOf<String, (Int) -> Unit>()

    /** Height cache in PIXELS — survives uiModel recreation across recompositions */
    val heightCachePx = mutableMapOf<String, Int>()

    // -- WebView Lifecycle --

    fun getOrCreate(key: String, context: Context, onCreate: WebView.() -> Unit): WebView {
        return pool.getOrPut(key) {
            WebView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                onCreate()
            }
        }
    }

    fun get(key: String): WebView? = pool[key]

    fun destroyAll() {
        pool.values.forEach { it.destroy() }
        pool.clear()
        heightCachePx.clear()
        loadedEmbeds.clear()
        subtypes.clear()
        heightCallbacks.clear()
    }

    // -- Load State Tracking --

    fun isLoaded(key: String): Boolean = key in loadedEmbeds

    fun markLoaded(key: String, subtype: String?) {
        loadedEmbeds.add(key)
        subtypes[key] = subtype
    }

    // -- Height Management --

    fun setHeightCallback(key: String, callback: (Int) -> Unit) {
        heightCallbacks[key] = callback
    }

    fun removeHeightCallback(key: String) {
        heightCallbacks.remove(key)
    }

    /**
     * Updates the cached height for an embed.
     *
     * For datawrapper embeds, height can grow or shrink (supports pagination).
     * For all other embeds, height only grows to protect against transient small values.
     */
    fun updateHeight(key: String, heightPx: Int, subtype: String?) {
        if (heightPx <= 0) return

        val currentHeight = heightCachePx[key] ?: 0
        val shouldUpdate = when (subtype) {
            "datawrapper" -> heightPx != currentHeight  // Can grow or shrink
            else -> heightPx > currentHeight             // Only grow
        }

        if (shouldUpdate) {
            heightCachePx[key] = heightPx
            heightCallbacks[key]?.invoke(heightPx)
        }
    }

    /**
     * Forces height re-measurement for an embed.
     * Call this when the embed becomes visible to ensure correct height.
     */
    fun forceHeightMeasurement(key: String, context: Context) {
        val webView = pool[key] ?: return
        val subtype = subtypes[key]

        // Re-inject JS height reporter for fresh measurement
        webView.post {
            injectHeightReporter(webView, subtype)
        }

        // Also trigger native layout measurement
        webView.post {
            val contentHeightPx = webView.contentHeight
            if (contentHeightPx > 0) {
                val density = context.resources.displayMetrics.density
                val heightInPx = (contentHeightPx * density).toInt()
                updateHeight(key, heightInPx, subtype)
            }
        }
    }
}

// -- CompositionLocal for WebViewPool --

val LocalWebViewPool = staticCompositionLocalOf<WebViewPool> {
    error("No WebViewPool provided — wrap your article screen with ProvideWebViewPool { ... }")
}

@Composable
fun ProvideWebViewPool(content: @Composable () -> Unit) {
    val pool = remember { WebViewPool() }
    DisposableEffect(Unit) {
        onDispose { pool.destroyAll() }
    }
    CompositionLocalProvider(LocalWebViewPool provides pool) {
        content()
    }
}

// ============================================================================
// MAIN COMPOSABLE
// ============================================================================

/**
 * [fixedHeight] turns the embed into a fixed viewport: the WebView is given exactly that height and
 * the page scrolls inside it, horizontally as well as vertically, instead of being stretched to its
 * own content height. Callers that pass it own the height (the sub-nav panel takes it from the
 * element's `item.sizes`); without it the embed keeps growing to fit its content.
 */
@Composable
fun WebEmbedView(
    index: Int,
    uiModel: WebEmbedUiModel,
    onArticleInteractionEvent: (ArticleInteractionEvent) -> Unit,
    webEmbedSettings: WebEmbedSettings,
    fixedHeight: Dp? = null,
) {
    val context = LocalContext.current
    val pool = LocalWebViewPool.current
    val density = LocalDensity.current

    val scrollsInternally = fixedHeight != null

    val embedKey = rememberEmbedKey(index, uiModel)
    val (embedHeightPx, setEmbedHeightPx) = rememberHeightState(embedKey, pool)

    // Convert pixels to dp for layout
    val measuredHeight: Dp? = remember(embedHeightPx, density) {
        if (embedHeightPx > 0) with(density) { embedHeightPx.toDp() } else null
    }

    // Register height callback -- only the growing embeds have a height to report.
    if (!scrollsInternally) {
        RegisterHeightCallback(embedKey, pool, setEmbedHeightPx)
    }

    // Create or retrieve WebView
    val webView = rememberWebView(embedKey, uiModel, context, pool, webEmbedSettings, scrollsInternally)

    // Handle loading and height polling
    EmbedLoadEffect(embedKey, webView, uiModel, context, pool, measureHeight = !scrollsInternally)

    // Render the embed
    EmbedContainer(
        embedHeight = fixedHeight ?: measuredHeight,
        webView = webView,
        uiModel = uiModel,
        scrollsInternally = scrollsInternally,
        onArticleInteractionEvent = onArticleInteractionEvent
    )
}

// ============================================================================
// COMPOSABLE HELPERS
// ============================================================================

@Composable
private fun rememberEmbedKey(index: Int, uiModel: WebEmbedUiModel): String {
    return remember(uiModel) {
        val url = uiModel.url.orEmpty()
        val oembedHash = uiModel.oembed?.hashCode() ?: 0
        val subtype = uiModel.subtype.orEmpty()
        "embed_${index}_${url}_${oembedHash}_${subtype}"
    }
}

@Composable
private fun rememberHeightState(embedKey: String, pool: WebViewPool): Pair<Int, (Int) -> Unit> {
    var embedHeightPx by remember(embedKey) {
        mutableStateOf(pool.heightCachePx[embedKey] ?: 0)
    }
    return embedHeightPx to { newHeight: Int -> embedHeightPx = newHeight }
}

@Composable
private fun RegisterHeightCallback(
    embedKey: String,
    pool: WebViewPool,
    setHeight: (Int) -> Unit
) {
    DisposableEffect(embedKey) {
        pool.setHeightCallback(embedKey) { newHeightPx -> setHeight(newHeightPx) }
        onDispose { pool.removeHeightCallback(embedKey) }
    }
}

@Composable
private fun rememberWebView(
    embedKey: String,
    uiModel: WebEmbedUiModel,
    context: Context,
    pool: WebViewPool,
    webEmbedSettings: WebEmbedSettings,
    scrollsInternally: Boolean,
): WebView {
    return remember(embedKey) {
        pool.getOrCreate(embedKey, context) {
            configureWebView(this, uiModel, embedKey, context, pool, webEmbedSettings, scrollsInternally)
        }
    }
}

@Composable
private fun EmbedLoadEffect(
    embedKey: String,
    webView: WebView,
    uiModel: WebEmbedUiModel,
    context: Context,
    pool: WebViewPool,
    measureHeight: Boolean
) {
    LaunchedEffect(embedKey) {
        // Load content if not already loaded
        if (!pool.isLoaded(embedKey)) {
            loadEmbed(webView, uiModel, context)
            pool.markLoaded(embedKey, uiModel.subtype)
        }

        // A caller-supplied height is the layout, so there is nothing to measure or poll for.
        if (!measureHeight) return@LaunchedEffect

        // Initial measurement after view attaches
        kotlinx.coroutines.delay(100)
        pool.forceHeightMeasurement(embedKey, context)

        // Poll for height changes (async content like Twitter/TikTok)
        POLL_INTERVALS.forEach { interval ->
            kotlinx.coroutines.delay(interval)
            pool.forceHeightMeasurement(embedKey, context)
        }
    }
}

@Composable
private fun EmbedContainer(
    embedHeight: Dp?,
    webView: WebView,
    uiModel: WebEmbedUiModel,
    scrollsInternally: Boolean,
    onArticleInteractionEvent: (ArticleInteractionEvent) -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(embedHeight ?: dimensionResource(id = R.dimen.embed_placeholder_height))

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> createEmbedContainer(ctx, webView, uiModel, scrollsInternally) },
            update = { container -> updateEmbedContainer(container, webView, uiModel, scrollsInternally) }
        )

        // Reddit overlay for tap interception
        if (uiModel.subtype == "reddit") {
            RedditTapOverlay(uiModel, onArticleInteractionEvent)
        }
    }
}

@Composable
private fun RedditTapOverlay(
    uiModel: WebEmbedUiModel,
    onArticleInteractionEvent: (ArticleInteractionEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                uiModel.url?.let { url ->
                    onArticleInteractionEvent(ArticleInteractionEvent.LinkClickEvent(url))
                }
            }
    )
}

// ============================================================================
// WEBVIEW CONFIGURATION
// ============================================================================

private val POLL_INTERVALS = listOf(300L, 500L, 1000L, 2000L, 3000L, 5000L)

private fun configureWebView(
    webView: WebView,
    uiModel: WebEmbedUiModel,
    embedKey: String,
    context: Context,
    pool: WebViewPool,
    webEmbedSettings: WebEmbedSettings,
    scrollsInternally: Boolean,
) {
    val subtype = uiModel.subtype

    // Height tracking via layout changes -- a fixed viewport has no height to report.
    if (!scrollsInternally) {
        webView.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val wv = v as WebView
            val contentHeightPx = wv.contentHeight
            if (contentHeightPx > 0) {
                val density = context.resources.displayMetrics.density
                val heightInPx = (contentHeightPx * density).toInt()
                pool.updateHeight(embedKey, heightInPx, subtype)
            }
        }
    }

    // Subtype-specific settings
    when {
        // The WebView fills its fixed box and scrolls its own content in both directions.
        // loadWithOverviewMode stays off on purpose: shrinking the page to fit would leave
        // nothing to scroll horizontally, which is the point of a wide component here.
        scrollsInternally -> {
            webView.isScrollContainer = true
            webView.isVerticalScrollBarEnabled = true
            webView.isHorizontalScrollBarEnabled = true
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = false
        }
        subtype == "datawrapper" -> {
            webView.isHorizontalScrollBarEnabled = true
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        }
        else -> webView.isScrollContainer = false
    }

    // JavaScript interfaces
    webView.addJavascriptInterface(LinkHandlerInterface(context), "LinkHandler")
    webView.addJavascriptInterface(HeightReporterInterface(pool, embedKey, subtype), "HeightReporter")
    webView.addEmbedJsInterface(webEmbedSettings.embedJSInterface)

    // WebView client
    webView.webViewClient = object : EmbedWebViewClient(subtype) {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.post {
                injectLinkInterceptor(view)
                if (!scrollsInternally) injectHeightReporter(view, subtype)
            }
        }
    }

    // Load content
    loadEmbed(webView, uiModel, context)
    pool.markLoaded(embedKey, subtype)
}

// ============================================================================
// JAVASCRIPT INTERFACES
// ============================================================================

private class LinkHandlerInterface(private val context: Context) {
    @android.webkit.JavascriptInterface
    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            )
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}

private class HeightReporterInterface(
    private val pool: WebViewPool,
    private val embedKey: String,
    private val subtype: String?
) {
    @android.webkit.JavascriptInterface
    fun reportHeight(px: Int) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            pool.updateHeight(embedKey, px, subtype)
        }
    }
}

// ============================================================================
// CONTAINER CREATION & UPDATE
// ============================================================================

private fun createEmbedContainer(
    ctx: Context,
    webView: WebView,
    uiModel: WebEmbedUiModel,
    scrollsInternally: Boolean
): FrameLayout {
    val container = when {
        scrollsInternally -> ScrollingEmbedContainer(ctx)
        uiModel.subtype == "datawrapper" -> DatawrapperTouchContainer(ctx)
        else -> FrameLayout(ctx)
    }

    (webView.parent as? FrameLayout)?.removeView(webView)
    container.addView(webView, embedLayoutParams(scrollsInternally))
    return container
}

private fun updateEmbedContainer(
    container: FrameLayout,
    webView: WebView,
    uiModel: WebEmbedUiModel,
    scrollsInternally: Boolean
) {
    if (webView.parent !== container) {
        (webView.parent as? FrameLayout)?.removeView(webView)
        container.addView(webView, embedLayoutParams(scrollsInternally))
    }

    if (scrollsInternally) return

    // Force height measurement when view becomes visible
    webView.post {
        webView.requestLayout()
        injectHeightReporter(webView, uiModel.subtype)
    }
}

/**
 * A fixed-viewport embed matches its box so the page scrolls inside it; every other embed wraps
 * its content, which is what the measured height then sizes.
 */
private fun embedLayoutParams(scrollsInternally: Boolean) = FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    if (scrollsInternally) {
        FrameLayout.LayoutParams.MATCH_PARENT
    } else {
        FrameLayout.LayoutParams.WRAP_CONTENT
    }
)

/**
 * Custom FrameLayout for Datawrapper that handles horizontal scroll gestures
 * while allowing vertical scrolling of the parent LazyColumn.
 */
private class DatawrapperTouchContainer(context: Context) : FrameLayout(context) {
    private var startX = 0f
    private var startY = 0f
    private var directionLocked = false

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                directionLocked = false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (!directionLocked) {
                    val dx = kotlin.math.abs(ev.x - startX)
                    val dy = kotlin.math.abs(ev.y - startY)
                    if (dx > 8 || dy > 8) {
                        directionLocked = true
                        parent?.requestDisallowInterceptTouchEvent(dx > dy)
                    }
                }
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                directionLocked = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return false
    }
}

/**
 * Holds an embed that scrolls inside a fixed box. The article is a LazyColumn, so a drag that
 * starts here would otherwise scroll the article past the panel instead of the embed. The gesture
 * is handed to the WebView only while it can still scroll the way the finger is moving -- once the
 * embed is at its edge the article takes over, so the panel never becomes a dead zone.
 */
private class ScrollingEmbedContainer(context: Context) : FrameLayout(context) {
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var directionLocked = false

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                directionLocked = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val embed = getChildAt(0)
                if (!directionLocked && embed != null) {
                    val dx = ev.x - startX
                    val dy = ev.y - startY
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                        directionLocked = true
                        // canScroll* takes the direction the content moves, which is the
                        // opposite of the finger: dragging down (dy > 0) scrolls up.
                        val consumes = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            embed.canScrollHorizontally(if (dx > 0) -1 else 1)
                        } else {
                            embed.canScrollVertically(if (dy > 0) -1 else 1)
                        }
                        parent?.requestDisallowInterceptTouchEvent(consumes)
                    }
                }
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                directionLocked = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return false
    }
}

// ============================================================================
// EMBED LOADING
// ============================================================================

private fun loadEmbed(webView: WebView, uiModel: WebEmbedUiModel, context: Context) {
    val data = uiModel.oembed

    when {
        data != null -> loadOembedContent(webView, uiModel.subtype, data, context)
        uiModel.subtype == "datawrapper" || uiModel.subtype == "partial" -> uiModel.url?.let { webView.loadUrl(it) }
    }
}

private fun loadOembedContent(
    webView: WebView,
    subtype: String?,
    data: String,
    context: Context
) {
    when (subtype) {
        "twitter" -> {
            val html = TWITTER_WRAPPER
                .replace("{{{DATA}}}", data)
                .replace("{{{THEME}}}", getThemeForTwitter(context))
                .replace("{{{BG_COLOR}}}", getBackgroundColorForTwitter(context))
            webView.loadBase64Html(html)
        }
        "instagram", "facebook", "tiktok", "reddit" -> {
            val baseUrl = "https://www.$subtype.com"
            webView.loadDataWithBaseURL(
                baseUrl,
                buildBaseHtmlWrapper(data),
                "text/html",
                "UTF-8",
                null
            )
        }
        "ai2html" -> {
            val html = buildBaseHtmlWrapper(data)
            webView.loadBase64Html(html)
        }
    }
}

private fun WebView.loadBase64Html(html: String) {
    val encoded = Base64.encodeToString(html.toByteArray(), Base64.NO_PADDING)
    loadData(encoded, "text/html", "base64")
}

private fun buildBaseHtmlWrapper(data: String): String = """
    <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            $WAPO_FONTS_CSS
        </head>
        <body style="margin:0; padding:0;">
            <div id="wapo-app-wrapper" align="center">
                $data
            </div>
        </body>
    </html>
""".trimIndent()

// ============================================================================
// JAVASCRIPT INJECTION
// ============================================================================

/**
 * Injects JavaScript to observe and report content height changes.
 *
 * Uses ResizeObserver and MutationObserver to detect when embed content
 * finishes loading (especially for async content like Twitter/TikTok iframes).
 *
 * Safe to call multiple times — observers are only set up once.
 */
internal fun injectHeightReporter(webView: WebView, subtype: String?) {
    webView.evaluateJavascript(HEIGHT_REPORTER_JS, null)
}

/**
 * Injects click and postMessage listeners to intercept link taps.
 *
 * Needed because shouldOverrideUrlLoading doesn't fire inside cross-origin iframes.
 */
private fun injectLinkInterceptor(webView: WebView) {
    webView.evaluateJavascript(LINK_INTERCEPTOR_JS, null)
}

// ============================================================================
// WEBVIEW CLIENT
// ============================================================================

internal open class EmbedWebViewClient(
    private val subtype: String?
) : android.webkit.WebViewClient() {

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest?
    ): Boolean {
        // Datawrapper handles its own navigation
        if (subtype == "datawrapper") {
            return super.shouldOverrideUrlLoading(view, request)
        }

        // Open all other links externally
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, request?.url)
            view.context.startActivity(intent)
        } catch (_: Exception) { }
        return true
    }
}

data class WebEmbedSettings(
    val embedJSInterface: EmbedJSInterface,
)

// ============================================================================
// THEME HELPERS
// ============================================================================

private fun getThemeForTwitter(context: Context): String {
    val nightMode = context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
    return if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
}

private fun getBackgroundColorForTwitter(context: Context): String {
    val color = ContextCompat.getColor(context, R.color.app_bg)
    return String.format("#%06x", color and 0xffffff)
}

// ============================================================================
// HTML & JAVASCRIPT CONSTANTS
// ============================================================================

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

private const val WAPO_FONTS_CSS = """
<style>
    @font-face {
        font-family: PostoniDisplayMag;
        font-weight: 800;
        font-display: fallback;
        src: url(https://www.washingtonpost.com/wp-stat/assets/fonts/PostoniDisplayMag-Ultra_Italic.woff2);
    }
    @font-face {
        font-family: PostoniDisplayMag;
        font-weight: 800;
        font-display: fallback;
        src: url(https://www.washingtonpost.com/wp-stat/assets/fonts/PostoniDisplayMag-Ultra.woff2);
    }
    @font-face {
        font-family: Postoni;
        font-weight: 700;
        font-display: fallback;
        src: url(https://www.washingtonpost.com/wp-stat/assets/fonts/PostoniWide-Bold.woff2);
    }
    @font-face {
        font-family: Postoni;
        font-weight: 300;
        font-display: fallback;
        src: url(https://www.washingtonpost.com/wp-stat/assets/fonts/PostoniWide-Regular.woff2);
    }
    @font-face {
        font-family: Franklin;
        font-weight: 700;
        font-display: fallback;
        src: url(https://www.washingtonpost.com/wp-stat/assets/fonts/ITC_Franklin-Bold.woff2);
    }
    @font-face {
        font-family: Franklin;
        font-weight: 300;
        font-display: fallback;
        src: url(https://www.washingtonpost.com/wp-stat/assets/fonts/ITC_Franklin-Light.woff2);
    }
</style>
"""

// language=JavaScript
private const val HEIGHT_REPORTER_JS = """
(function() {
    // Only set up observers once
    if (!window.__heightReporterInitialized) {
        window.__heightReporterInitialized = true;
        window.__lastReportedHeight = 0;
        
        // ResizeObserver for body
        if (typeof ResizeObserver !== 'undefined') {
            new ResizeObserver(function() {
                window.__reportHeightNow && window.__reportHeightNow();
            }).observe(document.body);
        }
        
        // Observe iframes (TikTok, Twitter, etc.)
        function observeIframe(iframe) {
            if (typeof ResizeObserver !== 'undefined') {
                new ResizeObserver(function() {
                    window.__reportHeightNow && window.__reportHeightNow();
                }).observe(iframe);
            }
            iframe.addEventListener('load', function() {
                setTimeout(function() {
                    window.__reportHeightNow && window.__reportHeightNow();
                }, 100);
            });
        }
        
        document.querySelectorAll('iframe').forEach(observeIframe);
        
        // Watch for dynamically added elements
        if (typeof MutationObserver !== 'undefined') {
            new MutationObserver(function(mutations) {
                var shouldReport = false;
                mutations.forEach(function(m) {
                    if (m.addedNodes.length > 0) shouldReport = true;
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeName === 'IFRAME') observeIframe(node);
                        else if (node.querySelectorAll) {
                            node.querySelectorAll('iframe').forEach(observeIframe);
                        }
                    });
                });
                if (shouldReport) {
                    setTimeout(function() {
                        window.__reportHeightNow && window.__reportHeightNow();
                    }, 50);
                }
            }).observe(document.body, { childList: true, subtree: true, attributes: true });
        }
    }
    
    // Calculate content height
    function getContentHeight() {
        var body = document.body;
        var html = document.documentElement;
        var maxHeight = Math.max(
            body ? body.scrollHeight : 0,
            body ? body.offsetHeight : 0,
            html ? html.clientHeight : 0,
            html ? html.scrollHeight : 0,
            html ? html.offsetHeight : 0
        );
        
        // Check iframes
        document.querySelectorAll('iframe').forEach(function(iframe) {
            var rect = iframe.getBoundingClientRect();
            if (rect.height > 0) {
                var iframeBottom = iframe.offsetTop + rect.height;
                if (iframeBottom > maxHeight) maxHeight = iframeBottom;
            }
        });
        
        // Check Twitter widgets
        document.querySelectorAll('twitter-widget, .twitter-tweet-rendered').forEach(function(widget) {
            var rect = widget.getBoundingClientRect();
            if (rect.height > 0) {
                var widgetBottom = widget.offsetTop + rect.height;
                if (widgetBottom > maxHeight) maxHeight = widgetBottom;
            }
        });
        
        return maxHeight;
    }
    
    // Report height if changed
    window.__reportHeightNow = function() {
        var h = Math.round(getContentHeight());
        if (h > 0 && h !== window.__lastReportedHeight) {
            window.__lastReportedHeight = h;
            HeightReporter.reportHeight(h);
        }
    };
    
    // Report immediately and after short delays
    window.__reportHeightNow();
    [50, 150, 300, 600].forEach(function(ms) {
        setTimeout(window.__reportHeightNow, ms);
    });
})();
"""

// language=JavaScript
private const val LINK_INTERCEPTOR_JS = """
(function() {
    // Intercept clicks on anchor tags
    document.addEventListener('click', function(e) {
        var a = e.target.closest ? e.target.closest('a') : null;
        if (a && a.href) {
            e.preventDefault();
            e.stopPropagation();
            LinkHandler.openUrl(a.href);
        }
    }, true);

    // Listen for postMessage events containing URLs
    window.addEventListener('message', function(e) {
        var d = e.data;
        if (!d) return;
        
        if (typeof d === 'string') {
            if (d.startsWith('http://') || d.startsWith('https://')) {
                LinkHandler.openUrl(d);
            }
        } else if (typeof d === 'object') {
            var url = (d.data && d.data.url) || d.url || '';
            if (url.startsWith('http://') || url.startsWith('https://')) {
                LinkHandler.openUrl(url);
            }
        }
    });
})();
"""
