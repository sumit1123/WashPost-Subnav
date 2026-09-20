package com.wapo.flagship.features.ask.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wapo.android.commons.constants.X_SURFACE_NAME
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.ask.ui.TalkToThePostView
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.TalkToThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.TalkToThePostViewModel.TalkUiState
import com.wapo.flagship.features.audio.AudioActivity
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.remote.OkHttpSseService.Companion.CONVERSATION_ID
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.features.search2.remote.OkHttpSseService.Companion.MEDIA_TIMESTAMP_KEY
import com.wapo.flagship.features.search2.remote.OkHttpSseService.Companion.TRANSCRIPT_URL
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TalkToThePostBottomSheetFragment : BaseBottomSheetDialogFragment() {

    private val talkToThePostViewModel: TalkToThePostViewModel by viewModels()
    private val askThePostViewModel: AskThePostViewModel by activityViewModels()

    var audioProvider: AudioProvider? = null

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context?.registerReceiver(volumeReceiver, filter)
        checkVolume()
        askThePostViewModel.talkToThePostActive = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        arguments?.let {
            talkToThePostViewModel.conversationId = it.getString(CONVERSATION_ID)
            val timestamp: Float? = if (it.containsKey(MEDIA_TIMESTAMP_KEY)) {
                it.getFloat(MEDIA_TIMESTAMP_KEY)
            } else {
                null
            }
            talkToThePostViewModel.entryPoint = it.getString(X_SURFACE_NAME)
            talkToThePostViewModel.timestampAndTranscript = Pair(timestamp, it.getString(TRANSCRIPT_URL))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        talkToThePostViewModel.requestAudioFocus()
        talkToThePostViewModel.initializeListening()
        makeBackgroundTransparent()
        return ComposeView(requireContext()).apply {
            setContent {
                AndroidClassicTheme {
                    Surface(
                        color = Color.Transparent,
                    ) {
                        TalkToThePostView(
                            uiStateFlow = talkToThePostViewModel.talkUiState,
                            captionsEnabledFlow = talkToThePostViewModel.captionsEnabled,
                            previousResponseTextFlow = talkToThePostViewModel.previousResponseText,
                            responseTextFlow = talkToThePostViewModel.responseText,
                            orbRadiusFlow = talkToThePostViewModel.orbRadius,
                            captionsIndexFlow = talkToThePostViewModel.captionsIndex,
                            deviceVolumeFlow = talkToThePostViewModel.deviceVolume,
                            thinkingTextFlow = talkToThePostViewModel.thinkingText,
                            eventTrigger = { talkToThePostViewModel.eventTrigger(it) }
                        )
                    }
                }
            }
            observeUserEvents()
            Measurement.trackTalkSheetOpen()
            tryToShowVoiceSelectionFragment()
        }
    }

    private fun observeUserEvents() {
        talkToThePostViewModel.userEvent.observe(viewLifecycleOwner) { userEvent ->
            when (userEvent) {
                is UserEvent.TalkToThePostPlayPauseTapped -> {
                    talkToThePostViewModel.onTalkPlayPauseButtonTap()
                }
                is UserEvent.TalkToThePostClose -> {
                    dismiss()
                    if (askThePostViewModel.askSamEntryPoint == AskSamEntryPoint.PERSONALIZED_PODCAST) {
                        (activity as? AudioActivity)?.onAskSamEvent(com.wapo.flagship.features.personalizedpodcasts.events.UserEvent.TalkToThePostClose())
                        talkToThePostViewModel.timestampAndTranscript = Pair(null, null)
                    }
                }
                is UserEvent.TalkToThePostToggleCaptions -> {
                    talkToThePostViewModel.toggleCaptions()
                }
                is UserEvent.TalkToThePostOpenVoiceSelectionSheet -> {
                    showVoiceSelectionFragment()
                }
                is UserEvent.TalkToThePostUpdateCaptionsIndex -> {
                    talkToThePostViewModel.updateCaptionsIndex(userEvent.index)
                }
                is UserEvent.TalkToThePostCaptionsDoneRendering -> {
                    talkToThePostViewModel.onCaptionsDoneRendering()
                }
                else -> {}
            }
        }
    }

    fun getConversationId(): String? {
        return talkToThePostViewModel.conversationId
    }

    private fun checkVolume() {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            talkToThePostViewModel.updateDeviceVolume(it.getStreamVolume(AudioManager.STREAM_MUSIC))
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            checkVolume()
        }
    }

    private fun tryToShowVoiceSelectionFragment() {
        /* Show the voice selection sheet if this is the user's second visit AND they did not select
           a voice on their first visit */
        if (!talkToThePostViewModel.isMirroringAndroidAuto &&
            PrefUtils.getTalkToThePostVisitCount(context) == 1 &&
            PrefUtils.getTalkToThePostVoiceSelection(context) == null
        ) {
            showVoiceSelectionFragment()
        }
    }

    private fun showVoiceSelectionFragment() {
        talkToThePostViewModel.updateState(TalkUiState.LISTENING_ON_HOLD)
        createVoiceSelectionFragment().show(
            parentFragmentManager,
            TalkToThePostVoiceSelectionBottomSheetFragment.TAG
        )
    }

    private fun createVoiceSelectionFragment(): TalkToThePostVoiceSelectionBottomSheetFragment {
        return TalkToThePostVoiceSelectionBottomSheetFragment().apply {
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        talkToThePostViewModel.updateState(TalkUiState.LISTENING)
                    }
                }
            )
        }
    }

    override fun onPause() {
        talkToThePostViewModel.destroy()
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        context?.unregisterReceiver(volumeReceiver)
    }

    override fun onDestroy() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        talkToThePostViewModel.abandonAudioFocus()
        talkToThePostViewModel.destroy()
        context?.takeUnless { talkToThePostViewModel.isMirroringAndroidAuto }?.let {
            /* This pref is used for voice selection sheet logic, which is shown on the second visit,
               so we don't need to bother incrementing its value after the second visit */
            if (PrefUtils.getTalkToThePostVisitCount(it) < 2) {
                PrefUtils.incrementTalkToThePostVisitCount(it)
            }
        }

        askThePostViewModel.talkToThePostActive = false
        getConversationId()?.let { conversationId ->
            askThePostViewModel.openConversationWithId(conversationId)
            askThePostViewModel.hideOrShowResponse(true)
        }
        askThePostViewModel.startCollectingSseEvents()

        super.onDestroy()
    }

    companion object {
        val TAG = TalkToThePostBottomSheetFragment::class.simpleName
    }
}
