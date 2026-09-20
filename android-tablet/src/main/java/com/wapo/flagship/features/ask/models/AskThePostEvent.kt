package com.wapo.flagship.features.ask.models

import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.ask.ui.SourceBottomSheetCaller
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.search2.ui.CarouselUIItem
import com.wapo.flagship.features.search2.ui.SelectedPassageInfo
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

sealed class AskThePostEvent {
    data class InitiatePostAnswersFeedbackEvent(val feedBackEvent: Triple<String, String, Int>) :
        AskThePostEvent()

    data class ShareUrlStateEvent(val shareUrlState: ShareUrlState) : AskThePostEvent()
    data class UserEvent(
        val userEvent: com.wapo.flagship.features.search2.events.UserEvent,
        val caller: SourceBottomSheetCaller? = null
    ) :
        AskThePostEvent()

    data class ScreenShotTaken(val screenShotTaken: Boolean) : AskThePostEvent()
    data object ClearLiveConversationData : AskThePostEvent()
    data object DismissBottomSheet : AskThePostEvent()
    data object PassagesClicked : AskThePostEvent()
    data object CitationsClicked : AskThePostEvent()
    data object TrackArticleClicked : AskThePostEvent()
    data class StartLiveConversation(val question: Question? = null, val newText: String? = null) :
        AskThePostEvent()

    data class OpenConversation(
        val conversationId: String,
        val setPrivateMode: Boolean?,
        val hideOrShowResponse: Boolean = true,
        val showHistory: Boolean
    ) : AskThePostEvent()

    data class StartSamLiveConversation(val text: String) : AskThePostEvent()
    data class IsPrivateModeRequested(val requested: Boolean) : AskThePostEvent()

    data object OpenHowItWorks : AskThePostEvent()
    data object OnDoneLoadingFullConversation : AskThePostEvent()
    data class SetPassageData(val passageData: SelectedPassageInfo) : AskThePostEvent()
    data class InitiatePostAnswersFeedback(
        val endpoint: String?,
        val responseId: String?,
        val reaction: PostAnswersFeedbackReaction,
    ) : AskThePostEvent()

    data class ShowSourceSheetWith(val carouselItems: List<CarouselUIItem>) : AskThePostEvent()
    data class Share(val isTurn: Boolean) : AskThePostEvent()
    data class SetCitationData(val selectedPassageInfo: SelectedPassageInfo) : AskThePostEvent()
    data class QuestionClicked(val question: String, val tabName: String) : AskThePostEvent()
    data class TextSubmitted(val text: String) : AskThePostEvent()
    data object ResetShare : AskThePostEvent()
    data class HideOrShowPassage(val show: Boolean) :
        AskThePostEvent()

    data class HideOrShowHistory(val show: Boolean, val skipTracking: Boolean = false) :
        AskThePostEvent()

    data class PostAnswersCarouselItemClick(
        val userEvent: com.wapo.flagship.features.search2.events.UserEvent,
        val itemClicked: String
    ) :
        AskThePostEvent()

    data class DeleteConversation(
        val conversationId: String,
        val clearConversation: Boolean? = null
    ) : AskThePostEvent()

    data class CopyShareUrl(val shareUrl: String) : AskThePostEvent()
    data class DeleteShareUrlOrAll(val shareUrl: String?) : AskThePostEvent()
    data object DismissSplash : AskThePostEvent()

    data class ATPCtaDismissed(val banner: BannerPaywallMessage) : AskThePostEvent()
    data class ATPCtaClicked(
        val message: BannerPaywallMessage,
        val postIterableBannerEvent: PostIterableBannerEvent
    ) : AskThePostEvent()

    data class CreateAccountOrSignIn(
        val navBehavior: String,
        val iterableEvent: PostIterableBannerEvent
    ) : AskThePostEvent()

    data class DismissRegisterNewChatModal(val startNewChat: Boolean) : AskThePostEvent()
    data object StartNewChat : AskThePostEvent()
    data class ShowShareRegisterModal(val shareType: ShareType?) : AskThePostEvent()

    data object AskThePostReady : AskThePostEvent()
    data object TalkToThePostOpen: AskThePostEvent()
    data class AskThePostBannerSeen(val message: BannerPaywallMessage): AskThePostEvent()

    data object ConfirmAnonEnable: AskThePostEvent()
    data object DismissAnonAlert: AskThePostEvent()
}