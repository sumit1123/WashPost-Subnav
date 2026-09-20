package com.wapo.flagship.features.audio.service2.media.library

import androidx.media3.common.MediaItem
import java.util.Collections

class BrowseTree(
    private var musicSource: MusicSource?, // Make musicSource mutable if it can change
) {
    // This map will hold the hierarchical structure of the media library
    // Key: parentMediaId (e.g., ROOT, ALBUMS, album title)
    // Value: List of MediaItems that are children of that parent
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaItem>>()
    private val listeners = mutableListOf<() -> Unit>()

    /**
     * Rebuilds or updates the entire browse tree structure based on the current musicSource.
     * This method should be called whenever the underlying music data (MusicSource) changes.
     */
    fun updateTree() {
        mediaIdToChildren.clear() // Clear previous data before rebuilding

        val rootList = mutableListOf<MediaItem>()


        musicSource?.forEach { mediaItem ->
            rootList.add(mediaItem) // Add all items to the root list
        }

        mediaIdToChildren[ROOT] = rootList

        // Notify listeners once done
        listeners.forEach { it() }
    }

    /**
     * Retrieves the children for a given parent Media ID.
     * This is what MediaLibraryService's onGetChildren will call.
     */
    fun get(parentId: String): List<MediaItem> {
        return mediaIdToChildren[parentId] ?: Collections.emptyList()
    }

    fun getItem(mediaId: String): MediaItem? {
        // A simple way to find an item across all children, or iterate through musicSource
        return musicSource?.find { it.mediaId == mediaId }
    }

    fun onTreeUpdated(listener: () -> Unit) {
        listeners += listener
    }

    companion object {
        const val ROOT = "ROOT"
    }
}
