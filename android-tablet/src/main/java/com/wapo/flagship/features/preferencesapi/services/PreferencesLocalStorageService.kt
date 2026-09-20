// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.services

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.google.gson.Gson
import com.wapo.android.commons.util.Utils
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.wapo.flagship.features.preferencesapi.models.ContentPacksUiResponseBody
import com.wapo.flagship.features.preferencesapi.models.Followable
import com.wapo.flagship.util.PrefUtils
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

class PreferencesLocalStorageService
    @Inject
    constructor() {
        fun setContentPackItems(
            context: Context,
            data: ContentPacksUiResponseBody?,
        ) {
            try {
                FileUtils.writeStringToFile(File(context.filesDir, FILE_NAME), Gson().toJson(data))
            } catch (e: IOException) {
                Logger.d(TAG, "Exception writing content-pack.json to file: $e")
            }
        }

        fun setLastModifiedSince(
            context: Context,
            date: String,
        ) {
            PrefUtils.setContentPacksLastModified(context, date)
        }

        fun getLastModifiedSince(context: Context): String? = PrefUtils.getContentPacksLastModified(context)

        fun getFollowableForId(
            context: Context,
            id: String,
        ): Followable? {
            val contentPackItem = getContentPackForId(context, id)
            contentPackItem?.followable?.let {
                val requiredItems =
                    listOf(
                        it.heading,
                        it.registrationHeading,
                        it.unfollowedAffordanceTitle,
                        it.followedAffordanceTitle,
                        it.image,
                        it.unfollowedPrompt,
                        it.followedPrompt,
                    )
                if (requiredItems.any { item -> item == null }) {
                    return null
                } else {
                    return it
                }
            } ?: return null
        }

        fun getContentPackForId(
            context: Context,
            id: String,
        ): ContentPackUiItem? {
            val contentPacks = getContentPackItems(context)
            return contentPacks?.firstOrNull {
                it?.id == id
            }
        }

        fun getContentPackItems(context: Context): List<ContentPackUiItem?>? =
            try {
                val localContentPackJson = File(context.filesDir, FILE_NAME)
                if (localContentPackJson.exists()) {
                    val inputStream = FileInputStream(localContentPackJson)
                    val jsonString = Utils.inputStreamToString(inputStream)
                    Gson().fromJson(jsonString, ContentPacksUiResponseBody::class.java).contentPacks
                } else {
                    null
                }
            } catch (e: IOException) {
                Logger.d(TAG, "Exception reading content-pack.json from file: $e")
                null
            }

        companion object {
            private const val FILE_NAME = "content-pack.json"
            private val TAG = PreferencesLocalStorageService::class.java.simpleName
        }
    }
