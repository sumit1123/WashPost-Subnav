// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.podcast

import android.content.Context
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.FusionActivity
import com.wapo.flagship.features.audio.AudioMediaActivity
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import kotlin.properties.Delegates

class InlineAudioSectionView(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : LinearLayout(
        context,
        attrs,
        defStyleAttr,
    ) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val playPause: ImageView
    private val playerStatus: TextView
    private val loadingSpinner: ProgressBar
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private var audioMediaConfig: AudioMediaConfig? = null
    private var listenText: String? = null
    private val activityViewModel = context?.findActivityOfType<AudioMediaActivity>()?.getAudioMediaActivityViewModel()
    private val duration: TextView
    private var nightMode by Delegates.notNull<Int>()
    private val view: View

    init {
        val inflater = LayoutInflater.from(context)
        view = inflater.inflate(R.layout.item_section_inline_podcast, this, true)
        duration = view.findViewById(R.id.item_duration)
        playPause = view.findViewById(R.id.play_pause)
        playerStatus = view.findViewById(R.id.player_status)
        loadingSpinner = view.findViewById(R.id.loading_spinner)
        nightMode = AppCompatDelegate.getDefaultNightMode()

        duration.text =
            context?.let {
                AudioViewUtils.getDurationText(
                    audioMediaConfig?.duration,
                    it,
                )
            }
    }

    fun init(
        audioMediaConfig: AudioMediaConfig,
        listenText: String,
    ) {
        this.audioMediaConfig = audioMediaConfig
        this.listenText = listenText
        resetUi()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnClickListener { _ ->
            audioMediaConfig?.let {
                context.findActivityOfType<FusionActivity>()?.playAudioArticle(it, it.arcId)
            }
        }
        observeNowPlayingMediaItemObserver()
    }

    private fun observeNowPlayingMediaItemObserver() {
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver =
            Observer<NowPlayingAudioItem?> { nowPlayingAudioItem ->
                nowPlayingAudioItem ?: return@Observer
                if ((nowPlayingAudioItem.audioMediaConfig?.id == audioMediaConfig?.id)) {
                    when (nowPlayingAudioItem.audioPlaybackState) {
                        is AudioPlaybackState.Playing -> {
                            resetUi()
                            loadingSpinner.visibility = View.GONE
                            playPause.setImageResource(com.wapo.view.R.drawable.pause_icon_btn)
                            playPause.visibility = View.VISIBLE
                            val audioBtn = view.findViewById<ConstraintLayout>(R.id.audio_button)
                            audioBtn.background =
                                AppCompatResources.getDrawable(
                                    context,
                                    com.wapo.view.R.drawable.audio_pill_play,
                                )
                            if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                                duration.setTextColor(Color.BLACK)
                            } else {
                                duration.setTextColor(Color.WHITE)
                            }
                        }
                        AudioPlaybackState.Connecting,
                        AudioPlaybackState.Buffering,
                        AudioPlaybackState.JSONSourceInitializing,
                        AudioPlaybackState.JSONSourceInitialized,
                        -> {
                            resetUi()
                            loadingSpinner.visibility = View.VISIBLE
                            playPause.visibility = View.INVISIBLE
                        }
                        else -> {
                            resetUi()
                        }
                    }
                } else {
                    resetUi()
                }
            }.also {
                activityViewModel?.nowPlayingAudioItem?.observeForever(it)
            }
    }

    private fun resetUi() {
        playPause.visibility = View.VISIBLE
        loadingSpinner.visibility = View.GONE
        playPause.setImageResource(com.wapo.view.R.drawable.play_icon_btn)
        duration.text =
            context?.let {
                AudioViewUtils.getDurationText(
                    audioMediaConfig?.duration,
                    it,
                )
            }
        val audioBtn = view.findViewById<ConstraintLayout>(R.id.audio_button)
        audioBtn.background =
            AppCompatResources.getDrawable(
                context,
                com.wapo.view.R.drawable.audio_pill_default,
            )
        duration.setTextColor(
            AppCompatResources.getColorStateList(context, com.wapo.view.R.color.headline_text_color),
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setOnClickListener(null)
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = null
    }

    override fun onSaveInstanceState(): Parcelable? = SavedState(super.onSaveInstanceState())

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.baseState)
        }
    }

    class SavedState(
        val baseState: Parcelable?,
    ) : Parcelable {
        override fun describeContents(): Int = 0

        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeParcelable(baseState, flags)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                val baseState = `in`.readParcelable<Parcelable>(SavedState::class.java.classLoader)
                return SavedState(baseState)
            }

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
