package com.wapo.flagship.features.articles2.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.models.FollowState
import com.wapo.flagship.features.articles2.models.InlineTopicFollowItem
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import com.wapo.flagship.features.preferencesapi.models.Followable
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * This viewmodel will drive the state of the inline Topic Follow
 */
@HiltViewModel
class InlineTopicFollowViewModel
    @Inject
    constructor(
        private val contentPacksRepo: ContentPacksRepo,
        private val newslettersRepository: NewslettersRepository,
    ) : ViewModel() {
        /**
         * Live data for inline topic that needs to be updated.
         */
        private val _updateInlineTopicFollowItem = LiveEvent<String>()
        val updateInlineTopicFollowItem: LiveData<String> = _updateInlineTopicFollowItem

        private var inlineItemsMap = mutableMapOf<String, MutableList<InlineTopicFollowItem>>()

        private val isSignedIn
            get() = PaywallService.getInstance()?.isWpUserLoggedIn ?: false

        /**
         * reload given topic follow based on id.
         */
        fun reload(
            id: String,
            context: Context,
        ) {
            getInlineTopicFollowItem(id, null, context)
        }

        fun getFollowable(key: String?): Followable? {
            key ?: return null
            return contentPacksRepo.getFollowableForId(key)
        }

        /**
         * Create and store inline topic follow items in map.
         * @return the created item
         */
        fun getInlineTopicFollowItem(
            key: String?,
            group: String?,
            context: Context,
            shouldCreateNewInstance: Boolean = false,
        ): InlineTopicFollowItem? {
            if (key == null) {
                return null
            }
            // If followable doesn't exist, we should return and not add item to map
            val followable = contentPacksRepo.getFollowableForId(key) ?: return null
            val alertIds = followable.notifications?.mapNotNull { it?.id }
            val newsletterIds = followable.newsletters?.mapNotNull { it?.id }
            // fetch from content packs topic info, icon
            val state = getInlineTopicFollowState(key, context)

            var inlineItem: InlineTopicFollowItem? = null
            if (!this.inlineItemsMap.contains(key) || shouldCreateNewInstance) {
                inlineItem =
                    InlineTopicFollowItem(
                        topicDisplayName = followable.heading,
                        topicKey = key,
                        iconUrl = followable.image,
                        followState = state,
                    ).apply {
                        this.group = group
                    }
                if (!this.inlineItemsMap.containsKey(key)) {
                    this.inlineItemsMap[key] = mutableListOf()
                }
                this.inlineItemsMap[key]?.add(inlineItem)
            } else {
                this.inlineItemsMap[key]?.forEach {
                    it.followState = state
                }
            }

            this._updateInlineTopicFollowItem.value = key
            return inlineItem
        }

        /**
         * Get the state of the inline button
         * - plus -> if Topic is NOT FOLLOWED
         * - check -> if Topics is FOLLOWED
         */
        private fun getInlineTopicFollowState(
            packId: String,
            context: Context,
        ): FollowState =
            if (isSignedIn && contentPacksRepo.isTopicFollowed(context, packId)) {
                FollowState.FOLLOWED
            } else {
                FollowState.NOT_FOLLOWED
            }
    }
