package com.wapo.flagship.features.notification

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A plugin class for requesting notification permissions in an Android application.
 * It implements the DefaultLifecycleObserver interface to observe the lifecycle events of the owner.
 * @param activity The activity instance that is used for requesting permissions.
 * @param condition Request only if condition is true
 */
class NotificationPermissionRequestPlugin(
    private val activity: Activity,
    private val activityLauncher: ActivityResultLauncher<String>,
    private val condition: () -> Boolean
): DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onResume(owner)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isPermissionGranted() && condition()) {
                activityLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}