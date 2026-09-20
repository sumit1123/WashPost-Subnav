package com.wapo.flagship.features.audio.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.audio.adapter.PlaybackSpeedListAdapter
import com.wapo.flagship.features.audio.databinding.ItemPlaybackSpeedBinding
import com.wapo.flagship.features.audio.fragments.PlaybackSpeedDialogFragment
import com.wapo.flagship.features.audio.models.PlaybackSpeed

/**
 * View holder used in the playback speed recycler view in [PlaybackSpeedDialogFragment]
 * and used with [PlaybackSpeedListAdapter]
 */
class PlaybackSpeedViewHolder(val binding: ItemPlaybackSpeedBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(playbackSpeed: PlaybackSpeed, onClick: ((PlaybackSpeed) -> Unit)) {
        val playBackSpeedTextView = binding.tvPlaybackSpeed
        playBackSpeedTextView.text = playbackSpeed.text
        binding.containerView.setOnClickListener {
            onClick.invoke(playbackSpeed)
        }
    }
}