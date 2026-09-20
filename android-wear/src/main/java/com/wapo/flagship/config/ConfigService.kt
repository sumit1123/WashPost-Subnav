/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.config

import android.content.Context
import com.wapo.android.commons.util.LogUtil
import com.wapo.flagship.utils.CrashWrapper
import com.wapo.flagship.utils.Utils
import com.washingtonpost.android.R
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object ConfigService {

    private const val TAG = "ConfigService"

    fun readConfig(ctx: Context): Config {
        val localConfig = File(ctx.filesDir, Config.CONFIG_LOCAL_FILENAME)
        return if (localConfig.exists()) {
            try {
                val inputStream = FileInputStream(localConfig)
                val jsonString: String = Utils.inputStreamToString(inputStream)
                val jsonConfig = JSONObject(jsonString)
                Config.configFromJSONObject(jsonConfig, ctx)
            } catch (e: Exception) {
                LogUtil.e(TAG, "Can't parse local config", e)
                CrashWrapper.sendException(e)
                // if reading local config fails, then try to read config from resources.
                clearOldConfig(ctx)
                readConfigFromResources(ctx)
            }
        } else readConfigFromResources(ctx)
    }

    private fun readConfigFromResources(ctx: Context): Config {
        return try {
            val inputStream = ctx.resources.openRawResource(R.raw.config)
            val configString: String = Utils.inputStreamToString(inputStream)
            Config.parseJson(configString, ctx)
        } catch (e: JSONException) {
            CrashWrapper.sendException(e)
            throw RuntimeException("Can not parse a config", e)
        }
    }

    private fun clearOldConfig(ctx: Context): Boolean {
        try {
            val localConfig = File(ctx.filesDir, Config.CONFIG_LOCAL_FILENAME)
            return if (localConfig.exists()) {
                localConfig.delete()
            } else {
                true //if the file doesn't exist, don't worry about the rest
            }
        } catch (e: java.lang.Exception) {
            LogUtil.d(TAG, "Unable to delete old config$e")
        }
        return false
    }

}