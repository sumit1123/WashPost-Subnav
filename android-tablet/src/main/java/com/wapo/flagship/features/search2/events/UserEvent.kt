package com.wapo.flagship.features.search2.events

import android.widget.ImageView
import com.wapo.flagship.features.search2.model.ArticleItem
import com.wapo.flagship.features.search2.model.ElectionItem
import com.wapo.flagship.features.search2.model.ExpandableItem
import com.wapo.flagship.features.search2.model.RecipeItem
import com.wapo.flagship.features.search2.model.SearchItem
import com.wapo.flagship.features.search2.model.SearchQueryItem
import com.wapo.flagship.features.search2.model.SectionItem
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import kotlin.reflect.KClass

sealed class UserEvent {
    class ArticleItemClick(
        val item: ArticleItem,
        var position: Int,
    ) : UserEvent()

    class RecipeItemClick(
        val item: RecipeItem,
        var position: Int,
    ) : UserEvent()

    class SectionItemClick(
        val item: SectionItem,
    ) : UserEvent()

    class ExpandableItemClick(
        val item: ExpandableItem,
    ) : UserEvent()

    class SearchQueryItemClick(
        val item: SearchQueryItem,
    ) : UserEvent()

    class RemoveItemClick(
        val item: SearchItem,
    ) : UserEvent()

    class RemoveAllItemsClick(
        val group: KClass<out SearchItem>,
    ) : UserEvent()

    class RecipeBookmarkClick(
        val item: RecipeItem,
        val view: ImageView,
        val isLoading: Boolean,
    ) : UserEvent()

    class PostAnswersCarouselItemClick(
        val url: String,
        val passages: List<String>?,
        val carouselPosition: Int,
    ) : UserEvent()

    data object PostAnswersInfoClick : UserEvent()

    class PostAnswersFeedbackItemClick(
        val endpoint: String,
        val responseId: String,
        val reaction: PostAnswersFeedbackReaction,
    ) : UserEvent()

    class PostAnswerShowMoreClick : UserEvent()

    class SearchElectionItemClick(
        val item: ElectionItem,
        var position: Int,
    ) : UserEvent()

    class AskQuestionItemClick(
        val id: String,
        val question: String,
    ) : UserEvent()

    class DeepLinkItemClick(
        val link: String,
    ) : UserEvent()

    class GiveFeedbackClicked(): UserEvent()

    class SeePassagesClicked(): UserEvent()
    class SeeCitationsClicked(): UserEvent()
    class TalkToThePostOpen(): UserEvent()
    class TalkToThePostPlayPauseTapped: UserEvent()
    class TalkToThePostClose: UserEvent()
    class TalkToThePostToggleCaptions(): UserEvent()
    class TalkToThePostOpenVoiceSelectionSheet(): UserEvent()
    class TalkToThePostUpdateCaptionsIndex(val index: Int) : UserEvent()
    class TalkToThePostCaptionsDoneRendering(): UserEvent()
}
