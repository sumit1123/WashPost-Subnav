package com.wapo.flagship.features.audio.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.wapo.flagship.features.audio.adapter.PlaybackSpeedListAdapter
import com.wapo.flagship.features.audio.databinding.FragmentPlaybackSpeedDialogBinding
import com.wapo.flagship.features.audio.models.PlaybackSpeed
import com.wapo.flagship.features.audio.viewmodels.PlaybackSpeedViewModel
import com.wapo.fragment.BaseBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * A dialog that shows up when user taps on the playback speed in audio player.
 * It lets you change the [PlaybackSpeed] of the audio that's currently playing.
 */
@AndroidEntryPoint
class PlaybackSpeedDialogFragment : BaseBottomSheetDialogFragment() {

    private var _binding: FragmentPlaybackSpeedDialogBinding? = null
    private val binding get() = _binding!!

    private val playbackSpeedViewModel: PlaybackSpeedViewModel by activityViewModels()

    private val playbackSpeedListAdapter =
        PlaybackSpeedListAdapter {
            handleItemClick(it)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaybackSpeedDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.rcvContentView.adapter = playbackSpeedListAdapter
        playbackSpeedListAdapter.submitList(
            listOf(
                PlaybackSpeed.Slow(),
                PlaybackSpeed.Normal(),
                PlaybackSpeed.Quick(),
                PlaybackSpeed.Fast(),
                PlaybackSpeed.Faster(),
                PlaybackSpeed.Fastest()
            )
        )
    }

    private fun handleItemClick(playbackSpeed: PlaybackSpeed) {
        playbackSpeedViewModel.selectPlaybackSpeed(playbackSpeed)
        dismiss()
    }

    companion object {
        const val TAG = "PlaybackSpeedDialogFragment"
    }
}