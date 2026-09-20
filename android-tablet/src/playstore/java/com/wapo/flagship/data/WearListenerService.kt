// package com.wapo.flagship.data
//
// import android.content.Intent
// import com.wapo.android.commons.util.LogUtil
// import androidx.localbroadcastmanager.content.LocalBroadcastManager
// import com.google.android.gms.wearable.*
// import com.wapo.flagship.content.notifications.NotificationData
// import com.wapo.flagship.FlagshipApplication
// import com.washingtonpost.android.paywall.PaywallService
//
// /**
// * Created by moons on 7/26/17.
// */
// class WearListenerService : WearableListenerService() {
//
//    private val dataClient by lazy { Wearable.getDataClient(this) }
//
//    override fun onMessageReceived(messageEvent: MessageEvent) {
//        super.onMessageReceived(messageEvent)
//
//        val message = String(messageEvent.data)
//        LogUtil.d(TAG, "Message received in mobile: $message")
//
//        val notificationDataList =
//            FlagshipApplication.getInstance().cacheManager.notifications ?: emptyList()
//
//        when (messageEvent.path) {
//            "/DataLayerNotification" -> {
//                if (message == REQUEST_NOTIFICATION) {
//                    val mapList = notificationDataList
//                        .map { convertNotificationToDataMap(it) }
//                        .reversed()
//                    val request = PutDataMapRequest.create("/DataLayerNotification")
//                        .apply {
//                            dataMap.putDataMapArrayList(NOTIFICATION_LIST, ArrayList(mapList))
//                            dataMap.putLong("update_time", System.currentTimeMillis())
//                        }
//                        .setUrgent()
//                        .asPutDataRequest()
//                    dataClient.putDataItem(request)
//                        .addOnSuccessListener { dataItem ->
//                            LogUtil.d(TAG, "DataClient success: $dataItem")
//                        }
//                        .addOnFailureListener { e ->
//                            LogUtil.d(TAG, "DataClient success: $e")
//                        }
//                } else {
//                    Intent("WEAR_REQUEST")
//                        .apply { putExtra("REQUEST", message) }
//                        .also { LocalBroadcastManager.getInstance(this).sendBroadcast(it) }
//                }
//            }
//            "/SubscriptionStatus" -> {
//                LogUtil.d(TAG, PaywallService.getInstance().wapoAccessServiceInstance.isPremiumUser.toString())
//                val request = PutDataMapRequest.create("/SubscriptionStatus")
//                    .apply {
//                        dataMap.putBoolean(
//                            "is_premium_user",
//                            PaywallService.getInstance().wapoAccessServiceInstance.isPremiumUser
//                        )
//                        dataMap.putLong("update_time", System.currentTimeMillis())
//                    }
//                    .setUrgent()
//                    .asPutDataRequest()
//                dataClient.putDataItem(request)
//                    .addOnSuccessListener { dataItem ->
//                        LogUtil.d(TAG, "DataClient success: $dataItem")
//                    }
//                    .addOnFailureListener { e ->
//                        LogUtil.d(TAG, "DataClient success: $e")
//                    }
//            }
//        }
//
//    }
//
//    // convert the article data into DataMap object so it can be sent to the Data Layer.
//    private fun convertNotificationToDataMap(notification: NotificationData): DataMap {
//        return DataMap().apply {
//            notification.headline?.let { putString("headline", it) }
//            notification.storyUrl?.let { putString("story_url", it) }
//            putBoolean("read", notification.isRead)
//            notification.notifId?.let { putString("notification_id", it) }
//            notification.timestamp?.let { putString("time_stamp", it) }
//        }
//    }
//
//    companion object {
//        private const val TAG = "WearListenerService"
//        private const val NOTIFICATION_LIST = "notification_list"
//        private const val REQUEST_NOTIFICATION = "REQUEST_NOTIFICATION"
//    }
// }
