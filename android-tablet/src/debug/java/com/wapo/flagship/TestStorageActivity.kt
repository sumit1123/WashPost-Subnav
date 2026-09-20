package com.wapo.flagship

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.washingtonpost.android.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * TestActivity to look at files (cached) sizes
 */
@AndroidEntryPoint
class TestStorageActivity : AppCompatActivity() {
    val tag = "TestStorageActivity"
    private val viewModel: TestStorageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_storage)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.refresh_metrics -> {
                viewModel.refreshMetrics()
                Toast
                    .makeText(
                        applicationContext,
                        "displaying results in the logcat...",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
            R.id.open_data_settings -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        val resolvedActivityList: List<ResolveInfo> =
                            packageManager.queryIntentActivities(
                                this,
                                0,
                            )
                        if (resolvedActivityList.isNotEmpty()) {
                            val ri = resolvedActivityList[0]
                            this.component =
                                ComponentName(
                                    ri.activityInfo.packageName,
                                    ri.activityInfo.name,
                                )
                            startActivity(this)
                        } else {
                            Toast
                                .makeText(
                                    applicationContext,
                                    "No support for ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    }
                } else {
                    Toast
                        .makeText(
                            applicationContext,
                            "No support for ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }
}
