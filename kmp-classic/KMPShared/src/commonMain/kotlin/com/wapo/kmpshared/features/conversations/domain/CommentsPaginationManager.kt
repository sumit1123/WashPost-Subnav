package com.wapo.kmpshared.features.conversations.domain

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.value
import org.koin.core.annotation.Single

@Single
class CommentsPaginationManager {
    private val mainFeedCursor = AtomicReference<String?>(null)
    private val hasMoreMainItems = AtomicBoolean(true)

    private val threadCursors = ConcurrentMutableMap<String, String?>()
    private val threadHasMore = ConcurrentMutableMap<String, Boolean>()

    // If user gets to within 4 items from the bottom of the feed, we'll trigger the next fetch
    private val mainThreshold = 4
    private val replyThreshold = 2

    // For tracking IDs currently being fetched
    private val inFlightRequests = ConcurrentMutableSet<String?>()

    fun shouldFetchMore(
        index: Int?,
        count: Int,
        parentId: String?,
    ): Boolean {
        if (inFlightRequests.contains(parentId)) return false

        // Initial load case
        if (index == null) {
            return count == 0
        }

        // Find where the user is currently looking
        val isMainFeed = parentId == null
        val hasMore = if (isMainFeed) hasMoreMainItems.value else threadHasMore[parentId] ?: false

        // Satisfy threshold (e.g., user is at index 16 of a 20-item list)
        val currentThreshold = if (isMainFeed) mainThreshold else replyThreshold
        return hasMore && index >= count - currentThreshold
    }

    fun markAsFetching(parentId: String?) = inFlightRequests.add(parentId)

    fun markAsFinished(parentId: String?) = inFlightRequests.remove(parentId)

    fun update(
        parentId: String?,
        cursor: String?,
        hasMore: Boolean,
    ) {
        if (parentId == null) {
            mainFeedCursor.value = cursor
            hasMoreMainItems.value = hasMore
        } else {
            threadCursors[parentId] = cursor
            threadHasMore[parentId] = hasMore
        }
    }

    fun getRequestParams(parentId: String?): Pair<String?, Boolean> =
        if (parentId == null) {
            mainFeedCursor.value to hasMoreMainItems.value
        } else {
            threadCursors[parentId] to (threadHasMore[parentId] ?: true)
        }

    /**
     * Logic to determine if this is the first time we are loading this specific context.
     * Used for switching tabs
     */
    fun isFreshLoad(): Boolean = mainFeedCursor.value == null && threadCursors.isEmpty()

    fun isInitialLoadForThread(parentId: String): Boolean = threadCursors[parentId] == null

    /**
     * When user switches tab or a manual pull-to-refresh
     */
    fun reset() {
        mainFeedCursor.value = null
        hasMoreMainItems.value = true
        threadCursors.clear()
        threadHasMore.clear()
        inFlightRequests.clear()
    }
}
