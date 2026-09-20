// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.fusion.viewmodel

import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.fusion.event.EllipsisMenuAction
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Viewmodel tied directly to [EllipsisMenuFragment] and it's actions
 */
@HiltViewModel
class EllipsisMenuViewModel
    @Inject
    constructor(
        private val savedArticleManager: SavedArticleManager,
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        companion object {
            val tag: String = EllipsisMenuViewModel::class.java.simpleName
        }

        /**
         * Liveevent to listen to menu item being clicked
         */
        private val _choiceClickEvent = LiveEvent<EllipsisMenuAction>()
        val choiceClickEvent: LiveData<EllipsisMenuAction> = _choiceClickEvent

        /**
         * Members to load LiveData<ArticleAndMetaData> from db.
         */
        private val currentPageUrl = MutableLiveData<String>()

        /**
         * Live Data to get article meta and determine bookmarked status
         */
        val liveArticleByUrl =
            currentPageUrl.switchMap { url ->
                savedArticleManager.getLiveArticleByUrl(url)
            }

        /**
         * Set the current article url
         */
        fun setCurrentPageUrl(
            @NonNull url: String,
        ) {
            currentPageUrl.value = url
        }

        /**
         * Send Save event
         */
        fun handleSave(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionSave(actionItem)
        }

        /**
         * Send remove from saved list event
         */
        fun handleRemove(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionRemove(actionItem)
        }

        /**
         * Send read articles event
         */
        fun handleRead(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionRead(actionItem)
        }

        /**
         * Send share event
         */
        fun handleShare(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionShare(actionItem)
        }

        /**
         * Send share event
         */
        fun handleGift(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionGift(actionItem)
        }

        /**
         * Remove article from database.
         */
        fun removeSavedArticle(url: String) {
            val articleAndMetadata = ArticleAndMetadata(0, contentURL = replaceHttp(url))
            savedArticleManager.removeArticles(
                listOf(articleAndMetadata)
            )
        }

        fun isSaved(url: String): Boolean {
            val result = savedArticleManager.getArticleByUrl(url)
            return result != null
        }

        /**
         * Save article to database
         */
        fun saveArticle(actionItem: EllipsisActionItem) {
            val savedArticleModel =
                SavedArticleModel(
                    replaceHttp(actionItem.url),
                    System.currentTimeMillis()
                )
            val metadataModel =
                MetadataModel(
                    replaceHttp(actionItem.url),
                    System.currentTimeMillis()
                ).also {
                    it.imageURL = actionItem.imageUrl
                    it.headline = actionItem.headline
                    it.byline = actionItem.byline
                }
            savedArticleManager.addArticle(savedArticleModel, metadataModel)
        }

        private fun replaceHttp(url: String): String = url.replace("http://", "https://")

        fun handleAddToPlaylist(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionAddToPlayList(actionItem)
        }

        fun handleRemoveFromPlaylist(actionItem: EllipsisActionItem) {
            _choiceClickEvent.value = EllipsisMenuAction.ActionRemoveFromPlaylist(actionItem)
        }
    }
