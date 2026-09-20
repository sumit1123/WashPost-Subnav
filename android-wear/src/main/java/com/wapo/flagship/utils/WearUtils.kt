/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.getSystemService

object WearUtils {

    fun isScreenRound(context: Context): Boolean {
        return context.resources.configuration.isScreenRound
    }

    /**
     * Determines if the device has a way to output audio and if it is supported.
     *
     * This could be an on-device speaker, or a connected bluetooth device.
     */
    fun speakerIsSupported(activity: Activity): Boolean {
        val hasAudioOutputFeature =
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
        val devices = activity.getSystemService<AudioManager>()!!
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        // We can only trust AudioDeviceInfo.TYPE_BUILTIN_SPEAKER if the device advertises
        // FEATURE_AUDIO_OUTPUT
        val hasBuiltInSpeaker = devices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } &&
                hasAudioOutputFeature

        val hasBluetoothSpeaker = devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }

        return hasBuiltInSpeaker || hasBluetoothSpeaker
    }

}