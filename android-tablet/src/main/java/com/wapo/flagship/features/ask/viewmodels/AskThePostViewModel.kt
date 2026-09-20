package com.wapo.flagship.features.ask.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.AppContext
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.aixp.models.AskThePostShareTurnRequest
import com.wapo.flagship.features.aixp.models.ConversationTurn
import com.wapo.flagship.features.aixp.network.APIResult.Failure
import com.wapo.flagship.features.aixp.network.APIResult.NetworkError
import com.wapo.flagship.features.aixp.network.APIResult.Success
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.ask.domain.ConversationRole
import com.wapo.flagship.features.ask.domain.SseDataType
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.AskThePostUIState
import com.wapo.flagship.features.ask.models.PostIterableBannerEvent
import com.wapo.flagship.features.ask.models.ShareType
import com.wapo.flagship.features.ask.repo.AskThePostRepo
import com.wapo.flagship.features.ask.session.AskSessionGuard
import com.wapo.flagship.features.ask.session.AskSessionOwner
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.PostAnswerCarousel
import com.wapo.flagship.features.search2.model.PostAnswerFeedback
import com.wapo.flagship.features.search2.model.PostAnswerResponse
import com.wapo.flagship.features.search2.model.PostAnswerText
import com.wapo.flagship.features.search2.state.PostAnswersUIState
import com.wapo.flagship.features.search2.ui.CarouselSubtype
import com.wapo.flagship.features.search2.ui.CarouselUIItem
import com.wapo.flagship.features.search2.ui.MimeType
import com.wapo.flagship.features.search2.ui.PostAnswerUIItem
import com.wapo.flagship.features.search2.ui.SelectedPassageInfo
import com.wapo.flagship.features.search2.ui.TextSubtype
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.Measurement.ASK_THE_POST_DEEPLINK
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "AskThePostViewModel"

@HiltViewModel
class AskThePostViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val searchRepo: SearchRepo,
    private val askThePostRepo: AskThePostRepo,
    private val remoteLog: RemoteLogRepo,
    private val loadRenderMetrics: LoadRenderMetrics,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(AskThePostUIState())
    val uiState: StateFlow<AskThePostUIState> = _uiState.asStateFlow()

    private val _askThePostEvent: MutableSharedFlow<AskThePostEvent> = MutableSharedFlow(replay = 0)
    val askThePostEvent: SharedFlow<AskThePostEvent> = _askThePostEvent
    var askSamEntryPoint: AskSamEntryPoint? = null
    var analyticsData = Pair<Question?, TrackingInfo?>(null, null)

    var feedbackData = Triple("", "", 1)

    var tabName: String? = null

    //needed to build the text response and append the latest event to current text
    private var accumulatedText = StringBuilder()

    var clearRun: Boolean = false
    var forceStop: Boolean = false

    //used to determine what response the user clicks on for sources or copy
    private var turnId: String? = null

    fun eventTrigger(event: UserEvent) {
        viewModelScope.launch {
            _askThePostEvent.emit(AskThePostEvent.UserEvent(event))
        }
    }

    var talkToThePostActive = false


    init {
        startCollectingSseEvents()
        setBottomSheetTabs()
    }

    private fun getSharedLinks() {
        viewModelScope.launch {
            val response = askThePostRepo.getShares()
            when (response) {
                is Failure -> {
                    EventLog
                        .Builder()
                        .apply {
                            setErrorMessage(response.getMessage())
                            setModule(LogModules.ATP_SHARE)
                            setMessage("fetching shared links failed [type=${response::class.simpleName}]")
                        }.run {
                            remoteLog.e(
                                build()
                            )
                        }
                }

                is NetworkError -> {

                }

                is Success -> {
                    response.data?.let { list ->
                        _uiState.update {
                            it.copy(
                                sharedLinks = list
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setBottomSheetTabs() {
        val history = context.getString(
            R.string.history
        )
        val sharedLinks = context.getString(
            R.string.shared_links
        )
        val categoryList = listOf(history, sharedLinks)
        _uiState.update {
            it.copy(
                historyCategories = categoryList
            )
        }
    }

    fun startCollectingSseEvents() {
        viewModelScope.launch {
            searchRepo.sseEventState.takeWhile { !talkToThePostActive }.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is SseEvent.Data -> {
                            when (state.type?.let { SseDataType.fromString(it) }) {
                                SseDataType.NEW_CONVERSATION_ID -> {
                                    _uiState.update {
                                        it.copy(
                                            conversationId = state.data
                                        )
                                    }
                                }

                                SseDataType.SYSTEM_MESSAGE -> {
                                    loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.AskQuestionRenderEvent)
                                    _uiState.update {
                                        it.copy(
                                            searchQueryResponse = context.getString(
                                                com.wapo.view.R.string.ai_overview_loading
                                            )
                                        )
                                    }
                                }

                                SseDataType.REPLY -> {
                                    _uiState.update {
                                        it.copy(
                                            isStreamingResponseLoading = false
                                        )
                                    }
                                    val currentList = uiState.value.conversationHistory
                                    val lastItem = currentList.lastOrNull()
                                    val updatedList: List<ConversationItem>

                                    if (lastItem is ConversationItem.ResponseItem) {
                                        val mutableList = currentList.toMutableList()

                                        // Getting the updated text by combining the last item's text and the new data
                                        val updatedText = lastItem.text + state.data

                                        // Replace the last item in the mutable copy
                                        mutableList[mutableList.lastIndex] =
                                            ConversationItem.ResponseItem(updatedText, turnId ?: "")

                                        // Convert back to an immutable list for posting
                                        updatedList = mutableList.toList()
                                    } else {
                                        updatedList =
                                            currentList + ConversationItem.ResponseItem(
                                                state.data, turnId ?: ""
                                            )
                                    }
                                    saveUpdateConversationHistory(updatedList)
                                }

                                SseDataType.SOURCES -> {
                                    val parsedSources =
                                        searchRepo.parseArticleSources(state.data)
                                    val currentList = uiState.value.conversationHistory

                                    val updatedList =
                                        currentList + ConversationItem.Sources(
                                            parsedSources,
                                            turnId ?: ""
                                        )
                                    saveUpdateConversationHistory(updatedList)
                                }

                                SseDataType.TURN_ID -> {
                                    turnId = state.data
                                }

                                else -> {}
                            }
                        }

                        is SseEvent.Open -> {
                            _uiState.update {
                                it.copy(
                                    searchQueryResponse = ""
                                )
                            }
                            // delay to allow time for UI to update placement of question before response placeholder
                            delay(500)
                            _uiState.update {
                                it.copy(
                                    isStreamingResponseLoading = true,
                                    connectionClosed = false
                                )
                            }
                        }

                        is SseEvent.Closed -> {
                            _uiState.update {
                                it.copy(
                                    isStreamingResponseLoading = false,
                                    connectionClosed = true
                                )
                            }
                            Measurement.trackConversationResponse(turnId, analyticsData.second)
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AskQuestionRenderEvent)
                        }

                        is SseEvent.ForceStop -> {
                            forceStop = true
                            if (clearRun) {
                                _uiState.update {
                                    it.copy(
                                        conversationHistory = mutableListOf()
                                    )
                                }
                            }
                            _uiState.update {
                                it.copy(
                                    isStreamingResponseLoading = false,
                                    connectionClosed = true
                                )
                            }
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AskQuestionRenderEvent)
                        }

                        is SseEvent.Error -> {
                            _uiState.update {
                                it.copy(
                                    isStreamingResponseLoading = false,
                                    connectionClosed = true,
                                    searchQueryResponse = ""
                                )
                            }

                            if (!forceStop) {
                                val currentList = uiState.value.conversationHistory

                                val updatedList =
                                    currentList + ConversationItem.ErrorItem(
                                        context.getString(
                                            R.string.error_response
                                        )
                                    )
                                saveUpdateConversationHistory(updatedList)
                                logPostAnswerError(
                                    "SSE Error: ${state.response?.code}",
                                    "",
                                    0L,
                                    "post-answers",
                                    state.response?.message ?: ""
                                )
                            }
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AskQuestionRenderEvent)
                        }

                        else -> {
                            Logger.d(TAG, "data from sse: State = $state ")
                        }
                    }

                    _askThePostEvent.emit(AskThePostEvent.AskThePostReady)
                }
            }
        }
    }


    fun startLiveConversation(
        query: String,
        questionClicked: Boolean? = false,
        tab: String? = null,
        type: String? = null
    ) {
        forceStop = false
        clearRun = false
        accumulatedText.clear()
        _uiState.update {
            it.copy(
                postAnswersUIState = PostAnswersUIState.Loading
            )
        }
        val currentHistory = uiState.value.conversationHistory

        val newList = currentHistory + ConversationItem.QuestionItem(query)
        _uiState.update {
            it.copy(
                conversationHistory = newList
            )
        }

        val conversationId = uiState.value.conversationId
        val incomingShareId = uiState.value.incomingShareId
        if (incomingShareId != null) {
            incomingShareId.let {
                searchRepo.continueLiveConversation(
                    null,
                    query,
                    uiState.value.isPrivateModeEnabled,
                    entryPoint = askSamEntryPoint?.name?.lowercase(),
                    shareId = incomingShareId
                )
            }
            resetShareState()
        } else if (uiState.value.conversationId != null) {
            conversationId?.let {
                searchRepo.continueLiveConversation(
                    it,
                    query,
                    uiState.value.isPrivateModeEnabled,
                    entryPoint = askSamEntryPoint?.name?.lowercase(),
                    shareId = null
                )
            }
            Measurement.trackFollowUpQuestion(turnId ?: conversationId)
        } else {
            searchRepo.startLiveConversation(
                query,
                uiState.value.isPrivateModeEnabled,
                entryPoint = askSamEntryPoint?.name?.lowercase()
            )
            if (questionClicked != true) {
                Measurement.trackEnteredAskQuestionEvent(query, type, analyticsData.second)
            } else {
                Measurement.trackAskThePostSuggestedQuestionClicked(tab, type, analyticsData.second)
            }
        }
    }

    fun setIsPrivateModeRequested(isRequested: Boolean) {
        _uiState.update {
            it.copy(
                isPrivateModeRequested = isRequested
            )
        }
    }

    fun openConversation(setPrivateMode: Boolean?, conversationId: String, hideOrShowResponse: Boolean, showHistory: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPrivateModeEnabled = setPrivateMode ?: it.isPrivateModeEnabled,
                    showResponse = hideOrShowResponse,
                    showHistory = showHistory
                )
            }
            openConversationWithId(
                conversationId
            )
            getSharedLinks()
            if (!showHistory) {
                Measurement.resetAskThePostOrigination()
            }
            if (setPrivateMode != null) {
                Measurement.trackAnonymousMode(setPrivateMode)
            }
        }
    }

    fun setPrivateMode(isEnabled: Boolean) {
        _uiState.update {
            it.copy(
                isPrivateModeEnabled = isEnabled
            )
        }
        Measurement.trackAnonymousMode(isEnabled)
    }

    fun togglePrivateMode() {
        _uiState.update {
            it.copy(
                isPrivateModeRequested = true
            )
        }
    }

    fun getSessionHistory() {
        viewModelScope.launch {
            val response =
                askThePostRepo.getSessionHistory(AppContext.getAirshipNamedUserId(context))
            when (response) {
                is Failure -> {
                    _uiState.update {
                        it.copy(
                            historyList = emptyList()
                        )
                    }
                }

                is NetworkError -> {
                }

                is Success -> {
                    val historyItems = mutableListOf<ConversationItem.HistoryItem>()
                    if (response.data?.isEmpty() == true) {
                        _uiState.update {
                            it.copy(
                                historyList = emptyList()
                            )
                        }
                        return@launch
                    }
                    response.data?.forEach { historyResponse ->
                        val conversationItem = ConversationItem.HistoryItem(
                            historyResponse
                        )
                        historyItems.add(conversationItem)
                        _uiState.update {
                            it.copy(
                                historyList = historyItems
                            )
                        }
                    }
                }
            }
        }
    }

    fun openConversationWithId(conversationId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    continuingConversation = true,
                    conversationId = conversationId
                )
            }
            resetShareState()
            val response = askThePostRepo.getConversationHistory(
                conversationId,
                AppContext.getAirshipNamedUserId(context)
            )
            when (response) {
                is Success -> {
                    response.data?.let {
                        handleConversationResponse(it, conversationId)
                        Measurement.trackOpenPreviousConversation(conversationId)
                    }
                }

                is Failure -> {}
                is NetworkError -> {}
            }
        }
    }

    private fun handleConversationResponse(
        response: List<ConversationTurn>?,
        conversationId: String?
    ) {
        clearRun = false
        if (conversationId == null || response == null) return
        val currentHistory = mutableListOf<ConversationItem>()
        response.forEach {
            when (ConversationRole.fromString(it.role)) {
                ConversationRole.USER -> {
                    currentHistory.add(
                        ConversationItem.QuestionItem(
                            it.message,
                            "${conversationId}_${it.turnId}"
                        )
                    )
                    turnId = "${conversationId}_${it.turnId}"
                }

                ConversationRole.ASSISTANT -> {
                    currentHistory.add(
                        ConversationItem.ResponseItem(
                            it.message,
                            "${conversationId}_${it.turnId}"
                        )
                    )
                    val sources = it.sources
                    turnId = "${conversationId}_${it.turnId}"
                    if (sources != null) {
                        currentHistory.add(
                            ConversationItem.Sources(
                                sources,
                                "${conversationId}_${it.turnId}"
                            )
                        )
                    }
                }

                else -> {}
            }
        }
        saveUpdateConversationHistory(currentHistory)
    }

    suspend fun deleteConversation(conversationId: String) {
        val response = askThePostRepo.deleteConversation(
            conversationId,
            AppContext.getAirshipNamedUserId(context)
        )
        when (response) {
            is Success -> {
                getSessionHistory()
                Measurement.trackDeleteChat(conversationId)
            }

            is Failure -> {
            }

            is NetworkError -> {
            }
        }
    }

    suspend fun endLiveConversation() {
        if (!AskSessionGuard.isOwnedBy(AskSessionOwner.ANDROID_AUTO)) {
            searchRepo.endLiveConversation()
        }
    }

    private fun processPostAnswerResult(
        query: String,
        responseTimeMillis: Long,
        postAnswerResponse: APIResult<PostAnswerResponse>,
        entryPoint: String
    ): PostAnswersUIState =
        when (postAnswerResponse) {
            is APIResult.Success -> {
                val data = postAnswerResponse.data
                if (data == null || data.items.isNullOrEmpty()) {
                    logPostAnswerError(
                        "Null or Empty Data",
                        query,
                        responseTimeMillis,
                        entryPoint,
                        postAnswerResponse.getMessage()
                    )
                    PostAnswersUIState.Error
                } else {
                    Logger.d("Articles2ViewModel", "Success")
                    logATPMetrics(query, responseTimeMillis, entryPoint)
                    val uiItems: ArrayList<PostAnswerUIItem> = arrayListOf()
                    postAnswerResponse.data.items?.forEach { postAnswerItemResponse ->
                        when (postAnswerItemResponse) {
                            is PostAnswerText -> {
                                uiItems.add(
                                    PostAnswerUIItem.TextItem(
                                        subtype = TextSubtype.entries.find { it.value == postAnswerItemResponse.subtype },
                                        content = postAnswerItemResponse.content.orEmpty(),
                                        mimeType = MimeType.entries.find { it.value == postAnswerItemResponse.mime },
                                        streamingUrl = postAnswerItemResponse.streamingUrl,
                                        icon = postAnswerItemResponse.icon
                                    )
                                )
                            }

                            is PostAnswerCarousel -> {
                                val carouselItems =
                                    postAnswerItemResponse.items?.map { carouselItem ->
                                        CarouselUIItem(
                                            id = carouselItem.id,
                                            url = carouselItem.url.orEmpty(),
                                            publishDateMillis = carouselItem.publishDate ?: 0,
                                            content = carouselItem.content.orEmpty(),
                                            imageUrl = carouselItem.image.orEmpty(),
                                            passages = carouselItem.passages.orEmpty()
                                        )
                                    }
                                uiItems.add(
                                    PostAnswerUIItem.Carousel(
                                        subtype = CarouselSubtype.entries.find { it.value == postAnswerItemResponse.subtype },
                                        items = carouselItems.orEmpty()
                                    )
                                )
                            }

                            is PostAnswerFeedback -> {
                                if (!postAnswerItemResponse.endpoint.isNullOrEmpty() &&
                                    !postAnswerItemResponse.responseId.isNullOrEmpty()
                                ) {
                                    uiItems.add(
                                        PostAnswerUIItem.Feedback(
                                            endPointUrl = postAnswerItemResponse.endpoint,
                                            responseId = postAnswerItemResponse.responseId
                                        )
                                    )
                                    feedbackData = Triple(
                                        postAnswerItemResponse.endpoint,
                                        postAnswerItemResponse.responseId,
                                        1
                                    )
                                }
                            }

                            else -> {}
                        }
                    }

                    uiItems.let { PostAnswersUIState.Success(uiItems) }
                }
            }

            is APIResult.Failure -> {
                logPostAnswerError(
                    "Failure",
                    query,
                    responseTimeMillis,
                    entryPoint,
                    postAnswerResponse.getMessage()
                )
                PostAnswersUIState.Error
            }

            is APIResult.NetworkError -> {
                PostAnswersUIState.Error
            }
        }

    private fun logATPMetrics(query: String, responseTimeMillis: Long, entryPoint: String) {
        val responseTimeSeconds = responseTimeMillis / 1000f
        EventLog
            .Builder()
            .apply {
                setModule(LogModules.POST_ANSWERS)
                set(QUERY, query)
                set(RESPONSE_TIME_SECONDS, responseTimeSeconds)
                set(APP_NAME, entryPoint)
            }.run {
                remoteLog.m(build())
            }
    }

    private fun logPostAnswerError(
        errorType: String,
        query: String,
        responseTimeMillis: Long,
        entryPoint: String,
        responseMessage: String
    ) {
        val responseTimeSeconds = responseTimeMillis / 1000f
        EventLog
            .Builder()
            .apply {
                setModule(LogModules.POST_ANSWERS)
                set(QUERY, query)
                set(RESPONSE_TIME_SECONDS, responseTimeSeconds)
                set(ERROR_TYPE, errorType)
                set(APP_NAME, entryPoint)
                setErrorMessage(responseMessage)
            }.run {
                remoteLog.e(build())
            }
        Logger.e(
            "Articles2ViewModel",
            "QUERY= $query RESPONSE_TIME_SECONDS= $responseTimeSeconds ERROR_TYPE= $errorType err_msg= $responseMessage"
        )
    }

    fun clearLiveConversationData() {
        clearRun = true
        accumulatedText = StringBuilder()
        _uiState.update {
            it.copy(
                shareUrlState = ShareUrlState.Empty,
                incomingShareId = null,
                searchQueryResponse = "",
                conversationId = null,
                conversationHistory = mutableListOf()
            )
        }
        viewModelScope.launch {
            endLiveConversation()
        }
    }

    fun saveUpdateConversationHistory(data: List<ConversationItem>) {
        if (!clearRun) {
            _uiState.update {
                it.copy(
                    conversationHistory = data
                )
            }
        } else {
            clearRun = false
        }
    }

    fun initiatePostAnswersFeedback(
        endpoint: String?,
        responseId: String?,
        reaction: PostAnswersFeedbackReaction,
    ) {
        endpoint ?: return
        responseId ?: return
        viewModelScope.launch {
            _askThePostEvent.emit(
                AskThePostEvent.InitiatePostAnswersFeedbackEvent(
                    Triple(
                        endpoint,
                        responseId,
                        reaction.ordinal
                    )
                )
            )
        }
    }

    fun hideOrShowResponse(show: Boolean) {
        _uiState.update {
            it.copy(
                showResponse = show
            )
        }
        if (!show) {
            Measurement.resetAskThePostOrigination()
        }
    }

    fun showHistory(show: Boolean) {
        getSharedLinks()
        _uiState.update {
            it.copy(
                showHistory = show
            )
        }
    }

    fun hideOrShowPassage(show: Boolean) {
        _uiState.update {
            it.copy(
                showPassage = show
            )
        }
    }

    fun showSourceSheetWith(items: List<CarouselUIItem>) {
        _uiState.update {
            it.copy(
                sourceSheetItems = items
            )
        }
    }

    fun seePassagesClicked() {
        _uiState.update {
            it.copy(
                showPassage = (it.showPassage == true).not()
            )
        }
    }

    fun onDoneLoadingFullConversation() {
        _uiState.update {
            it.copy(
                continuingConversation = false
            )
        }
    }

    fun setPassageData(data: SelectedPassageInfo?) {
        _uiState.update {
            it.copy(
                selectedPassageInfo = data
            )
        }
    }

    fun setCitationData(data: SelectedPassageInfo?) {
        _uiState.update {
            it.copy(
                storedCitationInfo = data
            )
        }
    }

    fun seeCitationsClicked() {
        _uiState.update {
            it.copy(
                showCitation = it.showCitation.not()
            )
        }
    }

    fun readMoreArticleClicked() {
        tabName?.let {
            Measurement.setAskThePostOrigination(it)
        } ?: run {
            Measurement.setAskThePostOrigination("tab")
        }
    }

    /**
     * Share a conversation or turn.
     */
    fun shareATP(isTurn: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    shareUrlState = ShareUrlState.Loading
                )
            }
            _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Loading))
            uiState.value.conversationId?.let {
                val shareTurnRequest =
                    if (isTurn && turnId != null) AskThePostShareTurnRequest(turnId = turnId) else AskThePostShareTurnRequest(
                        turnId = null
                    )
                val response =
                    askThePostRepo.shareChat(it, shareTurnRequest)
                when (response) {
                    is Success -> {
                        if (!response.data.isNullOrEmpty()) {
                            _uiState.update { state ->
                                state.copy(
                                    shareUrlState = ShareUrlState.SuccessShare(
                                        response.data!!
                                    )
                                )
                            }
                            _askThePostEvent.emit(
                                AskThePostEvent.ShareUrlStateEvent(
                                    ShareUrlState.SuccessShare(
                                        response.data!!
                                    )
                                )
                            )
                            Measurement.trackSuccessfulShare(it, turnId)
                        } else {
                            _uiState.update { state ->
                                state.copy(
                                    shareUrlState = ShareUrlState.Error
                                )
                            }
                            _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Error))

                        }
                    }

                    else -> {
                        _uiState.update { state ->
                            state.copy(
                                shareUrlState = ShareUrlState.Error
                            )
                        }
                        _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Error))
                    }
                }
            } ?: run {
                _uiState.update { state ->
                    state.copy(
                        shareUrlState = ShareUrlState.Error
                    )
                }
                _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Error))
            }
        }
    }

    fun processDeepLink(shareId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    incomingShareId = shareId,
                    shareUrlState = ShareUrlState.Processing(
                        shareId
                    )
                )
            }
            _askThePostEvent.emit(
                AskThePostEvent.ShareUrlStateEvent(
                    ShareUrlState.Processing(
                        shareId
                    )
                )
            )
        }
    }

    fun handleDeepLink(shareId: String) {
        viewModelScope.launch {
            when (val response = askThePostRepo.getSharedConversation(shareId)) {
                is Failure -> {
                    if (response.statusCode == 404) {
                        _uiState.update { state ->
                            state.copy(
                                shareUrlState = ShareUrlState.ShareExpired
                            )
                        }
                        _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.ShareExpired))
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                shareUrlState = ShareUrlState.Error
                            )
                        }
                        _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Error))
                        EventLog
                            .Builder()
                            .apply {
                                setErrorMessage(response.getMessage())
                                setModule(LogModules.ATP_SHARE)
                                setMessage("fetching conversation failed")
                            }.run {
                                remoteLog.e(
                                    build()
                                )
                            }
                    }
                }

                is NetworkError -> {
                    _uiState.update { state ->
                        state.copy(
                            shareUrlState = ShareUrlState.Error
                        )
                    }
                    _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Error))
                }

                is Success -> {
                    response.data?.let {
                        clearLiveConversationData()
                        _uiState.update { state ->
                            state.copy(
                                conversationId = it.conversationId,
                                shareUrlState = ShareUrlState.ShareProcessed,
                                incomingShareId = shareId
                            )
                        }
                        hideOrShowResponse(true)
                        Measurement.trackATPCloseProfileInteraction(ASK_THE_POST_DEEPLINK)
                        handleConversationResponse(it.displayChat, it.conversationId)
                        _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.ShareProcessed))
                    }
                }
            }
        }
    }

    fun resetShareState() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    incomingShareId = null,
                    shareUrlState = ShareUrlState.Empty
                )
            }
            _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Empty))
        }
    }

    fun screenShotTaken() {
        uiState.value.conversationId?.let {
            Measurement.trackShareScreenShotTaken(it, turnId)
        }
        viewModelScope.launch {
            _askThePostEvent.emit(AskThePostEvent.ScreenShotTaken(true))
            _uiState.update {
                it.copy(
                    screenShotTaken = true
                )
            }
        }
    }

    fun deleteShareLinks(shareId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                shareId?.let {
                    val response = askThePostRepo.deleteShareLink(it).execute()
                    if (response.isSuccessful) {
                        getSharedLinks()
                        Measurement.trackUserDeletesLink(it)
                    }
                } ?: run {
                    val response = askThePostRepo.deleteAllShareLinks().execute()
                    if (response.isSuccessful) {
                        getSharedLinks()
                    }
                }
            } catch (e: Exception) {
                EventLog
                    .Builder()
                    .apply {
                        setErrorMessage(e.message)
                        setModule(LogModules.ATP_SHARE)
                        setMessage("deleting shares failed")
                    }.run {
                        remoteLog.e(
                            build()
                        )
                    }
            }
        }
    }

    fun copyShareLink(shareId: String) {
        viewModelScope.launch {
            _askThePostEvent.emit(AskThePostEvent.ShareUrlStateEvent(ShareUrlState.Copy(shareId)))
        }
    }

    fun showBanner(show: Boolean) {
        _uiState.update {
            it.copy(
                showBanner = show
            )
        }
    }

    fun setPostIterableBannerEvent(postIterableBannerEvent: PostIterableBannerEvent?) {
        _uiState.update {
            it.copy(
                postIterableBannerEvent = postIterableBannerEvent
            )
        }
    }

    fun setATPCTABanner(askThePostMessage: BannerPaywallMessage?) {
        _uiState.update {
            it.copy(
                askThePostMessage = askThePostMessage,
                showBanner = askThePostMessage != null
            )
        }
    }

    fun showRegisterView(showRegisterInHistoryView: Boolean) {
        _uiState.update {
            it.copy(
                showRegisterInHistoryView = showRegisterInHistoryView
            )
        }
        isUserLoggedIn(!showRegisterInHistoryView)
    }

    fun isUserLoggedIn(isLoggedIn: Boolean) {
        _uiState.update {
            it.copy(
                isUserLoggedIn = isLoggedIn
            )
        }
    }

    fun showRegisterModal(show: Boolean) {
        _uiState.update {
            it.copy(
                showRegisterSaveChatModal = show
            )
        }
    }

    fun showRegisterBeforeShareModal(
        shareType: ShareType?,
        iterableBannerEvent: PostIterableBannerEvent? = null
    ) {
        if (shareType == null) {
            if (iterableBannerEvent is PostIterableBannerEvent.Dismiss) {
                Measurement.trackShareChatModalDismissed()
            }
        } else {
            Measurement.trackShareChatModalSeen()
        }

        _uiState.update {
            it.copy(
                showRegisterShareConvoModal = shareType
            )
        }
    }

    companion object {
        private const val ERROR_TYPE = "error_type"
        private const val RESPONSE_TIME_SECONDS = "response_time_seconds"
        private const val QUERY = "query"
        private const val APP_NAME = "app_name"
    }
}

sealed class ShareUrlState {

    /**
     * Empty state.
     */
    data object Empty : ShareUrlState()

    /**
     * Loading represents when we are fetching a share id.
     */
    data object Loading : ShareUrlState()

    /**
     * Success represents when we successfully fetched a share id. [shareId] Is the
     * share id we get back from the backend.
     */
    data class SuccessShare(val shareId: String) : ShareUrlState()

    /**
     * Error fetching a share id.
     */
    data object Error : ShareUrlState()

    /**
     * Share id has expired
     */
    data object ShareExpired : ShareUrlState()

    /**
     * Copy a share id
     */
    data class Copy(val shareId: String) : ShareUrlState()

    /**
     * Processing a deeplink
     */
    data class Processing(val shareId: String) : ShareUrlState()

    /**
     * ShareProcessed represents when we successfully processed a shared deeplink..
     */
    data object ShareProcessed : ShareUrlState()
}
