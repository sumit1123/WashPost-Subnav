package com.wapo.flagship.features.ask.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.wapo.android.commons.constants.X_SURFACE_NAME
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.ask.session.AskSessionGuard
import com.wapo.flagship.features.ask.session.AskSessionOwner
import com.wapo.flagship.features.personalizedpodcasts.events.UserEvent
import com.wapo.flagship.features.search2.remote.OkHttpSseService.Companion.CONVERSATION_ID
import com.wapo.flagship.features.search2.remote.OkHttpSseService.Companion.MEDIA_TIMESTAMP_KEY
import com.wapo.flagship.features.search2.remote.OkHttpSseService.Companion.TRANSCRIPT_URL
import com.washingtonpost.android.R

class TalkToThePostBottomSheetFragmentFactory(
    private var eventTrigger: ((UserEvent) -> Unit)? = null
) {

    private var conversationId: String? = null
    private var surfaceName: String? = null
    private var passedAudioProvider: AudioProvider? = null
    private var timestamp: Float? = null
    private var transcript: String? = null

    fun tryToShowTalkFragment(
        parentActivity: Activity,
        parentFragmentManager: FragmentManager,
        conversationId: String?,
        surfaceName: String?,
        isFirstDenial: Boolean,
        recordAudioPermissionRequest: ActivityResultLauncher<String>? = null,
        audioProvider: AudioProvider? = null,
        timestamp: Float? = null,
        transcript: String? = null,
    ) {
        this.conversationId = conversationId
        this.surfaceName = surfaceName
        this.passedAudioProvider = audioProvider
        this.timestamp = timestamp
        this.transcript = transcript

        val permissionGranted = ContextCompat.checkSelfPermission(
            parentActivity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val isActiveInAndroidAuto =
            AskSessionGuard.isOwnedBy(AskSessionOwner.ANDROID_AUTO)

        if (permissionGranted || isActiveInAndroidAuto) {
            eventTrigger?.invoke(UserEvent.TalkToThePostOpening())
            showTalkFragment(parentFragmentManager)
        } else {
            recordAudioPermissionRequest?.launch(Manifest.permission.RECORD_AUDIO) ?: run {
                showPermissionAlertDialog(parentActivity, isFirstDenial)
            }
        }
    }

    private fun showTalkFragment(parentFragmentManager: FragmentManager) {
        TalkToThePostBottomSheetFragment().apply {
            this.audioProvider = passedAudioProvider
            arguments = Bundle().apply {
                putString(CONVERSATION_ID, conversationId)
                timestamp?.let { putFloat(MEDIA_TIMESTAMP_KEY, it) }
                transcript?.let { putString(TRANSCRIPT_URL, it) }
                surfaceName?.let { putString(X_SURFACE_NAME, it) }
            }
        }.show(
            parentFragmentManager,
            TalkToThePostBottomSheetFragment.TAG
        )
    }

    fun showPermissionAlertDialog(parentActivity: Activity, isFirstDenial: Boolean) {
        val alertDialog = AlertDialog.Builder(parentActivity).apply {
            setTitle(context.resources.getString(com.wpds.wpds.R.string.microphone_access_title))
            setMessage(context.resources.getString(com.wpds.wpds.R.string.microphone_access_message))
            setNegativeButton(context.resources.getString(R.string.dismiss)) { _, _ -> /* No-op, just close the dialog */ }
        }

        /* Don't push the user to go to settings if this is only the first denial, just show
           the rationale */
        if (!isFirstDenial) {
            alertDialog.setPositiveButton(parentActivity.resources.getString(com.wpds.wpds.R.string.open_settings)) { _, _ ->
                val uri = Uri.fromParts("package", parentActivity.packageName, null)
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                parentActivity.startActivity(intent)
            }
        }
        alertDialog.show()
    }

    fun onDestroy() {
        eventTrigger = null
    }
}
