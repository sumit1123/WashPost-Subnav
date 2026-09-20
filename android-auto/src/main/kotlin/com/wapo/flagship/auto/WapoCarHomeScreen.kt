package com.wapo.flagship.auto

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.wapo.flagship.auto.ask.AskErrorAudioProvider
import com.wapo.flagship.auto.ask.AskFeaturedQuestion
import com.wapo.flagship.auto.ask.AskFeaturedQuestionsGateway
import com.wapo.flagship.auto.ask.AskSpeechRecognizer
import com.wapo.flagship.auto.ask.AskThePostGateway
import com.wapo.flagship.features.audio.service2.media.FOR_YOU_SECTION_ID
import com.wapo.flagship.features.audio.service2.media.PODCAST_SECTION_ID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/** Root car screen containing the existing media tabs and Ask the Post. */
@OptIn(ExperimentalCarApi::class, UnstableApi::class)
internal class WapoCarHomeScreen(
    carContext: CarContext,
    askThePostGateway: AskThePostGateway,
    private val featuredQuestionsGateway: AskFeaturedQuestionsGateway,
    speechRecognizer: AskSpeechRecognizer,
    errorAudioProvider: AskErrorAudioProvider,
) : Screen(carContext),
    TabTemplate.TabCallback,
    DefaultLifecycleObserver {
    private val askController =
        AskThePostCarController(
            carContext = carContext,
            askThePostGateway = askThePostGateway,
            speechRecognizer = speechRecognizer,
            errorAudioProvider = errorAudioProvider,
        )
    private val artworkLoader = CarArtworkLoader(carContext)
    private val mediaCoordinator = CarMediaCoordinator(carContext, lifecycleScope)
    private val featuredQuestionIcon =
        CarIcon
            .Builder(
                IconCompat.createWithResource(
                    carContext,
                    com.wpds.wpds.R.drawable.ai_filled,
                ),
            ).setTint(CarColor.DEFAULT)
            .build()
    private val artworkByMediaId = mutableMapOf<String, CarIcon>()
    private val requestedArtworkIds = mutableSetOf<String>()
    private var displayedMediaState = CarMediaState()
    private var featuredQuestions: List<AskFeaturedQuestion> = emptyList()
    private var featuredQuestionsLoaded = false
    private var pendingFeaturedQuestion: String? = null
    private var showFullScreenPermission = false
    private val sections =
        listOf(
            CarTabSection(ASK_THE_POST_SECTION_ID, "Ask the Post"),
            CarTabSection(FOR_YOU_SECTION_ID, "For You"),
            CarTabSection(PODCAST_SECTION_ID, "Podcasts"),
        )

    private var selectedSectionId = ASK_THE_POST_SECTION_ID

    init {
        lifecycle.addObserver(this)
        lifecycleScope.launch {
            askController.state.collect { invalidate() }
        }
        lifecycleScope.launch {
            featuredQuestions =
                runCatching { featuredQuestionsGateway.loadFeaturedQuestions() }
                    .getOrDefault(emptyList())
            featuredQuestionsLoaded = true
            invalidate()
        }
        lifecycleScope.launch {
            mediaCoordinator.state.collect { state ->
                withTimeoutOrNull(ARTWORK_LOAD_TIMEOUT_MS.milliseconds) {
                    loadMediaArtwork(state.itemsBySection.values.flatten())
                }
                displayedMediaState = state
                invalidate()
            }
        }
        mediaCoordinator.connect()
    }

    override fun onGetTemplate(): Template {
        if (displayedMediaState.isLoading) {
            return MessageTemplate
                .Builder(carContext.getString(R.string.ask_the_post_car_loading))
                .setLoading(true)
                .build()
        }

        val activeSection = sections.firstOrNull { it.id == selectedSectionId } ?: sections.first()
        return TabTemplate
            .Builder(this)
            .apply {
                sections.forEach { addTab(it.toTab()) }
                setTabContents(
                    TabContents
                        .Builder(contentFor(activeSection.id))
                        .build(),
                )
            }.setHeaderAction(Action.APP_ICON)
            .setActiveTabContentId(activeSection.id)
            .build()
    }

    override fun onTabSelected(tabContentId: String) {
        if (selectedSectionId == ASK_THE_POST_SECTION_ID && tabContentId != ASK_THE_POST_SECTION_ID) {
            askController.pause()
        }
        selectedSectionId = tabContentId
        invalidate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        askController.destroy()
        mediaCoordinator.release()
    }

    override fun onStop(owner: LifecycleOwner) {
        askController.pause()
    }

    private fun contentFor(sectionId: String): Template =
        if (sectionId == ASK_THE_POST_SECTION_ID) {
            askThePostTemplate()
        } else {
            val mediaState = displayedMediaState
            mediaListTemplate(
                items = mediaState.itemsBySection[sectionId].orEmpty(),
                activeMediaItem = mediaState.activeMediaItem,
            )
        }

    private fun mediaListTemplate(
        items: List<MediaItem>,
        activeMediaItem: MediaItem?,
    ): Template {
        val itemList =
            ItemList
                .Builder()
                .apply {
                    // if there are no items, show a placeholder row that indicates there is no audio available
                    if (items.isEmpty()) {
                        addItem(
                            Row.Builder()
                                .setTitle(carContext.getString(R.string.ask_the_post_car_no_audio))
                                .build()
                        )
                    } else {
                        // for each media item, add a row to the list with the title, subtitle, and artwork (if available)
                        items.forEach { item ->
                            addItem(
                                Row
                                    .Builder()
                                    .setTitle(item.mediaMetadata.title ?: "")
                                    .apply {
                                        item.mediaMetadata.subtitle?.let { addText(it) }
                                        artworkByMediaId[item.mediaId]?.let { artwork ->
                                            setImage(artwork, Row.IMAGE_TYPE_LARGE)
                                        }
                                    }.setOnClickListener { play(item) }
                                    .build(),
                            )
                        }
                    }
                }.build()
        return ListTemplate
            .Builder()
            .setSingleList(itemList)
            .apply {
                if (activeMediaItem != null) {
                    addAction(nowPlayingAction())
                }
            }
            .build()
    }

    private fun nowPlayingAction(): Action =
        Action
            .Builder()
            .setIcon(
                CarIcon
                    .Builder(
                        IconCompat.createWithResource(
                            carContext,
                            com.wpds.wpds.R.drawable.soundwave,
                        ),
                    )
                    .build(),
            ).setBackgroundColor(CarColor.BLUE)
            .setOnClickListener(::showNowPlaying)
            .build()

    private suspend fun loadMediaArtwork(items: List<MediaItem>) {
        val pendingItems =
            items.filter { item ->
                item.mediaMetadata.artworkUri != null &&
                        item.mediaId !in artworkByMediaId &&
                        requestedArtworkIds.add(item.mediaId)
            }
        if (pendingItems.isEmpty()) return

        // load artwork for each media item and store it in the artworkByMediaId map so it can be displayed in the list template
        coroutineScope {
            val downloads = pendingItems
                .groupBy { it.mediaMetadata.artworkUri }
                .mapNotNull { (artworkUri, mediaItems) ->
                    if (artworkUri == null) null
                    else async {
                        val artwork = artworkLoader.load(artworkUri)
                        if (artwork != null) {
                            mediaItems.forEach { item ->
                                artworkByMediaId[item.mediaId] = artwork
                            }
                        }
                    }
                }

            downloads.awaitAll()
        }
    }

    private fun askThePostTemplate(): Template {
        // if the user has not granted microphone permission, show a full screen permission request before showing the ask the post template
        if (showFullScreenPermission && !hasMicrophonePermission()) {
            return microphonePermissionTemplate()
        }

        val state = askController.state.value

        if (state == AskThePostCarState.IDLE) return askThePostEntryTemplate()
        if (!hasMicrophonePermission()) return microphonePermissionTemplate()

        val builder = MessageTemplate.Builder(carContext.getString(state.messageResource()))
        when (state) {
            AskThePostCarState.LISTENING -> Unit

            AskThePostCarState.ERROR -> {
                builder.addAction(retryAction())
                builder.addAction(endAction())
            }

            AskThePostCarState.ERROR_SPEAKING -> builder.addAction(endAction())

            else -> {
                builder.addAction(pauseAction(state))
                builder.addAction(endAction())
            }
        }
        return builder.build()
    }

    // UI for initial entry into Ask The Post Tab before conversation starts
    private fun askThePostEntryTemplate(): Template {
        if (!featuredQuestionsLoaded) {
            return ListTemplate
                .Builder()
                .setLoading(true)
                .apply {
                    if (displayedMediaState.activeMediaItem != null) {
                        addAction(nowPlayingAction())
                    }
                }
                .build()
        }

        val itemListBuilder = ItemList.Builder()
        val askRow =
            Row
                .Builder()
                .setTitle(carContext.getString(R.string.ask_the_post_car_start))
                .setOnClickListener {
                    if (hasMicrophonePermission()) {
                        askController.start()
                    } else {
                        showFullScreenPermission = true
                        invalidate()
                    }
                }
                .build()
        itemListBuilder.addItem(askRow)

        val questionLimit =
            (
                carContext
                    .getCarService(ConstraintManager::class.java)
                    .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) - 1
            ).coerceAtLeast(0)
        featuredQuestions.take(questionLimit).forEach { question ->
            itemListBuilder.addItem(
                Row
                    .Builder()
                    .setTitle(question.text)
                    .setImage(featuredQuestionIcon, Row.IMAGE_TYPE_SMALL)
                    .setBrowsable(true)
                    .setOnClickListener { startFeaturedQuestion(question.text) }
                    .build(),
            )
        }

        return ListTemplate
            .Builder()
            .setSingleList(itemListBuilder.build())
            .apply {
                if (displayedMediaState.activeMediaItem != null) {
                    addAction(nowPlayingAction())
                }
            }
            .build()
    }

    private fun startFeaturedQuestion(question: String) {
        if (hasMicrophonePermission()) {
            askController.startWithQuestion(question)
        } else {
            pendingFeaturedQuestion = question
            showFullScreenPermission = true
            invalidate()
        }
    }

    // UI for user to grant microphone permission before starting Ask The Post conversation
    private fun microphonePermissionTemplate(): Template =
        MessageTemplate
            .Builder(carContext.getString(R.string.ask_the_post_car_permission))
            .addAction(
                Action
                    .Builder()
                    .setTitle(carContext.getString(R.string.ask_the_post_car_grant_permission))
                    .setOnClickListener(
                        ParkedOnlyOnClickListener.create {
                            carContext.requestPermissions(listOf(Manifest.permission.RECORD_AUDIO)) { granted, _ ->
                                showFullScreenPermission = false
                                val question = pendingFeaturedQuestion
                                pendingFeaturedQuestion = null

                                if (Manifest.permission.RECORD_AUDIO in granted) {
                                    if (question == null) {
                                        askController.start()
                                    } else {
                                        askController.startWithQuestion(question)
                                    }
                                } else {
                                    invalidate()
                                }
                            }
                        }
                    ).build(),
            ).build()

    private fun pauseAction(state: AskThePostCarState): Action =
        Action
            .Builder()
            .setTitle(
                carContext.getString(
                    if (state == AskThePostCarState.PAUSED) {
                        R.string.ask_the_post_car_resume
                    } else {
                        R.string.ask_the_post_car_pause
                    },
                ),
            ).setOnClickListener(askController::togglePause)
            .build()

    private fun retryAction(): Action =
        Action
            .Builder()
            .setTitle(carContext.getString(R.string.ask_the_post_car_retry))
            .setOnClickListener(askController::retry)
            .build()

    private fun endAction(): Action =
        Action
            .Builder()
            .setTitle(carContext.getString(R.string.ask_the_post_car_end))
            .setOnClickListener(askController::end)
            .build()

    private fun play(item: MediaItem) {
        lifecycleScope.launch {
            if (mediaCoordinator.play(item)) {
                showNowPlaying()
            }
        }
    }

    private fun showNowPlaying() {
        carContext
            .getCarService(ScreenManager::class.java)
            .push(WapoMediaPlaybackScreen(carContext, mediaCoordinator))
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(carContext, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun AskThePostCarState.messageResource(): Int =
        when (this) {
            AskThePostCarState.IDLE -> R.string.ask_the_post_car_ready
            AskThePostCarState.LISTENING -> R.string.ask_the_post_car_listening
            AskThePostCarState.THINKING -> R.string.ask_the_post_car_thinking
            AskThePostCarState.ANSWERING, AskThePostCarState.RECOGNITION_ERROR_SPEAKING -> R.string.ask_the_post_car_answering
            AskThePostCarState.PAUSED -> R.string.ask_the_post_car_paused
            AskThePostCarState.ERROR_SPEAKING, AskThePostCarState.ERROR -> R.string.ask_the_post_car_error
        }

    private fun CarTabSection.toTab(): Tab {
        val iconResource =
            when (id) {
                ASK_THE_POST_SECTION_ID -> R.drawable.ic_ask_the_post
                PODCAST_SECTION_ID -> R.drawable.ic_podcasts
                FOR_YOU_SECTION_ID -> R.drawable.ic_podcasts
                else -> R.drawable.ic_podcasts
            }
        val carIcon =
            CarIcon
                .Builder(IconCompat.createWithResource(carContext, iconResource))
                .build()
        return Tab
            .Builder()
            .setContentId(id)
            .setTitle(title)
            .setIcon(carIcon)
            .build()
    }

    private data class CarTabSection(
        val id: String,
        val title: String,
    )

    private companion object {
        const val ASK_THE_POST_SECTION_ID = "ask_the_post"
        const val ARTWORK_LOAD_TIMEOUT_MS = 5_000L
    }
}
