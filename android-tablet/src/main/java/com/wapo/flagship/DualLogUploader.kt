// Copyright (c) 2019 The Washington Post. All rights reserved.

package com.wapo.flagship

import android.content.Context
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.remotelog.logger.LogFileUploader
import com.wapo.android.remotelog.splunk.SplunkHECUploader
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.Config
import java.io.File

/*
Created a log uploader that will upload to Splunk based on certain conditions
 */
class DualLogUploader : LogFileUploader.UploadHelper {
    private val config: Config get() = ConfigManager.getInstance().config

    override fun upload_gZipCompressedFile(file: File?): Boolean {
        var wasEitherUploadSuccessful = false

        if (config.isSplunkLoggingActive &&
            isSampledForSplunkLogging(
                FlagshipApplication.getInstance(),
            )
        ) {
            if (SplunkHECUploader(
                    config.loggerConfig.splunkHttpURL,
                    WapoSecDataProvider.splunkToken,
                ).upload_gZipCompressedFile(file)
            ) {
                wasEitherUploadSuccessful = true
            }
        }

        return wasEitherUploadSuccessful
    }

    private fun isSampledForSplunkLogging(context: Context): Boolean {
        if (config.isSplunkSamplingActive) {
            // config value is a percentage, multiplying by 10 in order to sample across 0 - 1000 range
            val splunkSamplingRate = Math.round(config.splunkSamplingRate * 10) as Int
            return config.splunkSamplingSegment < splunkSamplingRate
        }
        return true
    }
}
