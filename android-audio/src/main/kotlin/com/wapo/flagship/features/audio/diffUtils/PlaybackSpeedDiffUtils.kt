package com.wapo.flagship.features.audio.diffUtils

import androidx.recyclerview.widget.DiffUtil
import com.wapo.flagship.features.audio.fragments.PlaybackSpeedDialogFragment
import com.wapo.flagship.features.audio.models.PlaybackSpeed


/**
 * diff util for the playback speed recycler view in the playback speed dialog [PlaybackSpeedDialogFragment].
 */
class PlaybackSpeedDiffUtils : DiffUtil.ItemCallback<PlaybackSpeed>() {

    override fun areItemsTheSame(old: PlaybackSpeed, aNew: PlaybackSpeed): Boolean {
        return old.speed == aNew.speed
    }

    override fun areContentsTheSame(old: PlaybackSpeed, aNew: PlaybackSpeed): Boolean {
        return old.speed == aNew.speed
    }
}