package com.wapo.flagship.features.settings

import android.content.Context
import android.os.Bundle
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.fragments.PlaybackSpeedDialogFragment
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.audio.viewmodels.PlaybackSpeedViewModel
import com.wapo.flagship.features.onboarding2.activity.Onboarding2Activity
import com.wapo.flagship.features.onboarding2.viewstatehelper.AudioViewStateHelper
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.databinding.FragmentSettingsAudioBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsAudioFragment : Fragment() {
    private val TAG = SettingsAudioFragment::class.java.name

    private lateinit var binding: FragmentSettingsAudioBinding

    private var isOnboarding = false
    private var lastTrackedSpeed: Float = SPEED_NOT_INITIALIZED

    /**
     * Viewmodel to update and access users audio speed settings
     */
    private val playbackSpeedViewModel: PlaybackSpeedViewModel by activityViewModels()

    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()

    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null

    /**
     * Helper to update UI states
     */
    private var _customizeAudioViewStateHelper: AudioViewStateHelper? = null
    private val viewStateHelper get() = _customizeAudioViewStateHelper!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Onboarding2Activity) {
            isOnboarding = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSettingsAudioBinding.inflate(inflater, container, false)
        _customizeAudioViewStateHelper = AudioViewStateHelper(binding, requireContext())
        lastTrackedSpeed = AudioPreferences.getAudioPlaybackSpeed(requireContext())
        return binding.root
    }

    /**
     * Initialize button events, live data observing and initial values
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        PrefUtils.setShouldShowAudioOnboarding(context, false)
        if (isOnboarding) {
            binding.header.visibility = View.GONE
        }
        binding.playPauseButton.setOnClickListener {
            playAudio()
        }
        binding.incrementSpeed.setOnClickListener {
            playbackSpeedViewModel.incrementPlaybackSpeed()
        }
        binding.decrementSpeed.setOnClickListener {
            playbackSpeedViewModel.decrementPlaybackSpeed()
        }

        observePlaybackSpeedChanges()
        observeMediaState()
        playbackSpeedViewModel.selectPlaybackSpeed(lastTrackedSpeed)
    }

    /**
     * Tracks and assembles value of Miscellany parameter for audio submission event.
     */
    fun trackAudioPrefsSelection(entryType: String) {
        val speed = "${playbackSpeedViewModel.selectedPlaybackSpeed.value?.text}"
        val miscellany = "${Measurement.PROFILE_PREFERENCE_AUDIO};speed:$speed;$entryType"
        Measurement.trackOnboardingClick(miscellany)
    }

    /**
     * Observes the selection of playback speed from [PlaybackSpeedDialogFragment] dialog
     * to set the appropriate text to the button and change the playback speed of the player.
     */
    private fun observePlaybackSpeedChanges() {
        playbackSpeedViewModel.selectedPlaybackSpeed.observe(viewLifecycleOwner) {
            audioMediaActivityViewModel.setPlaybackSpeed(it)
            binding.currentSpeedText.text = it.text
        }
    }

    /**
     * Observe mediastate and update ui accordingly
     */
    private fun observeMediaState() {
        nowPlayingObserver?.let {
            audioMediaActivityViewModel.nowPlayingAudioItem.removeObserver(it)
        }
        nowPlayingObserver =
            Observer<NowPlayingAudioItem?> { nowPlayingAudioItem ->
                nowPlayingAudioItem ?: return@Observer
                if (nowPlayingAudioItem.audioPlaybackState == AudioPlaybackState.JSONSourceInitializing ||
                    nowPlayingAudioItem.audioPlaybackState == AudioPlaybackState.JSONSourceInitialized
                ) {
                    viewStateHelper.showAudioLoadingState()
                    return@Observer
                } else if (nowPlayingAudioItem.audioPlaybackState == AudioPlaybackState.JSONSourceError) {
                    viewStateHelper.showAudioErrorState {}
                    return@Observer
                }
                val audioMediaConfig = nowPlayingAudioItem.audioMediaConfig ?: return@Observer
                audioMediaConfig.id.let {
                    when (nowPlayingAudioItem.audioPlaybackState) {
                        AudioPlaybackState.Buffering,
                        AudioPlaybackState.Connecting,
                        -> {
                            viewStateHelper.showAudioLoadingState()
                        }
                        is AudioPlaybackState.Error -> {
                            viewStateHelper.showAudioErrorState {
                                stopAudio()
                            }
                        }
                        is AudioPlaybackState.Playing -> {
                            viewStateHelper.showAudioPlayingState()
                        }
                        else -> {
                            viewStateHelper.showAudioStopState()
                        }
                    }
                }
            }.also {
                audioMediaActivityViewModel.nowPlayingAudioItem.observeForever(it)
            }
    }

    /**
     * Play Sample audio using audio manager
     */
    private fun playAudio() {
        val config =
            AudioMediaConfig(
                rawUrl = "https://rainbowdatanet-a.wpdigital.net/native/app-settings/audio/Ava.m4a",
            )
        audioMediaActivityViewModel.playMedia(config)
    }

    /**
     * Stop Sample audio and also make sure persistent player is not visible.
     */
    private fun stopAudio() {
        audioMediaActivityViewModel.stopMedia()
    }

    override fun onPause() {
        trackSelectionIfNeeded()
        super.onPause()
        stopAudio()
    }

    /**
     * Track selection if selected speed or voice are different than last tracked speed or voice.
     * Remote log if there is some initialization error.
     * Do not track in onboarding because we track when onboarding ends.
     */
    private fun trackSelectionIfNeeded() {
        if (!isOnboarding) {
            val speed = playbackSpeedViewModel.selectedPlaybackSpeed.value?.speed
            if (speed == null || lastTrackedSpeed == SPEED_NOT_INITIALIZED) {
                val error = "Audio Preferences error: Speed:$speed, LastTrackedSpeed:$lastTrackedSpeed"
                Logger.e(TAG, error)
                EventLog
                    .Builder()
                    .apply {
                        setMessage("Settings Audio Error")
                        setModule(LogModules.AUDIO)
                        setErrorMessage(error)
                    }.run {
                        RemoteLog.e(context, build())
                    }
            } else if (lastTrackedSpeed != speed) {
                trackAudioPrefsSelection("settings")
                lastTrackedSpeed = speed
            }
        }
    }

    companion object {
        private const val SPEED_NOT_INITIALIZED = 0f
    }
}
