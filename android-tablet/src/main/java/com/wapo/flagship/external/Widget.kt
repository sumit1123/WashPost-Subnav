package com.wapo.flagship.external

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.external.storage.AppWidget
import com.wapo.flagship.external.storage.WidgetType
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Widget : AppWidgetProvider() {
    override fun onDeleted(
        context: Context?,
        appWidgetIds: IntArray?,
    ) {
        Logger.d(TAG, "Widget SmallWidget - onDeleted")
        AppWidgetCoroutineScope.launch(Dispatchers.IO) {
            context?.let { context ->
                appWidgetIds?.forEach {
                    getStorage(context).deleteById(it)
                }
            }
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        Logger.d(TAG, "Widget SmallWidget - onReceive - action=${intent?.action}")
        context?.let { context ->
            intent?.let { intent ->
                val appWidgetId =
                    intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                    )
                if (intent.action == PREVIOUS_ACTION || intent.action == NEXT_ACTION) {
                    getUpdatedView(context, getStorage(context), appWidgetId).apply {
                        if (intent.action == PREVIOUS_ACTION) {
                            showPrevious(R.id.widget_small_article_body_view_flipper)
                        } else {
                            showNext(R.id.widget_small_article_body_view_flipper)
                        }
                        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, this)
                    }
                } else if (intent.action == OPEN_ACTION) {
                    IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                        this.fillIn(intent, 0)
                        context.startActivity(this)
                    }
                } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED ||
                    intent.action == REFRESH_ACTION
                ) {
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                        appWidgetId,
                        R.id.widget_small_article_body_view_flipper,
                    )
                } else {
                    // NO-OP
                }
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?,
    ) {
        Logger.d(TAG, "Widget SmallWidget - onUpdate")
        context?.let { context ->
            appWidgetIds?.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private const val TAG = "Widget"
        const val REFRESH_ACTION = "com.wapo.flagship.external.widget.REFRESH"
        const val PREVIOUS_ACTION = "com.wapo.flagship.external.widget.PREVIOUS"
        const val NEXT_ACTION = "com.wapo.flagship.external.widget.NEXT"
        const val OPEN_ACTION = "com.wapo.flagship.external.widget.OPEN"

        @JvmStatic
        private fun getStorage(context: Context): WidgetStorage = WidgetDBStorage.getInstance(context)

        @JvmStatic
        @WorkerThread
        fun storeIdToStorageToNotify(
            context: Context,
            appWidgetId: Int,
        ) {
            Logger.d(
                TAG,
                "Widget SmallWidget - storeIdToStorageToNotify - appWidgetId=$appWidgetId",
            )
            WidgetDBStorage.getInstance(context).insert(
                AppWidget(appWidgetId.toString(), "", "", WidgetType.WIDGET),
            )
        }

        @JvmStatic
        @WorkerThread
        fun notifyAppWidgetViewDataChanged(context: Context?) {
            context?.let {
                val appWidgets = WidgetDBStorage.getInstance(context).getAll()
                appWidgets.forEach {
                    if (it.widgetType == WidgetType.WIDGET) {
                        Logger.d(
                            TAG,
                            "Widget SmallWidget - notifyAppWidgetViewDataChanged - appWidgetId=${it.appWidgetId}",
                        )
                        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                            it.appWidgetId.toInt(),
                            R.id.widget_small_article_body_view_flipper,
                        )
                    }
                }
            }
        }

        @JvmStatic
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager?,
            appWidgetId: Int,
        ) {
            appWidgetManager?.updateAppWidget(
                appWidgetId,
                getUpdatedView(context, getStorage(context), appWidgetId),
            )
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                appWidgetId,
                R.id.widget_small_article_body_view_flipper,
            )
        }

        @JvmStatic
        private fun getUpdatedView(
            context: Context,
            storage: WidgetStorage,
            appWidgetId: Int,
        ): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_main_small).apply {
                val intent =
                    Intent(context, ViewFlipperWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                    }
                setRemoteAdapter(R.id.widget_small_article_body_view_flipper, intent)
                setEmptyView(R.id.widget_small_article_body_view_flipper, R.id.empty_view)

                val broadcastIntent =
                    Intent(context, Widget::class.java).apply {
                        putExtra(WidgetData.EXTRAS_WIDGET_TYPE, WidgetType.WIDGET.name)
                    }
                val broadcastPendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        broadcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                setPendingIntentTemplate(
                    R.id.widget_small_article_body_view_flipper,
                    broadcastPendingIntent,
                )

                AppWidgetCoroutineScope.launch(Dispatchers.IO) {
                    storage.getById(appWidgetId) ?: storeIdToStorageToNotify(context, appWidgetId)
                }
            }
    }
}
