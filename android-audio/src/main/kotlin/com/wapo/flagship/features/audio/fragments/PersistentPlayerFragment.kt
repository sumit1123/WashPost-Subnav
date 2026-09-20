package com.wapo.flagship.features.audio.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.flagship.features.audio.AudioActivity
import com.wapo.flagship.features.audio.models.AudioEvent
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.tts.domain.OnTtsEvent
import com.wapo.flagship.features.tts.viewmodel.TtsAudioPlayerViewModel
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.min

@AndroidEntryPoint
class PersistentPlayerFragment : Fragment() {

    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()
    private val personalizedPodcastViewModel: PersonalizedPodcastViewModel by activityViewModels()
    private val ttsAudioPlayerViewModel: TtsAudioPlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                min(
                    resources.getDimension(com.wapo.view.R.dimen.bottom_sheet_max_width).toInt(),
                    resources.displayMetrics.widthPixels
                ),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setContent {
                val nowPlayingItem by audioMediaActivityViewModel.nowPlayingAudioItem.observeAsState()
                val persoUiState by personalizedPodcastViewModel.uiState.collectAsState()
                val generationState = persoUiState.generationState
                val playbackState by audioMediaActivityViewModel.audioPlaybackState.observeAsState(AudioPlaybackState.None)
                val percentage by audioMediaActivityViewModel.position.observeAsState(0f)
                val isPlayingAd by audioMediaActivityViewModel.isPlayingAd.collectAsStateWithLifecycle(false)
                val ttsState by ttsAudioPlayerViewModel.ttsState.collectAsStateWithLifecycle()

                AndroidClassicTheme {
                    PersistentMiniPlayer(
                        state = AudioPlayerUiState(
                            nowPlayingItem = nowPlayingItem,
                            generationState = generationState,
                            playbackState = playbackState,
                            percentage = if (ttsState.isSpeaking || ttsState.isPause) ttsState.progress else percentage,
                            isPersonalizedPodcast = PersonalizedPodcastHelper.isPersonalizedPodcastItem(nowPlayingItem?.mediaItemData?.audioType),
                            ttsState = ttsState
                        ),
                        isPlayingAd = isPlayingAd,
                        onEvent = { event ->
                            when (event) {
                                AudioPlayerUiEvent.PauseOrPlay -> {
                                    if (ttsState.isSpeaking) {
                                        ttsAudioPlayerViewModel.pauseTts()
                                    } else if (ttsState.isPause) {
                                        ttsAudioPlayerViewModel.resumeTts()
                                    } else {
                                        audioMediaActivityViewModel.pauseOrPlay()
                                    }
                                }
                                AudioPlayerUiEvent.Expand -> {
                                    val podcastPagerFragment = AudioPagerFragment.newInstance()
                                    podcastPagerFragment.show(
                                        parentFragmentManager,
                                        AudioPagerFragment.FRAGMENT_TAG
                                    )
                                }
                                is AudioPlayerUiEvent.Close -> {
                                    personalizedPodcastViewModel.cancelPodcastGeneration()
                                    audioMediaActivityViewModel.stopMedia(event.conclusionState)
                                    (activity as? AudioActivity)?.removePersistentPlayerFragment()
                                    ttsAudioPlayerViewModel.stopTts()
                                }
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAudioEvents()
        observeTtsEvents()
    }

    override fun onResume() {
        super.onResume()
        audioMediaActivityViewModel.updatePosition()
    }

    private fun observeAudioEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                audioMediaActivityViewModel.event.collect { event ->
                    when (event) {
                        is AudioEvent.LaunchTts -> {
                            val currentAudio = audioMediaActivityViewModel.nowPlayingAudioItem.value
                            currentAudio?.audioMediaConfig?.contentUrl?.let {
                                ttsAudioPlayerViewModel.playSource(it)
                            }
                        }
                        is AudioEvent.Stop -> {
                            ttsAudioPlayerViewModel.stopTts()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeTtsEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ttsAudioPlayerViewModel.ttsEvents.collect { event ->
                    when (event) {
                        is OnTtsEvent.OnTtsError -> {
                            Toast.makeText(requireContext(), resources.getString(event.errorId), Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    companion object {
        const val FRAGMENT_TAG = "persistent_player_fragment"

        @JvmStatic
        fun newInstance() = PersistentPlayerFragment()
    }
}
