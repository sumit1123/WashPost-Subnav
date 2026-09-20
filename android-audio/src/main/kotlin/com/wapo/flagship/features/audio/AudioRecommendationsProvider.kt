package com.wapo.flagship.features.audio

import com.wapo.flagship.features.audio.config2.AudioMediaConfig

/**
 * Provide next audio recommendations to user.
 */
interface AudioRecommendationsProvider {

    /**
     * Load the next audio recommendation from network
     */
    suspend fun loadNextAudioRecommendation(excludeList:List<String> = emptyList())

    /**
     * Get cached audio recommendation
     */
    fun getNextAudioRecommendation(): AudioMediaConfig?

    /**
     * Add current audio to consumed history in database.
     */
    suspend fun addAudioToHistory(audioMediaConfig: AudioMediaConfig)

    /**
     * Set roll through for next audio. Used for analytics
     */
    fun setRollthroughNextAudio(isRollThrough: Boolean = true)

    /**
     * check if the recommended audio was played as rollthrough.
     */
    fun isRecommendedRollthrough() : Boolean

    /**
     * Get the list of audio recommendations.
     */
    suspend fun getAudioRecommendationList(excludeList: List<String>? = null): List<AudioMediaConfig>
}