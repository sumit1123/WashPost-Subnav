package com.wapo.flagship.features.audio.service2.media.extensions

import android.net.Uri
import androidx.core.os.BundleCompat
import androidx.media3.common.MediaMetadata
import com.wapo.flagship.features.audio.ads.model.AudioAdConfig

/**
 * Useful extensions for [MediaMetadata].
 */
inline val MediaMetadata.id: String?
    get() = extras?.getString(METADATA_KEY_MEDIA_ID)

inline val MediaMetadata.playAd: String?
    get() = extras?.getString(METADATA_KEY_PLAY_AD)

inline val MediaMetadata.artist: String?
    get() = extras?.getString(METADATA_KEY_ARTIST)

inline val MediaMetadata.duration
    get() = extras?.getLong(METADATA_KEY_DURATION)

inline val MediaMetadata.album: String?
    get() = extras?.getString(METADATA_KEY_ALBUM)

inline val MediaMetadata.author: String?
    get() = extras?.getString(METADATA_KEY_AUTHOR)

inline val MediaMetadata.date: String?
    get() = extras?.getString(METADATA_KEY_DATE)

inline val MediaMetadata.trackCount
    get() = extras?.getLong(METADATA_KEY_NUM_TRACKS)

inline val MediaMetadata.artUri: Uri
    get() = this.extras?.getString(METADATA_KEY_ART_URI).toUri()

inline val MediaMetadata.albumArtUri: Uri
    get() = this.extras?.getString(METADATA_KEY_ALBUM_ART_URI).toUri()

inline val MediaMetadata.rating
    get() = extras?.getLong(METADATA_KEY_RATING)

inline val MediaMetadata.displaySubtitle: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_SUBTITLE)

inline val MediaMetadata.displayDescription: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_DESCRIPTION)

inline val MediaMetadata.displayIconUri: Uri
    get() = this.extras?.getString(METADATA_KEY_DISPLAY_ICON_URI).toUri()

inline val MediaMetadata.mediaUri: Uri
    get() = this.extras?.getString(METADATA_KEY_MEDIA_URI).toUri()

// ========= App specific constants - Start =========

inline val MediaMetadata.seriesSlug
    get() = extras?.getString(METADATA_KEY_SERIES_SLUG)

inline val MediaMetadata.podcastSlug
    get() = extras?.getString(METADATA_KEY_PODCAST_SLUG)

inline val MediaMetadata.subscriptionLinks: List<String>?
    get() = stringToList(extras?.getString(METADATA_SUBSCRIPTION_LINKS))

inline val MediaMetadata.displayTitlePrefix: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_TITLE_PREFIX)

inline val MediaMetadata.displayTitleSeparator: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_TITLE_SEPARATOR)

inline val MediaMetadata.displayPrimaryLabel: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_LABEL_PRIMARY)

inline val MediaMetadata.displayPrimaryLabelStyle: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_LABEL_STYLE)

inline val MediaMetadata.displaySecondaryLabel: String?
    get() = extras?.getString(METADATA_KEY_DISPLAY_LABEL_SECONDARY)

inline val MediaMetadata.playerType: String?
    get() = extras?.getString(METADATA_KEY_PLAYER_TYPE)

inline val MediaMetadata.firstPublished: Long?
    get() = extras?.getLong(METADATA_KEY_FIRST_PUBLISHED)

inline val MediaMetadata.imageUrl: String?
    get() = extras?.getString(METADATA_KEY_IMAGE_URL)

inline val MediaMetadata.imageCaption: String?
    get() = extras?.getString(METADATA_KEY_IMAGE_CAPTION)

inline val MediaMetadata.voices: List<String>?
    get() = stringToList(extras?.getString(METADATA_KEY_VOICES))

inline val MediaMetadata.caption: String?
    get() = extras?.getString(METADATA_KEY_CAPTION)

inline val MediaMetadata.contentUrl: String?
    get() = extras?.getString(METADATA_KEY_CONTENT_URL)

inline val MediaMetadata.sectionName: String?
    get() = extras?.getString(METADATA_KEY_SECTION_NAME)

inline val MediaMetadata.mediaAdsUri: Uri?
    get() = extras?.getString(METADATA_KEY_MEDIA_ADS_URI)?.toUri()

inline val MediaMetadata.audioType: String?
    get() = extras?.getString(METADATA_KEY_AUDIO_TYPE)

inline val MediaMetadata.adsConfig: AudioAdConfig?
    get() = extras?.let {
        BundleCompat.getParcelable(
            it,
            METADATA_KEY_ADS_CONFIG,
            AudioAdConfig::class.java
        )
    }

inline val MediaMetadata.isAd: Boolean?
    get() = extras?.getBoolean(METADATA_KEY_IS_AD)

inline val MediaMetadata.contentMediaId: String?
    get() = extras?.getString(METADATA_KEY_CONTENT_MEDIA_ID)

inline val MediaMetadata.contentMediaListIds: List<String>?
    get() = stringToList(extras?.getString(METADATA_KEY_CONTENT_MEDIA_PLAYLIST_IDS))

// ========= App specific constants - End =========

/**
 * Helper method for converting string to list
 */
fun stringToList(input: String?): List<String>? {
    if (input != null && input.isNotBlank() && input.contains(METADATA_LIST_DELIMITER)) {
        return input.split(METADATA_LIST_DELIMITER)
    }
    return null
}

// ========= App specific constants - Start =========

const val METADATA_KEY_SERIES_SLUG = "METADATA_KEY_SERIES_SLUG"
const val METADATA_KEY_PODCAST_SLUG = "METADATA_KEY_PODCAST_SLUG"
const val IS_SHARED_PODCAST = "IS_SHARED_PODCAST"
const val METADATA_SUBSCRIPTION_LINKS = "METADATA_SUBSCRIPTION_LINKS"
const val METADATA_LIST_DELIMITER = "@METADATA@"
const val METADATA_KEY_DISPLAY_TITLE_PREFIX = "METADATA_KEY_DISPLAY_TITLE_PREFIX"
const val METADATA_KEY_DISPLAY_TITLE_SEPARATOR = "METADATA_KEY_DISPLAY_TITLE_SEPARATOR"
const val METADATA_KEY_DISPLAY_LABEL_PRIMARY = "METADATA_KEY_DISPLAY_LABEL_PRIMARY"
const val METADATA_KEY_DISPLAY_LABEL_SECONDARY = "METADATA_KEY_DISPLAY_LABEL_SECONDARY"
const val METADATA_KEY_DISPLAY_LABEL_STYLE = "METADATA_KEY_DISPLAY_LABEL_STYLE"
const val METADATA_KEY_PLAYER_TYPE = "METADATA_KEY_PLAYER_TYPE"
const val METADATA_KEY_AUDIO_TYPE = "METADATA_KEY_AUDIO_TYPE"
const val METADATA_KEY_FIRST_PUBLISHED = "METADATA_KEY_FIRST_PUBLISHED"
const val METADATA_KEY_IMAGE_URL = "METADATA_KEY_IMAGE_URL"
const val METADATA_KEY_IMAGE_CAPTION = "METADATA_KEY_IMAGE_CAPTION"
const val METADATA_KEY_VOICES = "METADATA_KEY_VOICES"
const val METADATA_KEY_CAPTION = "METADATA_KEY_CAPTION"
const val METADATA_KEY_CONTENT_URL = "METADATA_KEY_CONTENT_URL"
const val METADATA_KEY_SECTION_NAME = "METADATA_KEY_SECTION_NAME"
const val METADATA_KEY_PLAY_AD = "METADATA_KEY_PLAY_AD"
const val METADATA_KEY_MEDIA_ADS_URI = "METADATA_KEY_MEDIA_ADS_URI"
const val METADATA_KEY_MEDIA_ID = "METADATA_KEY_MEDIA_ID"
const val METADATA_KEY_ARTIST = "METADATA_KEY_ARTIST"
const val METADATA_KEY_DURATION = "METADATA_KEY_DURATION"
const val METADATA_KEY_ALBUM = "METADATA_KEY_ALBUM"
const val METADATA_KEY_AUTHOR = "METADATA_KEY_AUTHOR"
const val METADATA_KEY_WRITER = "METADATA_KEY_WRITER"
const val METADATA_KEY_DATE = "METADATA_KEY_DATE"
const val METADATA_KEY_TRACK_NUMBER = "METADATA_KEY_TRACK_NUMBER"
const val METADATA_KEY_NUM_TRACKS = "METADATA_KEY_NUM_TRACKS"
const val METADATA_KEY_ART_URI = "METADATA_KEY_ART_URI"
const val METADATA_KEY_ALBUM_ART_URI = "METADATA_KEY_ALBUM_ART_URI"
const val METADATA_KEY_RATING = "METADATA_KEY_RATING"
const val METADATA_KEY_DISPLAY_SUBTITLE = "METADATA_KEY_DISPLAY_SUBTITLE"
const val METADATA_KEY_DISPLAY_DESCRIPTION = "METADATA_KEY_DISPLAY_DESCRIPTION"
const val METADATA_KEY_DISPLAY_ICON_URI = "METADATA_KEY_DISPLAY_ICON_URI"
const val METADATA_KEY_MEDIA_URI = "METADATA_KEY_MEDIA_URI"
const val METADATA_KEY_ADS_CONFIG = "METADATA_KEY_ADS_CONFIG"
const val METADATA_KEY_IS_AD = "METADATA_KEY_IS_AD"
const val METADATA_KEY_CONTENT_MEDIA_ID = "METADATA_KEY_CONTENT_MEDIA_ID"
const val METADATA_KEY_CONTENT_MEDIA_PLAYLIST_IDS = "METADATA_KEY_CONTENT_MEDIA_PLAYLIST_IDS"


// ========= App specific constants - End =========
