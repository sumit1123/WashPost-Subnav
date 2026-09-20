package com.wapo.flagship.external

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.wapo.flagship.external.storage.AppWidget
import com.wapo.flagship.external.storage.WidgetType
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.ReachabilityUtil
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.models.config.WidgetSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseWidgetConfigurationActivity : AppCompatActivity() {
    protected open val tag = "BaseWidgetConfiguration"
    protected var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val widgetSections: List<WidgetSection> = WidgetData.getSectionsList()
    private val displayWidgetSections: List<String> = widgetSections.map { it.displayName }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
            ?: appWidgetId
        setResult(RESULT_CANCELED, intent)
        Logger.d(tag, "appWidgetId=$appWidgetId")

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        if (OneTrustHelper.shouldShowBanner()) {
            Toast
                .makeText(
                    this,
                    getString(R.string.widget_gdpr_error),
                    Toast.LENGTH_SHORT,
                ).show()
            finish()
        } else if (!ReachabilityUtil.isConnected(applicationContext)) {
            Toast
                .makeText(
                    this,
                    getString(R.string.widget_network_connectivity_error),
                    Toast.LENGTH_SHORT,
                ).show()
            finish()
        }

        showDialog(this, widgetSections, displayWidgetSections)
    }

    protected open fun showDialog(
        context: Context,
        sections: List<WidgetSection>,
        displaySections: List<String>,
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(R.drawable.ic_wp32)
        builder.setTitle(getString(R.string.configure_widget_title))
        builder.setItems(displaySections.toTypedArray()) { _, which ->
            AppWidgetCoroutineScope.launch {
                val bundleName = sections[which].bundleName
                val sectionName = sections[which].displayName
                withContext(Dispatchers.IO) {
                    WidgetDBStorage
                        .getInstance(applicationContext)
                        .insert(
                            AppWidget(
                                appWidgetId.toString(),
                                sectionName,
                                bundleName,
                                getWidgetType(),
                            ),
                        )
                }

                onItemSelected(bundleName)

                setResult(
                    Activity.RESULT_OK,
                    Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) },
                )
                finish()
            }
        }
        builder.setOnCancelListener { finish() }
        builder.show()
    }

    /*
     * Override onItemSelected method in the child class and
     * call <Widget>.updateAppWidget method to update the Widget first time.
     */
    abstract fun onItemSelected(
        @NonNull bundleName: String,
    )

    abstract fun getWidgetType(): WidgetType
}
