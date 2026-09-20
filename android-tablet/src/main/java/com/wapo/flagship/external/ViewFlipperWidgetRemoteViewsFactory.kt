package com.wapo.flagship.external

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.TextUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.external.storage.AppWidget
import com.wapo.flagship.features.articles2.activities.ARTICLES_URL_PARAM
import com.wapo.flagship.features.articles2.activities.CURRENT_ARTICLE_ID_PARAM
import com.wapo.flagship.features.articles2.activities.SECTION_START_POS
import com.wapo.flagship.features.articles2.activities.WIDGET_ORIGINATED
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.di.app.modules.features.articles2.WidgetDependencyEntryPoint
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import com.wapo.flagship.util.UIUtil
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import rx.Observable
import java.lang.IllegalStateException

class ViewFlipperWidgetRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent,
) : RemoteViewsService.RemoteViewsFactory {
    enum class ViewType {
        DATA,
        GDPR,
    }

    @Volatile
    private var viewType: ViewType = ViewType.DATA
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetItems: WidgetData.Articles? = null
    private var widgetItemsUrlsArray: Array<String>? = null

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var articlesSaveRepo: ArticlesSaveRepo

    override fun onCreate() {
        val entryPoint = EntryPointAccessors.fromApplication(
            context = context,
            entryPoint = WidgetDependencyEntryPoint::class.java
        )
        articlesSaveRepo = entryPoint.getArticlesSaveRepo()
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onDataSetChanged() {
        appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        Logger.d(
            TAG,
            "Widget ViewFlipperRemoteViewsFactory - onDataSetChanged - appWidgetId=$appWidgetId, $this",
        )
        WidgetDBStorage.getInstance(context).getById(appWidgetId)?.run {
            refreshData(this)
        }
    }

    override fun hasStableIds(): Boolean = false

    override fun getViewAt(position: Int): RemoteViews? {
        Logger.d(
            TAG,
            "Widget ViewFlipperRemoteViewsFactory - getViewAt$position, viewType=$viewType",
        )
        if (viewType == ViewType.GDPR) {
            return if (position == 0) {
                RemoteViews(
                    context.packageName,
                    R.layout.widget_tablet_main_gdpr,
                ).apply {
                    Intent().apply {
                        action = Widget.OPEN_ACTION
                        setOnClickFillInIntent(R.id.appwidget_gdpr_text, this)
                    }
                }
            } else {
                null
            }
        } else {
            widgetItems?.let {
                if (it.size == 0) {
                    CrashWrapper.sendException(
                        IllegalStateException("FlipperList is empty! position=$position"),
                    )
                    return null
                }

                return RemoteViews(context.packageName, R.layout.widget_main_small_row_item).apply {
                    val article = it[position]
                    val articleUrl = article.contentUrl
                    val sectionPosition = widgetItemsUrlsArray?.indexOf(articleUrl)

                    // Set Title
                    setTextViewText(R.id.widget_small_header_section_text, it.categoryName)

                    // Set count
                    setTextViewText(
                        R.id.widget_small_header_article_xOfx_text,
                        "${position + 1} of ${it.size}",
                    )

                    // Set headline
                    setTextViewText(
                        R.id.widget_small_article_body_item_text,
                        Html.fromHtml(
                            if (!TextUtils.isEmpty(article.headline)) article.headline else "",
                        ),
                    )

                    // Bind the click intent for logo
                    setImageViewBitmap(
                        R.id.widget_small_header_logo,
                        UIUtil.vectorToBitmap(context, R.drawable.ic_wp24_dark),
                    )
                    Intent().apply {
                        action = Widget.OPEN_ACTION
                        putExtra(WIDGET_ORIGINATED, true)
                        setOnClickFillInIntent(R.id.widget_small_header_logo, this)
                    }

                    // Bind the click intent for title
                    Intent().apply {
                        action = Widget.OPEN_ACTION
                        data =
                            Uri.parse(
                                context.getString(R.string.wapo_domain_url) + it.categoryPath,
                            )
                        putExtra(WIDGET_ORIGINATED, true)
                        putExtra(TopBarFragment.SectionDisplayName, it.categoryName)
                        putExtra(TopBarFragment.EXTRAS_BUNDLE_PATH, it.categoryPath)
                        putExtra(TopBarFragment.EXTRAS_DEEPLINK_TO_SECTION, true)
                        setOnClickFillInIntent(R.id.widget_small_header_section_text, this)
                    }

                    // Bind the click intent for the refresh button
                    setImageViewBitmap(
                        R.id.widget_small_header_refresh,
                        UIUtil.vectorToBitmap(context, R.drawable.ic_refresh_black_18dp),
                    )
                    Intent().apply {
                        action = Widget.REFRESH_ACTION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setOnClickFillInIntent(R.id.widget_small_header_refresh, this)
                    }

                    // Bind the click intent for the previous button
                    setImageViewBitmap(
                        R.id.widget_small_navigator_up,
                        UIUtil.vectorToBitmap(context, R.drawable.ic_arrow_drop_up_32dp),
                    )
                    Intent().apply {
                        action = Widget.PREVIOUS_ACTION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setOnClickFillInIntent(R.id.widget_small_navigator_up, this)
                    }

                    // Bind the click intent for the next button
                    setImageViewBitmap(
                        R.id.widget_small_navigator_down,
                        UIUtil.vectorToBitmap(context, R.drawable.ic_arrow_drop_down_32dp),
                    )
                    Intent().apply {
                        action = Widget.NEXT_ACTION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setOnClickFillInIntent(R.id.widget_small_navigator_down, this)
                    }

                    val widgetId = java.util.UUID.randomUUID().toString()

                    // Bind the click intent for the text item
                    val articlesParcel = ArticlesParcel.builder()
                        .setWidgetId(widgetId)
                        .setArticleSingleUrl(articleUrl)
                        .widgetOriginated(true)
                        .setCategoryName(it.categoryName)
                        .setCategoryPath(it.categoryPath)
                        .setSectionDisplayName(it.categoryName)
                        .setBackActivityParam(MainActivity::class.java.name)

                    setOnClickFillInIntent(R.id.widget_small_article_body_item_text, articlesParcel.buildIntent(context))
                    viewScope.launch {
                        articlesSaveRepo.saveArticles(widgetId, widgetItemsUrlsArray?.toList())
                    }
                }
            } ?: return null
        }
    }

    override fun getCount(): Int {
        if (viewType == ViewType.GDPR) {
            return 1
        } else {
            return widgetItems?.size ?: 0
        }
    }

    override fun getViewTypeCount(): Int = 2

    override fun onDestroy() {
        Logger.d(TAG, "Widget ViewFlipperRemoteViewsFactory - onDestroy")
        widgetItems?.clear()
        widgetItemsUrlsArray = null
        viewScope.cancel()
    }

    private fun updateState(appWidget: AppWidget?) {
        viewType = if (OneTrustHelper.shouldShowBanner()) ViewType.GDPR else ViewType.DATA
        if (viewType == ViewType.GDPR) {
            widgetItems?.clear()
            widgetItemsUrlsArray = null
        }
        if (viewType == ViewType.DATA) {
            appWidget?.let {
                widgetItems = WidgetData.Articles(appWidget.sectionName, appWidget.bundleName)
            }
        }
    }

    private fun refreshData(appWidget: AppWidget?) {
        updateState(appWidget)
        if (viewType == ViewType.DATA) {
            widgetItems?.let {
                if (it.categoryPath.isNotEmpty()) {
                    val bundleName =
                        WidgetData.getFusionBundleName(it.categoryPath) ?: it.categoryPath
                    val articleWrappers =
                        FlagshipApplication
                            .getInstance()
                            .contentManager
                            .listenToPage(bundleName, true)
                            .filter { pageLayout -> pageLayout != null }
                            .map { pageLayout ->
                                when {
                                    pageLayout.fusionPage != null -> {
                                        Logger.d(
                                            TAG,
                                            "Widget ViewFlipperRemoteViewsFactory - refreshData - getWidgetArticles - ${it.categoryName}, ${it.categoryPath}, $bundleName",
                                        )
                                        getWidgetArticles(
                                            pageLayout.fusionPage,
                                            it.categoryName,
                                            bundleName,
                                            context,
                                        )
                                    }

                                    else -> {
                                        Logger.d(
                                            TAG,
                                            "Widget ViewFlipperRemoteViewsFactory - refreshData - no pb and fusion content, skipping update - ${it.categoryName}, ${it.categoryPath}",
                                        )
                                        null
                                    }
                                }
                            }.onErrorResumeNext(Observable.just(null))
                            .toBlocking()
                            .firstOrDefault(null)
                    Logger.d(
                        TAG,
                        "Widget ViewFlipperRemoteViewsFactory - refreshData - ${articleWrappers?.size}",
                    )
                    articleWrappers?.let { pageArticles ->
                        val subListSize =
                            if (pageArticles.size > MAX_VIEWS_COUNT) MAX_VIEWS_COUNT else pageArticles.size
                        val subList = if (subListSize > 0) pageArticles.subList(
                            0,
                            subListSize
                        ) else articleWrappers
                        it.clear()
                        it.addAll(subList)
                        widgetItemsUrlsArray = WidgetData.getArticlesUrls(it).toTypedArray()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "RemoteViewsFactory"
        private const val MAX_VIEWS_COUNT = 10
    }
}
