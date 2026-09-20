package com.wapo.flagship.features.personalizedpodcasts.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.repo.PersonalizedPodcastRepository
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.personalizedpodcasts.repo.PodcastResult
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.wapo.view.habittiles.ArticleSource
import com.washingtonpost.userhistory.models.ConclusionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.Instant
import javax.inject.Inject

@HiltViewModel
class PersonalizedPodcastViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val personalizedPodcastRepository: PersonalizedPodcastRepository,
    private val audioManager: ClassicAudioManager2,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalizedPodcastUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PersonalizedPodcastEvents>()
    val events = _events.asSharedFlow()

    private val _singlePersonalizedPodcast = MutableLiveData<PodcastResult?>()
    val singlePersonalizedPodcast: LiveData<PodcastResult?> = _singlePersonalizedPodcast

    private var isPlaceholder: Boolean = false
    private var timerJob: Job? = null
    private var generationJob: Job? = null
    private var currentHostVoicePlayer: MediaPlayer? = null
    private var wasMainPlayerPausedForSample: Boolean = false
    private var cachedPodcastSources: List<ArticleSource> = emptyList()

    private val generationSteps = listOf(
        PodcastGenerationState.Initializing,
        PodcastGenerationState.FindingArticles,
        PodcastGenerationState.DraftingScript,
        PodcastGenerationState.LookingOver,
        PodcastGenerationState.Minimizable
    )

    fun getListOfPodcasts() {
        viewModelScope.launch {
            val podcasts = personalizedPodcastRepository.getPodcasts()
            podcasts?.let {
                _uiState.update { currentState ->
                    currentState.copy(personalizedPodcasts = it)
                }
            }
        }
    }

    fun getPodcastMetadata(podcastId: String) {
        viewModelScope.launch {
            _singlePersonalizedPodcast.value = personalizedPodcastRepository.getPodcastMetadata(podcastId)
        }
    }

    fun clearPodcastMetadata() {
        _singlePersonalizedPodcast.value = null
    }

    fun setPersoPodTrackingInfo(persoPodTrackingInfo: PersoPodTrackingInfo) {
        _uiState.update {
            val existingSources = it.persoPodTrackingInfo?.first?.sources
                ?: it.persoPodTrackingInfo?.second?.sources
            val mergedExistingSources = if (!existingSources.isNullOrEmpty()) {
                existingSources
            } else {
                cachedPodcastSources
            }
            val mergedTrackingInfo = if (persoPodTrackingInfo.sources.isNullOrEmpty() && mergedExistingSources.isNotEmpty()) {
                persoPodTrackingInfo.copy(sources = mergedExistingSources.toList())
            } else {
                if (!persoPodTrackingInfo.sources.isNullOrEmpty()) {
                    persoPodTrackingInfo.copy(sources = persoPodTrackingInfo.sources.toList())
                } else {
                persoPodTrackingInfo
                }
            }
            if (!mergedTrackingInfo.sources.isNullOrEmpty()) {
                cachedPodcastSources = mergedTrackingInfo.sources.toList()
            }
            it.copy(persoPodTrackingInfo = Pair(mergedTrackingInfo, null))
        }
    }

    fun cachePodcastSources(sources: List<ArticleSource>?) {
        if (!sources.isNullOrEmpty()) {
            cachedPodcastSources = sources.toList()
        }
    }

    fun getCachedPodcastSources(): List<ArticleSource> {
        return cachedPodcastSources
    }

    fun setPausedPodcast(isPaused: Boolean) {
        _uiState.update {
            it.copy(pausedPodcast = isPaused)
        }
    }

    fun getPodcastConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            val podcastConfigs = personalizedPodcastRepository.getPodcastConfigs()
            podcastConfigs?.let {
                _uiState.update { currentState ->
                    currentState.copy(configMetadata = it)
                }
            }
        }
    }

    fun generatePodcast(selectedItems: SnapshotStateMap<String, Pair<Int, List<String>>>?, savePref: Boolean? = false, canGenerate: Boolean? = true, userCreated: Boolean? = false) {
        val isPlaceholder = isPlaceholder
        this.isPlaceholder = false
        if (userCreated == true && canGenerate == true) {
            audioManager.stopMedia(ConclusionState.OTHER)
            audioManager.playMedia(createPlaceholderConfig())
            _uiState.update {
                it.copy(
                    persoPodTrackingInfo = Pair(
                        PersoPodTrackingInfo(
                            touchpoint = context.getString(R.string.media_player),
                            podcastType = "custom"
                        ), null
                    )
                )
            }
        }
        startGenerationTimer()
        viewModelScope.launch(Dispatchers.IO) {
            val jsonObj = PersonalizedPodcastHelper.createJsonObject(selectedItems ?: mutableStateMapOf())
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonObj.first.toRequestBody(mediaType)
            val userPrefConfig = jsonObj.second.toRequestBody(mediaType)
            if (userCreated == true && canGenerate == true) {
                val date = Instant.now().toString()
                audioManager.audioProvider.trackPersoEvents("perso-custom: $date", context.getString(R.string.media_player), "perso_podcast_generate", jsonObj.first, null)
            }
            if (savePref == true) audioManager.audioProvider.saveUserPersoPodConfig(userPrefConfig)
            generationJob = viewModelScope.launch(Dispatchers.IO) {
                val response =
                    if (canGenerate == true) personalizedPodcastRepository.generatePodcast(body) else null

                if (response == null) {
                    timerJob?.cancel()
                    _uiState.update { it.copy(generationState = PodcastGenerationState.Failure) }
                }

                if (!isActive) return@launch
                response?.let {
                    if (isPlaceholder) playSound()
                    getListOfPodcasts()
                    timerJob?.cancel()
                    _uiState.update { currentState ->
                        currentState.copy(generationState = PodcastGenerationState.Done)
                    }
                    delay(3000L)
                    val currentInfo = _uiState.value.persoPodTrackingInfo?.first
                    val newState =
                        PersoPodTrackingInfo(
                            date = response.createdAt,
                            touchpoint = currentInfo?.touchpoint,
                            podcastType = currentInfo?.podcastType,
                            id = response.id,
                            tags = jsonObj.first,
                            title = response.title,
                            transcriptUrl = response.transcript,
                            sources = response.articlesUsed?.map { article ->
                                ArticleSource(
                                    headline = article.headline,
                                    canonicalUrl = article.canonicalUrl,
                                    publishDate = article.displayDate,
                                    text = article.text,
                                    imageUrl = article.imageUrl
                                )
                            }
                        )

                    withContext(Dispatchers.Main) {
                        if (currentInfo?.podcastType == "intro") {
                            _uiState.update {
                                it.copy(persoPodTrackingInfo = Pair(currentInfo.copy(isRollThrough = true), newState))
                            }
                        } else {
                            _uiState.update {
                                it.copy(persoPodTrackingInfo = Pair(newState, null))
                            }
                        }
                    }
                    val tracker = createPodcastTracker(response.audioDuration?.toLong() ?: 0L, _uiState.value.persoPodTrackingInfo)
                    val newMediaConfig = AudioMediaConfig(
                        mediaId = response.id,
                        playerType = PlayerType.PODCAST,
                        title = response.title,
                        streamUrl = response.audioFilePath,
                        imageUrl = response.image,
                        primaryLabel = response.kicker,
                        audioType = response.itemType ?: "podcast",
                        contentUrl = null,
                        date = toDateLong(response.createdAt),
                        duration = response.audioDuration?.toLong(),
                        sectionName = response.kicker,
                        audioTracking = tracker
                    )
                    if (isPlaceholder) {
                        audioManager.replaceMediaAndPlay(newMediaConfig)
                    } else {
                        audioManager.addSingleMediaToQueue(newMediaConfig)
                    }
                    _uiState.update { it.copy(generationState = PodcastGenerationState.Hidden) }
                }
            }
        }
    }

    fun createPodcastTracker(duration: Long, persoPodTrackingInfo: Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>?): AudioTracker {
        return audioManager.audioProvider.createPodcastTracker(
            duration,
            persoPodTrackingInfo = persoPodTrackingInfo
        )
    }

    fun getPodcastEpisodeId(): String {
        return audioManager.player
            ?.currentMediaItem
            ?.mediaId ?: ""
    }

    private fun createPlaceholderConfig(): AudioMediaConfig {
        return AudioMediaConfig(
            playerType = PlayerType.PODCAST,
            title = "Creating your podcast",
            audioType = PLACEHOLDER,
            primaryLabel = "Your Personal Podcast",
            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/FTNQXJJ6XVBMNKFO3E6YDZBPGM.png",
            mediaId = "personalized_podcast_placeholder_${System.currentTimeMillis()}",
        )
    }

    private fun startGenerationTimer() {
        timerJob?.cancel() // Cancel any previous timer job
        var processingTime = 0L
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            for (state in generationSteps) {
                delay(3000L) // Wait 3 seconds
                processingTime += 3000L
                if (isActive) {
                    if (processingTime >= 15000L) {
                        _uiState.update { it.copy(generationState = PodcastGenerationState.Minimizable) }
                    } else {
                        _uiState.update { it.copy(generationState = state) }
                    }
                }
            }
        }
    }

    private fun playSound() {
        viewModelScope.launch {
            _events.emit(PersonalizedPodcastEvents.PlaySound)
        }
    }

    fun wasMainPlayerPlaying(): Boolean {
        val state = audioManager.nowPlayingAudioItem.value?.audioPlaybackState
        return state is AudioPlaybackState.Playing ||
                state == AudioPlaybackState.Buffering ||
                state == AudioPlaybackState.Connecting
    }

    fun playVoiceSample(sampleUrl: String) {
        stopCurrentPlayback()
        val wasMediaPlayerActive = wasMainPlayerPlaying()

        if (wasMediaPlayerActive) {
            wasMainPlayerPausedForSample = true
            audioManager.pauseMedia()
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(sampleUrl)
                    prepareAsync()
                    setOnPreparedListener { mp -> mp.start() }
                    setOnCompletionListener { mp ->
                        mp.release()
                        if (wasMainPlayerPausedForSample) {
                            audioManager.resumeMedia()
                            wasMainPlayerPausedForSample = false
                        }
                        if (mp == currentHostVoicePlayer) {
                            currentHostVoicePlayer = null
                        }
                    }
                }
                currentHostVoicePlayer = mediaPlayer
            } catch (e: Exception) {
                e.printStackTrace()
                currentHostVoicePlayer = null
                if (wasMainPlayerPausedForSample) {
                    audioManager.resumeMedia()
                    wasMainPlayerPausedForSample = false
                }
            }
        }
    }

    fun stopVoiceSample() {
        stopCurrentPlayback()
        if (wasMainPlayerPausedForSample) {
            audioManager.resumeMedia()
            wasMainPlayerPausedForSample = false
        }
    }

    fun stopCurrentPlayback() {
        currentHostVoicePlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        currentHostVoicePlayer = null
    }

    fun retrieveTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            val transcriptData = personalizedPodcastRepository.fetchTranscript(_uiState.value.persoPodTrackingInfo?.first?.transcriptUrl)
            transcriptData?.let {
                _uiState.update { currentState ->
                    currentState.copy(transcript = it.dialogue)
                }
            }
            audioManager.audioProvider.trackPersoEvents("perso-podcast:${_uiState.value.persoPodTrackingInfo?.first?.date}", context.getString(R.string.media_player),
                context.getString(
                    R.string.perso_podcast_transcript
                ), null, null)
        }
    }

    fun sharePodcast(context: Context, podcastId: String, kicker: String?) {
        viewModelScope.launch {
            audioManager.audioProvider.sharePodcast(context, podcastId)
            val avName = buildPersoPodcastAvName(uiState.value.persoPodTrackingInfo?.first, kicker)
            audioManager.audioProvider.trackPersoShare(avName, context.getString(R.string.perso_podcast), context.getString(R.string.perso_podcast))
        }
    }

    fun trackMenuOpen(kicker: String?) {
        viewModelScope.launch {
            val avName = buildPersoPodcastAvName(uiState.value.persoPodTrackingInfo?.first, kicker)
            audioManager.audioProvider.trackPersoMenuOpen(
                avName,
                context.getString(R.string.perso_podcast),
                context.getString(R.string.perso_podcast),
            )
        }
    }

    private fun buildPersoPodcastAvName(
        trackingInfo: PersoPodTrackingInfo?,
        kicker: String?
    ): String? {
        val podcastType = trackingInfo?.podcastType ?: return null
        val date = trackingInfo.date ?: return null

        return "perso-$podcastType:$kicker:$date"
    }

    fun restorePreviousPlayback() {
        viewModelScope.launch {
            _events.emit(PersonalizedPodcastEvents.RestorePreviousPlayback)
        }
    }

    fun cancelPodcastGeneration() {
        generationJob?.cancel()
        timerJob?.cancel()
        _uiState.update { it.copy(generationState = PodcastGenerationState.Hidden) }
        generationJob = null
        timerJob = null
    }

    fun checkAndAdvanceRollThrough() {
        val (currentInfo, nextInfo) = _uiState.value.persoPodTrackingInfo ?: return
        val playingId = audioManager.nowPlayingAudioItem.value?.audioMediaConfig?.id

        if (currentInfo.isRollThrough == true && currentInfo.id != playingId && nextInfo != null) {
            setPersoPodTrackingInfo(nextInfo)
        }
    }

    fun setIsCurrentPlaceholder(placeholder: Boolean) {
        isPlaceholder = placeholder
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentPlayback()
    }
}

enum class StyleType {
    CHIP, RADIO, SLIDER
}

sealed class PodcastGenerationState(val message: String) {
    data object Initializing : PodcastGenerationState("Generating...")
    data object FindingArticles : PodcastGenerationState("Finding articles for you...")
    data object DraftingScript : PodcastGenerationState("Drafting your custom script...")
    data object LookingOver : PodcastGenerationState("Looking things over...")
    data object Minimizable : PodcastGenerationState("We’ll let you know as soon as it’s ready —\nyou can safely minimize this window.")
    data object Done : PodcastGenerationState("Done. Let’s listen.")
    data object Hidden : PodcastGenerationState("")
    data object Failure : PodcastGenerationState("Something went wrong. Please try again.")
}
