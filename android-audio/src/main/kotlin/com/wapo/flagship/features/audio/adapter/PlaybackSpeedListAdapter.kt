package com.wapo.flagship.features.audio.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.wapo.flagship.features.audio.viewholders.PlaybackSpeedViewHolder
import com.wapo.flagship.features.audio.databinding.ItemPlaybackSpeedBinding
import com.wapo.flagship.features.audio.diffUtils.PlaybackSpeedDiffUtils
import com.wapo.flagship.features.audio.fragments.PlaybackSpeedDialogFragment
import com.wapo.flagship.features.audio.models.PlaybackSpeed

/**
 * List adapter used to populate the items of the play back speed recycler view in [PlaybackSpeedDialogFragment]
 */
class PlaybackSpeedListAdapter(private val onClick: ((PlaybackSpeed) -> Unit)): ListAdapter<PlaybackSpeed, PlaybackSpeedViewHolder>(PlaybackSpeedDiffUtils()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaybackSpeedViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlaybackSpeedBinding.inflate(inflater, parent, false)
        return PlaybackSpeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaybackSpeedViewHolder, position: Int) {
        val playbackSpeedItem = getItem(position)
        holder.bind(playbackSpeedItem, onClick)
    }
}