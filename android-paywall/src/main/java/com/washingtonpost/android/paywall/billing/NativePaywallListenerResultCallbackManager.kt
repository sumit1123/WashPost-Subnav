package com.washingtonpost.android.paywall.billing

object NativePaywallListenerResultCallbackManager {
    private val callbackMap: MutableMap<Long, NativePaywallResultCallbacks> = mutableMapOf()

    fun registerCallback(requestCode: Long, callback: NativePaywallResultCallbacks) {
        callbackMap[requestCode] = callback
    }

    fun unregisterCallback(requestCode: Long) {
        callbackMap.remove(requestCode)
    }

    fun getCallback(requestCode: Long): NativePaywallResultCallbacks? {
        return callbackMap[requestCode]
    }
}