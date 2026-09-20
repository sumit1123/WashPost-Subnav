package com.wapo.flagship.features.audio.views

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.Observer
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.audio.*
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.text.WpTextAppearanceSpan

class PodcastSectionView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): RelativeLayout(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val playButton: ImageView
    private val pauseButton: ImageView
    private val playerStatus: TextView
    private val playerDuration: TextView
    private val loadingSpinner : ProgressBar
    private var audioMediaConfig: AudioMediaConfig? = null
    private var playerStatusStyle: Int = -1
    private var durationStyle: Int = -1
    private var isCurrentMediaActive: Boolean = false
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private val activityViewModel = context?.findActivityOfType<AudioMediaActivity>()?.getAudioMediaActivityViewModel()

    init {
        val a = context!!.obtainStyledAttributes(attrs, R.styleable.PodcastSectionView, defStyleAttr, 0)
        try {
            playerStatusStyle = a.getResourceId(R.styleable.PodcastSectionView_audio_player_status_style, R.style.audio_cell_style_player_status)
            durationStyle = a.getResourceId(R.styleable.PodcastSectionView_audio_duration_style, R.style.audio_cell_style)
        } finally {
            a.recycle()
        }
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.native_audio_section_front_view, this, true)
        playButton = view.findViewById(R.id.exo_play)
        pauseButton = view.findViewById(R.id.exo_pause)
        playerStatus = view.findViewById(R.id.player_status)
        playerDuration = view.findViewById(R.id.duration)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
    }

    fun setAudioMediaConfig(audioMediaConfig: AudioMediaConfig) {
        this.audioMediaConfig = audioMediaConfig
        resetUi()
    }

    private fun resetUi() {
        isCurrentMediaActive = false
        audioMediaConfig?.duration?.let {
            setDuration(it)
        }
        setStatus("Listen")
        loadingSpinner.visibility = View.GONE
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
        playerDuration.visibility = View.VISIBLE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        playButton.setOnClickListener { _ ->
            audioMediaConfig?.let { activityViewModel?.playMedia(it) }
        }
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = Observer<NowPlayingAudioItem?> { nowPlayingAudioItem ->
            val mediaItem = nowPlayingAudioItem?.mediaItemData ?: return@Observer
            if (mediaItem.mediaId != null && mediaItem.mediaId == audioMediaConfig?.id) {
                when (nowPlayingAudioItem.audioPlaybackState) {
                    is AudioPlaybackState.Playing -> {
                        setStatus("Now Playing")
                        loadingSpinner.visibility = View.GONE
                        playButton.visibility = View.GONE
                        pauseButton.visibility = View.VISIBLE
                        playerDuration.visibility = View.GONE
                        isCurrentMediaActive = true
                    }
                    AudioPlaybackState.Connecting,
                    AudioPlaybackState.Buffering -> {
                        setStatus("Buffering")
                        loadingSpinner.visibility = View.VISIBLE
                        playButton.visibility = View.GONE
                        pauseButton.visibility = View.GONE
                        playerDuration.visibility = View.GONE
                        isCurrentMediaActive = true
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setOnClickListener(null)
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = null
    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.baseState)
        }
    }

    class SavedState(val baseState: Parcelable?) : Parcelable {
        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(baseState, flags)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                val baseState = `in`.readParcelable<Parcelable>(SavedState::class.java.classLoader)
                return SavedState(baseState)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    private fun setStatus(status: String) {
        val statusSpan = SpannableStringBuilder(status)
        statusSpan.setSpan(
                WpTextAppearanceSpan(
                        context,
                        playerStatusStyle
                ),
                0, statusSpan.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        playerStatus.text = statusSpan
    }

    private fun setDuration(durationInSeconds : Long) {
        val duration = DateUtils.formatElapsedTime(durationInSeconds)
        val durationSpan = SpannableStringBuilder(duration)

        durationSpan.setSpan(
                WpTextAppearanceSpan(
                        context,
                        durationStyle
                ),
                0, durationSpan.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        playerDuration.text = durationSpan
    }
}