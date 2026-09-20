package com.wapo.flagship.features.audio.playlist

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.audio.config2.AudioMediaSubscriptionLinks
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioMediaAdConfigTypeConverter
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioMediaSubscriptionLinksTypeConverter
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioTrackerTypeConverter
import com.wapo.flagship.features.audio.playlist.typeconverters.AudioVoiceListTypeConverter

@Entity(tableName = "playlist")
class Playlist(
    @ColumnInfo(name = "player_type") val playerType: String? = null,
    @NonNull
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "media_id") val mediaId: String? = null,
    @ColumnInfo(name = "human_ads_url") val humanAdsUrl: String? = null,
    @ColumnInfo(name = "human_raw_url") val humanRawUrl: String? = null,
    @ColumnInfo(name = "manifest_url") val manifestUrl: String? = null,
    @ColumnInfo(name = "ads_url") val adsUrl: String? = null,
    @ColumnInfo(name = "raw_url") val rawUrl: String? = null,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "title_prefix") val titlePrefix: String? = null,
    @ColumnInfo(name = "title_separator") val titleSeparator: String? = null,
    @ColumnInfo(name = "subtitle") val subtitle: String? = null,
    @ColumnInfo(name = "date") val date: Long? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "image_caption") val imageCaption: String? = null,
    @ColumnInfo(name = "duration") val duration: Long? = null,
    @ColumnInfo(name = "stream_url") val streamUrl: String? = null,
    @ColumnInfo(name = "stream_url_no_ads") val streamUrlNoAds: String? = null,
    @ColumnInfo(name = "content_url") val contentUrl: String? = null,
    @ColumnInfo(name = "section_name") val sectionName: String? = null,
    @ColumnInfo(name = "caption") val caption: String? = null,
    @ColumnInfo(name = "label_type") val labelType: String? = null,
    @ColumnInfo(name = "primary_label") val primaryLabel: String? = null,
    @ColumnInfo(name = "secondary_label") val secondaryLabel: String? = null,
    @TypeConverters(AudioVoiceListTypeConverter::class)
    @ColumnInfo(name = "voices") var voices: List<AudioVoice>? = null,
    @ColumnInfo(name = "arc_id") val arcId: String? = null,
    @TypeConverters(AudioTrackerTypeConverter::class)
    @ColumnInfo(name = "tracker") val tracker: AudioTracker? = null,
    @ColumnInfo(name = "primary_label_style") val primaryLabelStyle: String? = null,
    @ColumnInfo(name = "series") val series: String? = null,
    @TypeConverters(AudioMediaAdConfigTypeConverter::class)
    @ColumnInfo(name = "ad_config") val adConfig: AudioMediaAdConfig? = null,
    @ColumnInfo(name = "series_slug") val seriesSlug: String? = null,
    @ColumnInfo(name = "podcast_slug") val podcastSlug: String? = null,
    @TypeConverters(AudioMediaSubscriptionLinksTypeConverter::class)
    @ColumnInfo(name = "subscription_links") val subscriptionLinks: AudioMediaSubscriptionLinks? = null,
)
