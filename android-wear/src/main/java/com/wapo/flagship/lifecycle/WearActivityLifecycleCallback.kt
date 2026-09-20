/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.wapo.android.commons.util.LogUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.data.FreezableUtils
import com.google.android.gms.wearable.*
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.WearFlagshipApplication
import com.wapo.flagship.common.InfoActivity
import com.wapo.flagship.features.homepage.activities.HomepageActivity
import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.utils.connectivity.ConnectivityObserver
import com.wapo.flagship.utils.connectivity.NetworkConnectivityObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class WearActivityLifecycleCallback :
    Application.ActivityLifecycleCallbacks,
    CapabilityClient.OnCapabilityChangedListener,
    DataClient.OnDataChangedListener {

    private var homepageActivityWeakRef: WeakReference<HomepageActivity>? = null
    private lateinit var connectivityObserver: ConnectivityObserver

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activity is HomepageActivity) {
            homepageActivityWeakRef = WeakReference(activity)
            checkIfConnectedToPhone()
        }

        connectivityObserver = NetworkConnectivityObserver(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AppCompatActivity) {
            Wearable.getCapabilityClient(activity)
                .addListener(this, VERIFY_MOBILE_APP_CAPABILITY_NAME)
            Wearable.getDataClient(activity)
                .addListener(this)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is AppCompatActivity) {
            homepageActivityWeakRef = null
            Wearable.getCapabilityClient(activity).removeListener(this)
            Wearable.getDataClient(activity).removeListener(this)
        }
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        LogUtil.d(TAG, "onCapabilityChanged(): $capabilityInfo")

        WearFlagshipApplication.getInstance().connectedMobileNode =
            pickBestNode(capabilityInfo.nodes)
        WearFlagshipApplication.getInstance().verifyNodeAndUpdateUi()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        LogUtil.d(TAG, "onDataChanged()")

        // make sure the data is constant when retrieving to avoid errors
        val events: List<DataEvent> = FreezableUtils.freezeIterable(dataEvents)
        events.forEach { event ->
            LogUtil.d(TAG, "Data received in wear")
            // get the uri path
            val uri = event.dataItem.uri

            when (uri.path) {
                "/${WearFlagshipApplication.DATA_LAYER_NOTIFICATION}" -> {
                    sendNotificationListBroadcast(event)
                }
                "/${WearFlagshipApplication.SUBSCRIPTION_STATUS}" -> {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    WearAppContext.isPremiumUser = dataMap.getBoolean("is_premium_user")
                }
            }
        }
    }

    private fun checkIfConnectedToPhone() {
        LogUtil.d(TAG, "checkIfConnectedToPhone()")
        if (homepageActivityIsInactive()) return

        Wearable
            .getCapabilityClient(homepageActivityWeakRef!!.get()!!)
            .getCapability(VERIFY_MOBILE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capabilityInfo ->
                LogUtil.d(TAG, "Get capability info successfully: $capabilityInfo")
                WearFlagshipApplication.getInstance().connectedMobileNode =
                    pickBestNode(capabilityInfo.nodes)
                WearFlagshipApplication.getInstance().verifyNodeAndUpdateUi()
            }
            .addOnFailureListener { e ->
                LogUtil.d(TAG, "Failed to get capability info: $e")
            }
    }

    private fun sendNotificationListBroadcast(event: DataEvent) {
        val notificationDataMapList = DataMapItem
            .fromDataItem(event.dataItem)
            .dataMap
            .getDataMapArrayList(WearFlagshipApplication.NOTIFICATION_LIST)
            ?: ArrayList()

        val notificationList = notificationDataMapList.map { dataMap ->
            ArticleMeta(
                dataMap.getString("headline") ?: "",
                dataMap.getString("story_url") ?: "",
                null,
                dataMap.getString("time_stamp"),
                null
            )
        }

        // send the data to a notification list broadcast receiver
        val intent = Intent(WearFlagshipApplication.NOTIFICATION_INTENT)
        intent.putParcelableArrayListExtra(
            WearFlagshipApplication.NOTIFICATION_LIST,
            ArrayList(notificationList)
        )
        LocalBroadcastManager
            .getInstance(homepageActivityWeakRef!!.get()!!)
            .sendBroadcast(intent)
    }

    /**
     *  Picks a node that is in close proximity to Wear device to
     *  minimize message routing through multiple nodes.
     *
     */
    private fun pickBestNode(nodes: Set<Node>): Node? {
        LogUtil.d(TAG, "pickBestNodeId(): $nodes")

        return nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
    }

    private fun homepageActivityIsInactive(): Boolean {
        return homepageActivityWeakRef?.get() == null
    }

    companion object {
        private const val TAG = "WearActivityLifecycleCallback"
        const val VERIFY_MOBILE_APP_CAPABILITY_NAME = "verify_wash_post_mobile_app"
    }

}