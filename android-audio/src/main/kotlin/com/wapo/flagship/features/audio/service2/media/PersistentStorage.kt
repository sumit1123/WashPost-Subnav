/*
 * Copyright 2020 Google Inc. All rights reserved.
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

package com.wapo.flagship.features.audio.service2.media

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import com.bumptech.glide.Glide
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.service2.media.extensions.asAlbumArtContentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object PersistentStorage {

    suspend fun saveRecentSong(mediaItem: MediaItem, position: Long, context: Context) {

        val preferences: SharedPreferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

        withContext(Dispatchers.IO) {

            /**
             * After booting, Android will attempt to build static media controls for the most
             * recently played song. Artwork for these media controls should not be loaded
             * from the network as it may be too slow or unavailable immediately after boot. Instead
             * we convert the iconUri to point to the Glide on-disk cache.
             */
            val localIconUri =
                try {
                    Glide.with(context).asFile().load(mediaItem.mediaMetadata.artworkUri)
                        .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE).get()
                        .asAlbumArtContentUri()
                } catch (t: Throwable) {
                    // Ignore for now. App should handle a few exceptions that are thrown by the Glide.
                    // No placeholder, fallback, error or listeners will work in this case.
                    Logger.e("PersistentStorage", "Error in Glide module. error_msg=${t.message}")
                    null
                }
            preferences.edit()
                .putString(RECENT_SONG_MEDIA_ID_KEY, mediaItem.mediaId)
                .putString(RECENT_SONG_TITLE_KEY, mediaItem.mediaMetadata.title.toString())
                .putString(RECENT_SONG_SUBTITLE_KEY, mediaItem.mediaMetadata.subtitle.toString())
                .putString(RECENT_SONG_ICON_URI_KEY, localIconUri?.toString())
                .putLong(RECENT_SONG_POSITION_KEY, position)
                .apply()
        }
    }
}

private const val PREFERENCES_NAME = "recent_song"
private const val RECENT_SONG_MEDIA_ID_KEY = "recent_song_media_id"
private const val RECENT_SONG_TITLE_KEY = "recent_song_title"
private const val RECENT_SONG_SUBTITLE_KEY = "recent_song_subtitle"
private const val RECENT_SONG_ICON_URI_KEY = "recent_song_icon_uri"
private const val RECENT_SONG_POSITION_KEY = "recent_song_position"
const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px