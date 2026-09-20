// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.viewstatehelper

import android.app.AlertDialog
import android.content.Context
import android.view.View
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentSettingsAudioBinding

/**
 * Helper class to manage the various UI states for the Customize Audio Screen.
 */
class AudioViewStateHelper(
    val binding: FragmentSettingsAudioBinding,
    val context: Context,
) {
    private var voiceButtonMap = mutableMapOf<String, Chip>()

    /**
     * Update UI when audio is stopped
     */
    fun showAudioStopState() {
        binding.audioLoadProgress.visibility = View.GONE
        binding.playPauseButton.visibility = View.VISIBLE
        VectorDrawableCompat.create(context.resources, R.drawable.audio_play_icon, context.theme)?.apply {
            binding.playPauseButton.setImageDrawable(this)
        }
    }

    /**
     * Update UI when audio is played
     */
    fun showAudioPlayingState() {
        binding.audioLoadProgress.visibility = View.GONE
        binding.playPauseButton.visibility = View.VISIBLE
        VectorDrawableCompat.create(context.resources, R.drawable.audio_pause_icon, context.theme)?.apply {
            binding.playPauseButton.setImageDrawable(this)
        }
    }

    /**
     * Update UI when audio is loading
     */
    fun showAudioLoadingState() {
        binding.audioLoadProgress.visibility = View.VISIBLE
        binding.playPauseButton.visibility = View.GONE
    }

    /**
     * Update UI when audio fails to load
     */
    fun showAudioErrorState(onOk: () -> Unit) {
        binding.audioLoadProgress.visibility = View.GONE
        binding.playPauseButton.visibility = View.VISIBLE
        binding.playPauseButton.setBackgroundResource(R.drawable.audio_play_icon)
        AlertDialog
            .Builder(context)
            .setTitle(R.string.audio_error_title)
            .setMessage(R.string.audio_error_message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onOk()
            }.show()
    }

    /**
     * Load voice selection list and initialize selected item.
     */
    fun initVoiceButtons(
        list: List<PlaybackVoice>,
        storedVoiceId: String,
        onClick: (PlaybackVoice) -> Unit,
    ) {
        binding.voiceSelectionGroup.removeAllViews()
        voiceButtonMap.clear()
        list.forEach { voice ->
            val button = getVoiceChip(voice.text)
            button.setOnClickListener {
                onClick(voice)
            }
            if (voice.id == storedVoiceId) {
                button.isSelected = true
            }
            voiceButtonMap[voice.id] = button
            binding.voiceSelectionGroup.addView(button)
        }
    }

    /**
     * Load voice chip item
     */
    private fun getVoiceChip(text: String): Chip {
        val chip = Chip(context)
        val chipDrawable =
            ChipDrawable.createFromAttributes(
                context,
                null,
                0,
                R.style.Voice_Choice_Chip,
            )
        chip.setChipDrawable(chipDrawable)
        chip.setTextAppearanceResource(R.style.Voice_Choice_Chip_Text)
        chip.setEnsureMinTouchTargetSize(false)
        val paddingV =
            context.resources.getDimensionPixelOffset(R.dimen.onboarding_medium_large_space)
        chip.setPadding(0, paddingV, 0, paddingV)
        chip.textStartPadding = context.resources.getDimension(R.dimen.onboarding_large_space)
        chip.textEndPadding = context.resources.getDimension(R.dimen.onboarding_large_space)
        chip.text = text
        return chip
    }

    /**
     * Update the selected item.
     */
    fun updateSelectedVoiceUi(voiceId: String) {
        voiceButtonMap.forEach {
            it.value.isSelected = false
        }
        voiceButtonMap[voiceId]?.isSelected = true
    }

    /**
     * Update the details text based on the selected voice and speed setting
     */
    fun updateDetailsText(
        voice: String,
        speed: String,
    ) {
        val text = "Sample audio ($speed)"
        binding.audioDetailsText.text = text
    }
}
