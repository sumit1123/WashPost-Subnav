/* Copyright (c) 2019 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.network

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * Data for request sent to /list, /save, or /delete endpoint
 * [uris] - list of [UrisRequestValue] items
 *  /list - gets the save status for these specific uris (if [uris] is empty in a /list call, the status for all saved stories is returned)
 *  /save - saves the uris
 *  /delete - deletes the uris
 */
data class SavedStoriesRequest(
        @SerializedName("uris") val uris: List<UrisRequestValue>
)

/**
 * [location] - the content URL
 * [userUpdated] - the last updated time, in milliseconds
 * [arcId] - optional, not used at the moment
 * [contentType] - optional, not used at the moment
 * [section] - optional, not used at the moment
 */
data class UrisRequestValue(
        @SerializedName("location") val location: String,
        @SerializedName("userUpdated") val userUpdated: Long,
        @SerializedName("arcId") val arcId: String? = null,
        @SerializedName("contentType") val contentType: String? = null,
        @SerializedName("section") val section: String? = null
)

/**
 * [status] - the response status
 * [state] - numerical state code
 * [uris] - the stories that were modified locally via a /save or /delete call
 * [saved] - the list of stories retrieved from a /list call
 */
data class SavedStoriesResponse(
        @SerializedName("status") val status: String?,
        @SerializedName("state") val state: String?,
        @SerializedName("uris") val uris: List<UrisResponseValue>?,
        @SerializedName("saved") val saved: List<SavedResponseValue>?
)

/**
 * The stories that were modified locally via a /save or /delete call
 * [location] - the content URL
 * [userCreated] - the initial time the story was saved
 * [userUpdated] - the last updated time, in milliseconds
 * [archived] - if the saved story has been archived
 * [status] - the status of the transaction attempt
 *  "DELETED": successfully deleted
 *  "SAVED": successfully saved
 *  "NOT_FOUND": URL not found
 *  "REJECTED": [userUpdated] value that was sent in request is less recent than the value currently in the DB
 *  "FAILURE": something went wrong, log this and contact backend team
 */
data class UrisResponseValue(
        @SerializedName("location") val location: String?,
        @SerializedName("userCreated") val userCreated: Long?,
        @SerializedName("userUpdated") val userUpdated: Long?,
        @SerializedName("archived") val archived: Boolean?,
        @SerializedName("status") val status: String?
)

/**
 * The list of stories retrieved from a /list call
 * [location] - the content URL
 * [userCreated] - the initial time the story was saved
 * [userUpdated] - the last updated time, in milliseconds
 * [arcId] - optional, not used at the moment
 * [contentType] - optional, not used at the moment
 * [section] - optional, not used at the moment
 */
data class SavedResponseValue(
        @SerializedName("location") val location: String?,
        @SerializedName("userCreated") val userCreated: Long?,
        @SerializedName("userUpdated") val userUpdated: Long?,
        @SerializedName("arcId") val arcId: String?,
        @SerializedName("contentType") val contentType: String?,
        @SerializedName("section") val section: String?
)

data class Metadata(
        @SerializedName("metadata") val metadata: List<MetadataEntry>?
)

data class MetadataEntry(
        @SerializedName("url") val url: String,
        @SerializedName("canonical_url") val canonicalUrl: String,
        @SerializedName("headline") val headline: String,
        @SerializedName("byline") val byLine: String,
        @SerializedName("label") val label: MetadataLabel,
        @SerializedName("display_date") val displayDate: String,
        @SerializedName("social_image") val socialImageUrl: String,
        @SerializedName("description") val description: String,
        @SerializedName("last_updated_date") val lastUpdated: String,
        @SerializedName("content_restriction_code") val contentRestrictionCode: String,
        @SerializedName("webview_preferred") val shouldOpenWebView: Boolean,
        @SerializedName("error") val error: String?

)

data class MetadataLabel(
        @SerializedName("basic") val basic: MetadataLabelText,
        @SerializedName("transparency") val transparency: MetadataLabelTransparency
)

data class MetadataLabelTransparency(
        @SerializedName("text") val text: String
)

data class MetadataLabelText(
        @SerializedName("text") val text: String
)

data class MetadataRequest(
        @SerializedName("urls") val urls: List<String>
)