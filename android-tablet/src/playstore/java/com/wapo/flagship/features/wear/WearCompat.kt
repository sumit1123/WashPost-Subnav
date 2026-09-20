// package com.wapo.flagship.features.wear
//
// import android.content.BroadcastReceiver
// import android.content.Context
//
// /**
// * Created by kattim on 2/28/18.
// */
// class WearCompat() {
//
//    private var wearListener:WearListener? = WearListener()
//
//    fun canSupportWear() : Boolean {
//        return false
//    }
//
//    fun onCreate(context: Context) {
//        wearListener?.activityCreated(context)
//    }
//
//    fun onResume() {
//        wearListener?.activityResumed()
//    }
//
//    fun onPause() {
//        wearListener?.activityPaused()
//    }
// }
