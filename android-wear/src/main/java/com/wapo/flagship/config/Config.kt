/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.config

import android.content.Context
import com.google.gson.Gson
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.push.PushConfigStub
import com.wapo.flagship.features.section.models.SectionUrl
import com.wapo.flagship.utils.Utils
import org.json.JSONObject

class Config {

    var jsonAppBaseUrl: String? = null
    var singleNativeContentRainbowBaseUrl: String? = null
    var arcIoBaseUrl: String? = null

    // Map of section name to its url
    val availableSections = HashMap<String, SectionUrl>()

    var airshipPushConfig: PushConfigStub? = null
    var articleContentUpdateRulesConfig: ArticleContentUpdateRulesConfig =
        ArticleContentUpdateRulesConfig()

    var articleLimit: Int = 0
    var ttsLimit: Int = 0
    var paragraphLimit: Int = 0

    companion object {
        const val CONFIG_LOCAL_FILENAME = "config.json"
        private const val TAG = "Config"

        fun parseJson(configString: String, context: Context): Config {
            val jsonObject = JSONObject(configString)
            return configFromJSONObject(jsonObject, context)
        }

        fun configFromJSONObject(jsonObject: JSONObject, context: Context): Config {
            val config = Config()

            val gson = Gson()
            val jsonSections =
                if (jsonObject.has("availableSections")) {
                    jsonObject.getJSONArray("availableSections")
                } else {
                    null
                }
            if (jsonSections != null) {
                for (i in 0 until jsonSections.length()) {
                    val section = Section.fromJSONObject(jsonSections.getJSONObject(i))
                    config.availableSections[section.name] = section.sectionUrl
                }
            }
            config.jsonAppBaseUrl =
                if (jsonObject.has("jsonAppBaseUrl")) {
                    jsonObject.getString("jsonAppBaseUrl")
                } else {
                    null
                }
            config.singleNativeContentRainbowBaseUrl =
                if (jsonObject.has("singleNativeContentRainbowBaseUrl")) {
                    jsonObject.getString("singleNativeContentRainbowBaseUrl")
                } else {
                    null
                }
            config.arcIoBaseUrl =
                if (jsonObject.has("arcIoBaseUrl")) {
                    jsonObject.getString("arcIoBaseUrl")
                } else {
                    null
                }
            config.airshipPushConfig =
                if (jsonObject.has("pushConfig")) {
                    PushConfigStub.fromJSONObject(jsonObject.getJSONObject("pushConfig"))
                } else {
                    getDefaultPushConfigStub(context, "pushConfig")
                }
            config.airshipPushConfig?.apply { userData = DeviceUtils.getUniqueDeviceId(context) }
            config.articleLimit =
                if (jsonObject.has("articleLimit")) {
                    jsonObject.getString("articleLimit").toInt()
                } else {
                    0
                }
            config.ttsLimit =
                if (jsonObject.has("ttsLimit"))
                    jsonObject.getString("ttsLimit").toInt()
                else 0
            config.paragraphLimit =
                if (jsonObject.has("paragraphLimit")) {
                    jsonObject.getString("paragraphLimit").toInt()
                }
                else {
                    0
                }
            if (jsonObject.has("articleContentUpdateRulesConfig")) {
                config.articleContentUpdateRulesConfig =
                    gson.fromJson(
                        jsonObject.getString("articleContentUpdateRulesConfig"),
                        ArticleContentUpdateRulesConfig::class.java
                    )
            }

            return config
        }

        fun getDefaultPushConfigStub(context: Context, jsonObjectName: String): PushConfigStub? {
            try {
                val inputStream = context.resources.assets.open("default_push_config_stub.json")
                val json = Utils.inputStreamToString(inputStream)
                return PushConfigStub.fromJSONObject(JSONObject(json).getJSONObject(jsonObjectName))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }

}