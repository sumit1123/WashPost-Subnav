package com.wapo.flagship.features.utils

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.gson.Gson
import com.wapo.Utils
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.ONBOARDING
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PERSONALIZED_PODCAST_PLACEHOLDER
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PODCAST

object PersonalizedPodcastHelper {

    fun isPersonalizedPodcastItem(itemType: String?): Boolean {
        return itemType == PODCAST || itemType == ONBOARDING || itemType == PLACEHOLDER || itemType == PERSONALIZED_PODCAST_PLACEHOLDER
    }

    fun displayDate(dateLong: Long): String? {
        val context = AppContextUtils.appContext
        val format = context?.getString(R.string.date_format)
        return if (format != null) Utils.getDate(format, dateLong) else null
    }

    fun createJsonObject(selectedItems: SnapshotStateMap<String, Pair<Int, List<String>>>): Pair<String, String> {
        return if (selectedItems.isEmpty()) {
            Pair("{}", "{\"value\":{}}")
        } else {
            // A HashMap to build the final JSON structure
            val selectionMap = HashMap<String, Any>()

            selectedItems.forEach { (key, valuePair) ->
                val jsonKey = key.lowercase()
                // Check if what the selection limit is
                if (valuePair.first == 1) {
                    // If it's 1, we safely get the first item
                    val singleSelection = valuePair.second.firstOrNull()

                    if (singleSelection != null) {
                        selectionMap[jsonKey] = singleSelection
                    }
                } else if (valuePair.first > 1) {
                    // If the selection limit is greater than 1 we just use the list as the value
                    // An empty list is fine for multi-select
                    selectionMap[jsonKey] = valuePair.second
                }
            }
            // wrapperMap is used for sending to the Preferences API
            val wrapperMap = mapOf("value" to selectionMap)
            // Convert the dynamically-built map to a JSON string
            Pair(Gson().toJson(selectionMap), Gson().toJson(wrapperMap))
        }
    }

    object PersonalizedPodcastItemType {
        const val PODCAST = "podcast"
        const val ONBOARDING = "onboarding"
        const val PLACEHOLDER = "placeholder"

        const val PERSONALIZED_PODCAST_PLACEHOLDER = "personalized_podcast_placeholder"
        const val AUDIO_PODCAST = "audio"
    }
}