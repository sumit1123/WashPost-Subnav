/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package com.wapo.flagship.features.audio.models

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem

/**
 * Data class to encapsulate properties of a [MediaItem].
 *
 * If an item is [browsable] it means that it has a list of child media items that
 * can be retrieved by passing the mediaId to [MediaBrowserCompat.subscribe].
 *
 * Objects of this class are built from [MediaItem]s in
 * [AudioPagerFragmentViewModel.subscriptionCallback].
 */
data class MediaItemData(
    val mediaId: String? = null,
    val mediaUrl: String? = null,
    val titlePrefix: String? = null,
    val titleSeparator: String? = null,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val imageCaption: String? = null,
    val albumArtUrl: String? = null,
    val displayDate: String? = null,
    val duration: Long? = null,
    val seriesSlug: String? = null,
    val podcastSlug: String? = null,
    val subscriptionLinks: List<String>? = null,
    val notificationBitmap: Bitmap? = null,
    val firstPublished: Long? = null,
    val voices: List<PlaybackVoice>? = null,
    val caption: String? = null,
    val contentUrl: String? = null,
    val sectionName: String? = null,
    val playerTypeName: String? = null,
    var playbackState: Int,
    var playAd: String? = null,
    val style: String? = null,
    val audioType: String? = null,
    val isShared: Boolean? = false
) : Parcelable {

    private constructor(p: Parcel) : this(
        mediaId = p.readString(),
        mediaUrl = p.readString(),
        titlePrefix = p.readString(),
        titleSeparator = p.readString(),
        primaryLabel = p.readString(),
        secondaryLabel = p.readString(),
        title = p.readString() ?: "",
        subtitle = p.readString(),
        imageUrl = p.readString(),
        imageCaption = p.readString(),
        albumArtUrl = p.readString(),
        displayDate = p.readString(),
        duration = p.readValue(Long::class.java.classLoader) as? Long,
        seriesSlug = p.readString(),
        podcastSlug = p.readString(),
        subscriptionLinks = p.readArrayList(String::class.java.classLoader) as? List<String>,
        notificationBitmap = p.readParcelable(Bitmap::class.java.classLoader),
        firstPublished = p.readValue(Long::class.java.classLoader) as? Long,
        voices = p.readArrayList(PlaybackVoice::class.java.classLoader) as? List<PlaybackVoice>,
        caption = p.readString(),
        contentUrl = p.readString(),
        sectionName = p.readString(),
        playerTypeName = p.readString(),
        playbackState = p.readInt(),
        playAd = p.readString(),
        style = p.readString(),
        audioType = p.readString(),
        isShared = p.readValue(Boolean::class.java.classLoader) as? Boolean
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mediaId)
        dest.writeString(mediaUrl)
        dest.writeString(titlePrefix)
        dest.writeString(titleSeparator)
        dest.writeString(primaryLabel)
        dest.writeString(secondaryLabel)
        dest.writeString(title)
        dest.writeString(subtitle)
        dest.writeString(imageUrl)
        dest.writeString(imageCaption)
        dest.writeString(albumArtUrl)
        dest.writeString(displayDate)
        dest.writeValue(duration)
        dest.writeString(seriesSlug)
        dest.writeString(podcastSlug)
        dest.writeList(subscriptionLinks)
        dest.writeParcelable(notificationBitmap, 0)
        dest.writeValue(firstPublished)
        dest.writeList(voices)
        dest.writeString(caption)
        dest.writeString(contentUrl)
        dest.writeString(sectionName)
        dest.writeString(playerTypeName)
        dest.writeInt(playbackState)
        dest.writeString(playAd)
        dest.writeString(style)
        dest.writeString(audioType)
        dest.writeValue(isShared)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "MediaItemData(mediaId='$mediaId', mediaUrl='$mediaUrl', titlePrefix='$titlePrefix'," +
                " titleSeparator='$titleSeparator', primaryLabel='$primaryLabel', secondaryLabel='$secondaryLabel', `title='$title', subtitle='$subtitle', displayDate=$displayDate," +
                " duration=$duration, seriesSlug=$seriesSlug, podcastSlug=$podcastSlug," +
                " subscriptionLinks=$subscriptionLinks, notificationBitmap=$notificationBitmap," +
                " firstPublished=$firstPublished, voices=$voices, caption=$caption," +
                " contentUrl=$contentUrl, sectionName=$sectionName," +
                " playerTypeName=$playerTypeName, playbackState=$playbackState, playAd=$playAd, style=$style, audioType=$audioType, isShared=$isShared)"
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<MediaItemData> {
            override fun createFromParcel(parcel: Parcel): MediaItemData {
                return MediaItemData(parcel)
            }

            override fun newArray(size: Int): Array<MediaItemData?> {
                return arrayOfNulls(size)
            }
        }
    }
}

