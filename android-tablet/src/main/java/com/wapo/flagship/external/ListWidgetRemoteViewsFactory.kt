package com.wapo.flagship.external

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.di.app.modules.features.articles2.WidgetDependencyEntryPoint
import com.wapo.flagship.external.storage.AppWidget
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import rx.Observable

class ListWidgetRemoteViewsFactory(
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
            "Widget ListRemoteViewsFactory - onDataSetChanged - appWidgetId=$appWidgetId, $this",
        )
        WidgetDBStorage.getInstance(context).getById(appWidgetId)?.run {
            refreshData(this)
        }
    }

    override fun hasStableIds(): Boolean = false

    override fun getViewAt(position: Int): RemoteViews? {
        Logger.d(TAG, "Widget ListRemoteViewsFactory - getViewAt$position, viewType=$viewType")
        if (viewType == ViewType.GDPR) {
            return if (position == 0) {
                RemoteViews(
                    context.packageName,
                    R.layout.widget_tablet_main_gdpr,
                ).apply {
                    setOnClickFillInIntent(R.id.appwidget_gdpr_text, Intent())
                }
            } else {
                null
            }
        } else {
            widgetItems?.let {
                if (it.size == 0) {
                    CrashWrapper.sendException(
                        IllegalStateException("List is empty! position=$position"),
                    )
                    return null
                }

                return RemoteViews(context.packageName, R.layout.widget_tablet_main).apply {
                    val article = it[position]
                    val articleUrl = article.contentUrl
                    val sectionPosition = widgetItemsUrlsArray?.indexOf(articleUrl)

                    // Set label
                    if (!TextUtils.isEmpty(article.label)) {
                        setTextViewText(R.id.widget_item_label, Html.fromHtml(article.label))
                        setViewVisibility(R.id.widget_item_label, View.VISIBLE)
                    } else {
                        setViewVisibility(R.id.widget_item_label, View.GONE)
                    }
                    // Set headline
                    setTextViewText(
                        R.id.widget_item_headline,
                        Html.fromHtml(
                            if (!TextUtils.isEmpty(article.headline)) article.headline else "",
                        ),
                    )
                    // Set image
                    if (!article.mediaUrl.isNullOrEmpty()) {
                        val data =
                            FlagshipApplication
                                .getInstance()
                                .cacheManager
                                .get(
                                    article.mediaUrl,
                                )?.data
                        val bitmap =
                            BitmapUtils.parseBitmap(
                                data,
                                true,
                                THUMBNAIL_WIDTH,
                                THUMBNAIL_HEIGHT,
                            )
                        if (bitmap is Bitmap) {
                            setImageViewBitmap(R.id.widget_item_iv, bitmap)
                            setViewVisibility(R.id.widget_item_iv, View.VISIBLE)
                        } else {
                            setViewVisibility(R.id.widget_item_iv, View.GONE)
                        }
                    } else {
                        setViewVisibility(R.id.widget_item_iv, View.GONE)
                    }

                    val widgetId = java.util.UUID.randomUUID().toString()

                    val articlesParcel = ArticlesParcel.builder()
                        .setArticleSingleUrl(articleUrl)
                        .setWidgetId(widgetId)
                        .widgetOriginated(true)
                        .setCategoryName(it.categoryName)
                        .setCategoryPath(it.categoryPath)
                        .setSectionDisplayName(it.categoryName)
                        .setBackActivityParam(MainActivity::class.java.name)

                    setOnClickFillInIntent(R.id.widget_item, articlesParcel.buildIntent(context))
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
        Logger.d(TAG, "Widget ListRemoteViewsFactory - onDestroy")
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
                                            "Widget ListRemoteViewsFactory - refreshData - getWidgetArticles - ${it.categoryName}, ${it.categoryPath}, $bundleName",
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
                                            "Widget ListRemoteViewsFactory - refreshData - no content, skipping update - ${it.categoryName}, ${it.categoryPath}, $bundleName",
                                        )
                                        null
                                    }
                                }
                            }.onErrorResumeNext(Observable.just(null))
                            .toBlocking()
                            .firstOrDefault(null)
                    Logger.d(
                        TAG,
                        "Widget ListRemoteViewsFactory - refreshData - ${articleWrappers?.size}",
                    )
                    articleWrappers?.let { pageArticles ->
                        it.clear()
                        it.addAll(pageArticles)
                        widgetItemsUrlsArray = WidgetData.getArticlesUrls(it).toTypedArray()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "RemoteViewsFactory"
        private const val THUMBNAIL_WIDTH = 196
        private const val THUMBNAIL_HEIGHT = 196
    }
}
