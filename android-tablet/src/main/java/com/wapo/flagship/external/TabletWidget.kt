package com.wapo.flagship.external

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.external.storage.AppWidget
import com.wapo.flagship.external.storage.WidgetType
import com.wapo.flagship.features.articles2.activities.WIDGET_ORIGINATED
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class TabletWidget : AppWidgetProvider() {
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        Logger.d(TAG, "Widget TabletWidget - onDeleted")
        AppWidgetCoroutineScope.launch(Dispatchers.IO) {
            appWidgetIds.forEach {
                getStorage(context).deleteById(it)
            }
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        Logger.d(TAG, "Widget TabletWidget - onReceive - action=${intent?.action}")
        context?.let { context ->
            intent?.let { intent ->
                val appWidgetId =
                    intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                    )
                if (intent.action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED ||
                    intent.action == REFRESH_ACTION
                ) {
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                        appWidgetId,
                        R.id.list_view,
                    )
                }
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Logger.d(TAG, "Widget TabletWidget - onUpdate")
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private const val TAG = "TabletWidget"
        const val REFRESH_ACTION = "com.wapo.flagship.external.tabletwidget.REFRESH"
        const val SETTINGS_ACTION = "com.wapo.flagship.external.tabletwidget.SETTINGS"

        @JvmStatic
        private fun getStorage(context: Context): WidgetStorage = WidgetDBStorage.getInstance(context)

        @JvmStatic
        fun notifyAppWidgetViewDataChanged(context: Context) {
            val appWidgets = WidgetDBStorage.getInstance(context).getAll()
            appWidgets.forEach {
                if (it.widgetType == WidgetType.TABLET_WIDGET) {
                    Logger.d(
                        TAG,
                        "Widget TabletWidget - notifyAppWidgetViewDataChanged - appWidgetId=${it.appWidgetId}",
                    )
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                        it.appWidgetId.toInt(),
                        R.id.list_view,
                    )
                }
            }
        }

        @JvmStatic
        @WorkerThread
        fun storeIdToStorageToNotify(
            context: Context,
            appWidgetId: Int,
        ) {
            Logger.d(
                TAG,
                "Widget TabletWidget - storeIdToStorageToNotify - appWidgetId=$appWidgetId",
            )
            WidgetDBStorage.getInstance(context).insert(
                AppWidget(appWidgetId.toString(), "", "", WidgetType.TABLET_WIDGET),
            )
        }

        @JvmStatic
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager?,
            appWidgetId: Int,
        ) {
            AppWidgetCoroutineScope.launch {
                appWidgetManager?.updateAppWidget(
                    appWidgetId,
                    getUpdatedView(context, getStorage(context), appWidgetId),
                )
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.list_view,
                )
            }
        }

        private suspend fun getUpdatedView(
            context: Context,
            storage: WidgetStorage,
            widgetId: Int,
        ): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_layout).apply {
                // logo
                setImageViewBitmap(
                    R.id.widget_logo,
                    UIUtil.vectorToBitmap(context, R.drawable.ic_wp32_dark),
                )
                val mainIntent =
                    IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                        putExtra(WIDGET_ORIGINATED, true)
                        putExtra(WidgetData.EXTRAS_WIDGET_TYPE, WidgetType.TABLET_WIDGET.name)
                    }
                setOnClickPendingIntent(
                    R.id.widget_logo,
                    PendingIntent.getActivity(
                        context,
                        Random.nextInt(),
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    ),
                )

                // title
                var appWidget: AppWidget?
                withContext(Dispatchers.IO) {
                    appWidget = storage.getById(widgetId)
                    appWidget ?: storeIdToStorageToNotify(context, widgetId)
                }
                appWidget?.let {
                    if (it.sectionName.isNotEmpty()) {
                        setTextViewText(R.id.widget_title, it.sectionName)
                        val sectionIntent =
                            IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                                data =
                                    Uri.parse(
                                        context.getString(R.string.wapo_domain_url) + it.bundleName,
                                    )
                                putExtra(WIDGET_ORIGINATED, true)
                                putExtra(TopBarFragment.SectionDisplayName, it.sectionName)
                                putExtra(TopBarFragment.EXTRAS_BUNDLE_PATH, it.bundleName)
                                putExtra(TopBarFragment.EXTRAS_DEEPLINK_TO_SECTION, true)
                                putExtra(WidgetData.EXTRAS_WIDGET_TYPE, WidgetType.TABLET_WIDGET.name)
                            }
                        setOnClickPendingIntent(
                            R.id.widget_title,
                            PendingIntent.getActivity(
                                context,
                                Random.nextInt(),
                                sectionIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                            ),
                        )
                    }
                    Logger.d(
                        TAG,
                        "Widget TabletWidget - getUpdatedView - widgetId=$widgetId, sectionName=${it.sectionName}",
                    )
                }

                // refresh
                setImageViewBitmap(
                    R.id.widget_refresh,
                    UIUtil.vectorToBitmap(context, R.drawable.ic_refresh_24px),
                )
                val refreshIntent =
                    Intent(context, TabletWidget::class.java).apply {
                        action = REFRESH_ACTION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                setOnClickPendingIntent(
                    R.id.widget_refresh,
                    PendingIntent.getBroadcast(
                        context,
                        Random.nextInt(),
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    ),
                )

                // settings
                setImageViewBitmap(
                    R.id.widget_settings,
                    UIUtil.vectorToBitmap(context, R.drawable.baseline_settings_24),
                )
                val settingsIntent =
                    Intent(context, ListWidgetConfigurationActivity::class.java).apply {
                        action = SETTINGS_ACTION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                setOnClickPendingIntent(
                    R.id.widget_settings,
                    PendingIntent.getActivity(
                        context,
                        Random.nextInt(),
                        settingsIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    ),
                )

                // adapter
                val intent =
                    Intent(context, ListWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                    }
                setRemoteAdapter(R.id.list_view, intent)
                setViewVisibility(R.id.list_view, View.VISIBLE)

                // Collections fill-in
                val clickIntent =
                    IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                        putExtra(WidgetData.EXTRAS_WIDGET_TYPE, WidgetType.TABLET_WIDGET.name)
                    }
                val clickPI =
                    PendingIntent.getActivity(
                        context,
                        0,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                setPendingIntentTemplate(R.id.list_view, clickPI)
            }
    }
}
