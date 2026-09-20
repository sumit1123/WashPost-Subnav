package com.wapo.flagship.features.audio.models

/**
 * This class represents the playback speed for Polly audio.
 * [speed] is the playback speed in [Float]
 */
sealed class PlaybackSpeed(open val speed: Float, open val text: String){
    /**
     * 0.75X
     */
    class Slow(override val speed: Float = 0.75f, override val text: String = "0.75x"): PlaybackSpeed(speed, text)
    /**
     * 1X / Default
     */
    class Normal(override val speed: Float = 1f, override val text: String = "1x"): PlaybackSpeed(speed, text)
    /**
     * 1.25X
     */
    class Quick(override val speed: Float = 1.25f, override val text: String = "1.25x"): PlaybackSpeed(speed, text)
    /**
     * 1.5X
     */
    class Fast(override val speed: Float = 1.5f, override val text: String = "1.5x"): PlaybackSpeed(speed, text)
    /**
     * 1.75X
     */
    class Faster(override val speed: Float = 1.75f, override val text: String = "1.75x"): PlaybackSpeed(speed, text)
    /**
     * 2X
     */
    class Fastest(override val speed: Float = 2f, override val text: String = "2x"): PlaybackSpeed(speed, text)
}
