package com.wapo.android.data.repository

import android.os.Bundle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogKeys
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.kmpshared.logger.WPLogger
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KMPRemoteLogRepoImpl
    @Inject
    constructor(
        private val legacyRepo: RemoteLogRepoImpl,
    ) : RemoteLogRepo {

        private val kmpLogger = AtomicReference<WPLogger?>(null)

        init {
            // Register before configuration callbacks begin emitting logs.
            RemoteLog.setRepo(this)
        }

        fun bindKmpLogger(logger: WPLogger?) {
            kmpLogger.set(logger)
        }

        /**
         * The Refined Bridge Logic:
         * - 'process' string becomes the official WPTag (e.g. "syncer", "push").
         * - 'message' is extracted from the EventLog.
         * - Everything else (including 'module') stays in the Extras map.
         */
        private fun routeToKMP(
            eventLog: EventLog?,
            tagStr: String?,
            paywallInfo: String?,
            action: (String, String, String?, Boolean, Map<Any, Any>) -> Unit,
        ) {
            val eventLog = eventLog ?: return
            val data = eventLog.getEventData()

            // 1. Message in Message
            val message = data[LogKeys.MESSAGE.keyName]?.toString()?.takeIf { it.isNotBlank() }.orEmpty()

            // 2. Use the passed TAG (process) as the WPTag
            val module = data[LogKeys.MODULE.keyName]?.toString()?.takeIf { it.isNotBlank() }
            val tag = tagStr?.takeIf { it.isNotBlank() } ?: module ?: "Android"

            val forceUpload = eventLog.isForceUpload()

            val sampling = eventLog.getSampling()

            // 3. Extras in Extras
            // We filter out only the message. This automatically keeps 'module'
            // and other metadata keys in the extras map as requested.
            val extras = buildMap<Any, Any> {
                data
                    .filter {
                        it.key != LogKeys.MESSAGE.keyName &&
                            it.key != LogKeys.SAMPLING_RATE.keyName &&
                                it.key != LogKeys.MODULE.keyName
                    }
                    .forEach { (key, value) ->
                        put(key, value ?: "null")
                    }
                paywallInfo?.let { put("legacy_paywall_info", it) }
            }

            action(message, tag, sampling, forceUpload, extras)
        }

        private fun routeToKmpPlatform(
            eventLog: EventLog?,
            process: String?,
            paywallInfo: String?,
            level: LogLevel,
        ) {
            routeToKMP(eventLog, process, paywallInfo) { msg, tag, sampling, forceUpload, extras ->
                val logger = kmpLogger.get()
                if (logger != null) {
                    logger.log(level, msg, LogOrigin.Module(tag), sampling, extras)
                    if (forceUpload) logger.upload()
                } else {
                    legacyRepo.log(level, msg, LogOrigin.Module(tag), sampling, extras)
                    if (forceUpload) legacyRepo.upload()
                }
            }
        }

        // --- DEBUG ---
        override fun d(eventLog: EventLog?) = d(eventLog, null, null)

        override fun d(
            eventLog: EventLog?,
            process: String?,
        ) = d(eventLog, process, null)

        override fun d(
            eventLog: EventLog?,
            process: String?,
            paywallInfo: String?,
        ) {
            routeToKmpPlatform(
                eventLog = eventLog,
                process = process,
                paywallInfo = paywallInfo,
                level = LogLevel.Debug,
            )
        }

        // --- ERROR ---
        override fun e(eventLog: EventLog?) = e(eventLog, null, null)

        override fun e(
            eventLog: EventLog?,
            process: String?,
        ) = e(eventLog, process, null)

        override fun e(
            eventLog: EventLog?,
            process: String?,
            paywallInfo: String?,
        ) {
            routeToKmpPlatform(eventLog, process, paywallInfo, LogLevel.Error)
        }

        // --- WARNING ---
        override fun w(eventLog: EventLog?) {
            routeToKmpPlatform(eventLog, null, null, LogLevel.Warn)
        }

        // --- VERBOSE ---
        override fun v(eventLog: EventLog?) = v(eventLog, null, null)

        override fun v(
            eventLog: EventLog?,
            process: String?,
        ) = v(eventLog, process, null)

        override fun v(
            eventLog: EventLog?,
            process: String?,
            paywallInfo: String?,
        ) {
            routeToKmpPlatform(eventLog, process, paywallInfo, LogLevel.Verbose)
        }

        // --- PAYWALL ---
        override fun p(eventLog: EventLog?) = p(eventLog, null, null)

        override fun p(
            eventLog: EventLog?,
            process: String?,
        ) = p(eventLog, process, null)

        override fun p(
            eventLog: EventLog?,
            process: String?,
            paywallInfo: String?,
        ) {
            routeToKmpPlatform(eventLog, process ?: "Paywall", paywallInfo, LogLevel.Info)
        }

        // --- METRICS ---
        override fun m(eventLog: EventLog?) = m(eventLog, null, null)

        override fun m(
            eventLog: EventLog?,
            process: String?,
        ) = m(eventLog, process, null)

        override fun m(
            eventLog: EventLog?,
            process: String?,
            paywallInfo: String?,
        ) {
            // Metrics mirror iOS (TLog): warning-level logs tagged with sampling.
            routeToKmpPlatform(eventLog, process ?: "Metrics", paywallInfo, LogLevel.Warn)
        }

        override fun uploadLogFiles(bundle: Bundle?) {
            kmpLogger.get()?.upload() ?: legacyRepo.uploadLogFiles(bundle)
        }
    }
