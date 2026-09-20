/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.flagship.features.audio.service2.media.library

import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.IntDef
import androidx.media3.common.MediaItem

/**
 * Interface used by MusicService for looking up MediaItem objects.
 */
interface MusicSource : Iterable<MediaItem> {

    /**
     * Begins loading the data for this music source.
     */
    suspend fun load()

    /**
     * Method which will perform a given action after this MusicSource is ready to be used.
     */
    fun whenReady(performAction: (Boolean) -> Unit): Boolean

    /**
     * Searches the music source for items matching the query.
     */
    fun search(query: String, extras: Bundle): List<MediaItem>

    fun size(): Int {
        return this.toMutableList().size
    }
}

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class State

/**
 * State indicating the source was created, but no initialization has performed.
 */
const val STATE_CREATED = 1

/**
 * State indicating initialization of the source is in progress.
 */
const val STATE_INITIALIZING = 2

/**
 * State indicating the source has been initialized and is ready to be used.
 */
const val STATE_INITIALIZED = 3

/**
 * State indicating an error has occurred.
 */
const val STATE_ERROR = 4

/**
 * Base class for music sources
 */
abstract class AbstractMusicSource : MusicSource {
    @State
    var state: Int = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    /**
     * Performs an action when this MusicSource is ready.
     *
     * This method is *not* threadsafe. Ensure actions and state changes are only performed
     * on a single thread.
     */
    override fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }
            else -> {
                performAction(state == STATE_INITIALIZED)
                true
            }
        }

    /**
     * Handles searching a [MusicSource] from a focused voice search, often coming
     * from the Google Assistant.
     */
    override fun search(query: String, extras: Bundle): List<MediaItem> {
        val focusSearchResult = when (extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)) {
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                val genre = extras.getString(MediaStore.EXTRA_MEDIA_GENRE)
                filter { it.mediaMetadata.genre?.toString() == genre }
            }
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                val artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                filter { it.mediaMetadata.artist?.toString() == artist }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                val artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                val album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                filter {
                    it.mediaMetadata.artist?.toString() == artist &&
                            it.mediaMetadata.albumTitle?.toString() == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                val title = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)
                val album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                val artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                filter {
                    it.mediaMetadata.title?.toString() == title &&
                            it.mediaMetadata.albumTitle?.toString() == album &&
                            it.mediaMetadata.artist?.toString() == artist
                }
            }
            else -> emptyList()
        }

        return if (focusSearchResult.isEmpty() && query.isNotBlank()) {
            filter {
                it.mediaMetadata.title?.toString()?.contains(query, ignoreCase = true) == true ||
                        it.mediaMetadata.genre?.toString()?.contains(query, ignoreCase = true) == true
            }
        } else {
            focusSearchResult
        }
    }
}

private const val TAG = "MusicSource"
