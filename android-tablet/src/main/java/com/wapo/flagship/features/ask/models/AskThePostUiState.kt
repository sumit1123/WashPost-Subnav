package com.wapo.flagship.features.ask.models

import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.features.search2.state.PostAnswersUIState
import com.wapo.flagship.features.search2.ui.CarouselUIItem
import com.wapo.flagship.features.search2.ui.SelectedPassageInfo
import com.washingtonpost.android.config.domain.models.config.TimeOfDay
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

data class AskThePostUIState(
    val postAnswersUIState: PostAnswersUIState? = null,
    val showResponse: Boolean = false,
    val showPassage: Boolean? = null,
    val showCitation: Boolean = false,
    val selectedPassageInfo: SelectedPassageInfo? = null,
    val storedCitationInfo: SelectedPassageInfo? = null,
    val showHistory: Boolean = false,
    val historyList: List<ConversationItem.HistoryItem>? = null,
    val conversationHistory: List<ConversationItem> = emptyList(),
    val sourceSheetItems: List<CarouselUIItem> = emptyList(),
    val continuingConversation: Boolean = false,
    val isStreamingResponseLoading: Boolean = false,
    val connectionClosed: Boolean = true,
    val isPrivateModeEnabled: Boolean = false,
    val isPrivateModeRequested: Boolean = false,
    val searchQueryResponse: String? = null,
    val conversationId: String? = null,
    val shareUrlState: ShareUrlState = ShareUrlState.Empty,
    val historyCategories: List<String> = emptyList(),
    val sharedLinks: List<AskThePostShare> = emptyList(),
    val screenShotTaken: Boolean = false,
    val incomingShareId: String? = null,
    val askThePostMessage: BannerPaywallMessage? = null,
    val showBanner: Boolean = false,
    val showRegisterInHistoryView: Boolean = false,
    val showRegisterSaveChatModal: Boolean = false,
    val showRegisterShareConvoModal: ShareType? = null,
    val postIterableBannerEvent: PostIterableBannerEvent? = null,
    val isUserLoggedIn: Boolean = false
)

data class AskThePostQuestionsUiState(
    val questions: List<QuestionItem>? = null,
    val enteredText: String? = null,
    val timeOfDay: TimeOfDay? = null,
)

data class MainAskThePostUIState(
    val askThePostUIState: AskThePostUIState,
    val askThePostQuestionsUiState: AskThePostQuestionsUiState,
    val audioPlaybackState: AudioPlaybackState? = null,
    val articleQuestions: List<Question> = emptyList()
)

enum class ShareType {
    CONVO,
    TURN
}