package com.wapo.flagship.features.ask.auto

import android.content.Context
import androidx.core.text.HtmlCompat
import com.wapo.flagship.auto.ask.AskErrorAudioProvider
import com.wapo.flagship.auto.ask.AskErrorAudioType
import com.wapo.flagship.auto.ask.AskFeaturedQuestion
import com.wapo.flagship.auto.ask.AskFeaturedQuestionsGateway
import com.wapo.flagship.auto.ask.AskSpeechRecognizer
import com.wapo.flagship.auto.ask.AskThePostEvent
import com.wapo.flagship.auto.ask.AskThePostGateway
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.TalkToThePostAudioManager
import com.wapo.flagship.features.ask.domain.SseDataType
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo
import com.wapo.flagship.features.ask.session.AskSessionGuard
import com.wapo.flagship.features.ask.session.AskSessionOwner
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.features.settings.ASK_SAM_JUNIPER_ID
import com.wapo.flagship.util.PrefUtils
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AndroidAutoAskModule {
    @Binds
    @Singleton
    abstract fun bindAskThePostGateway(implementation: TabletAskThePostGateway): AskThePostGateway

    @Binds
    @Singleton
    abstract fun bindFeaturedQuestionsGateway(
        implementation: TabletFeaturedQuestionsGateway,
    ): AskFeaturedQuestionsGateway

    @Binds
    abstract fun bindAskSpeechRecognizer(implementation: CarAskSpeechRecognizer): AskSpeechRecognizer

    @Binds
    @Singleton
    abstract fun bindAskErrorAudioProvider(
        implementation: TabletAskErrorAudioProvider,
    ): AskErrorAudioProvider
}

internal class TabletFeaturedQuestionsGateway
    @Inject
    constructor(
        private val askQuestionsRepo: AskQuestionsRepo,
    ) : AskFeaturedQuestionsGateway {
        override suspend fun loadFeaturedQuestions(): List<AskFeaturedQuestion> {
            val questions =
                when (val result = askQuestionsRepo.getQuestions(skipCache = true)) {
                    is APIResult.Success ->
                        result.data?.questions
                            ?.filterNotNull()
                            .orEmpty()
                            .ifEmpty { askQuestionsRepo.defaultQuestions }
                    else -> askQuestionsRepo.defaultQuestions
                }

            return questions
                .mapNotNull { question ->
                    val text =
                        question.text
                            ?.let {
                                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                                    .toString()
                                    .trim()
                            }.orEmpty()
                    if (text.isEmpty()) {
                        null
                    } else {
                        AskFeaturedQuestion(
                            id = question.uuid.orEmpty(),
                            text = text,
                        )
                    }
                }.distinctBy { it.text }
        }
    }

internal class TabletAskThePostGateway
    @Inject
    constructor(
        private val searchRepo: SearchRepo,
        @ApplicationContext private val context: Context,
    ) : AskThePostGateway {
        override val events: Flow<AskThePostEvent> =
            searchRepo.sseEventState
                .filterNotNull()
                .mapNotNull(::toAndroidAutoEvent)

        override fun tryAcquireSession(): Boolean =
            AskSessionGuard.tryAcquire(AskSessionOwner.ANDROID_AUTO)

        override fun startConversation(question: String) {
            searchRepo.startLiveConversation(
                query = question,
                voiceResponse = selectedVoice(),
                entryPoint = AskSamEntryPoint.LANDING_PAGE.name.lowercase(),
            )
        }

        override fun continueConversation(
            conversationId: String,
            question: String,
        ) {
            searchRepo.continueLiveConversation(
                conversationId = conversationId,
                query = question,
                voiceResponse = selectedVoice(),
                entryPoint = AskSamEntryPoint.LANDING_PAGE.name.lowercase(),
            )
        }

        override suspend fun endConversation() {
            try {
                searchRepo.endLiveConversation()
            } finally {
                AskSessionGuard.release(AskSessionOwner.ANDROID_AUTO)
            }
        }

        private fun selectedVoice(): String =
            PrefUtils.getTalkToThePostVoiceSelection(context) ?: ASK_SAM_JUNIPER_ID

        private fun toAndroidAutoEvent(event: SseEvent): AskThePostEvent? =
            when (event) {
                is SseEvent.Data ->
                    when (event.type?.let(SseDataType::fromString)) {
                        SseDataType.NEW_CONVERSATION_ID ->
                            AskThePostEvent.ConversationStarted(event.data)
                        SseDataType.AUDIO_REPLY -> AskThePostEvent.AudioChunk(event.data)
                        else -> null
                    }
                is SseEvent.Closed -> AskThePostEvent.Closed
                is SseEvent.Error -> AskThePostEvent.Error
                else -> null
            }
    }

internal class TabletAskErrorAudioProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AskErrorAudioProvider {
        private var remainingRecognitionPrompts = mutableListOf<String>()

        override suspend fun loadErrorAudio(type: AskErrorAudioType): ByteArray? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val path =
                        when (type) {
                            AskErrorAudioType.RECOGNITION -> nextRecognitionPromptPath()
                            AskErrorAudioType.RECOGNITION_MAX ->
                                "$voiceBasePath/${TalkToThePostAudioManager.ERROR_MAX_PCM}"
                            AskErrorAudioType.API -> "$voiceBasePath/${TalkToThePostAudioManager.API_FAILS_PCM}"
                        }
                    context.assets.open(path).use { it.readBytes() }
                }.getOrNull()
            }

        @Synchronized
        private fun nextRecognitionPromptPath(): String {
            val directory = "$voiceBasePath/${TalkToThePostAudioManager.ERROR_MESSAGE_POOL}"
            if (remainingRecognitionPrompts.isEmpty()) {
                remainingRecognitionPrompts = context.assets.list(directory).orEmpty().toMutableList()
            }
            val fileName = remainingRecognitionPrompts.randomOrNull()
                ?: error("No Ask the Post recognition error prompts found")
            remainingRecognitionPrompts.remove(fileName)
            return "$directory/$fileName"
        }

        private val voiceBasePath: String
            get() {
                val voiceId =
                    PrefUtils.getTalkToThePostVoiceSelection(context) ?: ASK_SAM_JUNIPER_ID
                return "${TalkToThePostAudioManager.BASE_PATH}/$voiceId"
            }
    }
