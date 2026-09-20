/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.wapo.android.commons.util.LogUtil
import com.wapo.flagship.WearFlagshipApplication

/**
 * A class for sending messages from wear to the mobile using GoogleApi
 * using Wearable Data Layer API.
 *
 */
class MobileMessenger(private val context: Context) {

    private val messageClient by lazy { Wearable.getMessageClient(context) }

    fun sendMessage(path: String, message: String) {
        val connectedNode = WearFlagshipApplication.getInstance().connectedMobileNode
        connectedNode?.let {
            messageClient
                .sendMessage(it.id, "/$path", message.toByteArray())
                .addOnSuccessListener {
                    LogUtil.d(TAG, "Message sent to mobile successfully: $message")
                }
                .addOnFailureListener { e ->
                    LogUtil.d(TAG, "Failed to send message: $e")
                }
        }
    }

    companion object {
        private const val TAG = "MobileMessageSender"
    }
}