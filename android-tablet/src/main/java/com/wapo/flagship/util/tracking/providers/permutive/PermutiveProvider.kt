package com.wapo.flagship.util.tracking.providers.permutive

import android.content.Context
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.permutive.android.*
import com.permutive.android.ads.addPermutiveTargeting
import com.wapo.android.commons.config.sec.model.PermutiveConfig
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.audio.service2.media.extensions.toUri
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.util.tracking.Evars
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.MeasurementMap
import com.wapo.flagship.util.tracking.Provider
import com.wapo.flagship.util.tracking.Translator
import com.wapo.flagship.util.tracking.providers.permutive.PermutiveEventProperties.*
import com.washingtonpost.android.BuildConfig
import java.io.IOException
import java.util.*

/**
 * Permutive Provider class to handle Permutive SDK
 */
class PermutiveProvider(
    private val context: Context,
    private val config: PermutiveConfig?,
    private val translator: Translator,
) : Provider {
    private val tag = PermutiveProvider::class.java.simpleName

    private var permutive: Permutive? = null
    private var pageTracker: PageTracker? = null

    fun isSdkInitialized(): Boolean = permutive != null

    /**
     * Initializes Permutive SDK by using [PermutiveConfig] values.
     */
    fun initialize() {
        if (isSdkInitialized()) return
        if (config?.workspaceId.isNullOrEmpty() || config?.apiKey.isNullOrEmpty()) {
            Logger.e(tag, "Permutive Config is missing!")
            return
        }
        // Permutive is throwing an Exception in a release version. Added proguard rules to fix it.
        // Added initialization in a try-catch block to avoid crashes when sdk throws an
        // exception in any case.
        try {
            permutive =
                Permutive(
                    context = this.context,
                    workspaceId = UUID.fromString(config?.workspaceId),
                    apiKey = UUID.fromString(config?.apiKey),
                ).also {
                    it.setDeveloperMode(BuildConfig.DEBUG)
                }
        } catch (th: Throwable) {
            th.printStackTrace()
            EventLog
                .Builder()
                .apply {
                    setMessage("Permutive Initialization Error")
                    setModule(LogModules.ANALYTICS)
                    setErrorMessage(th.message)
                    set("cause", th.cause)
                }.run {
                    RemoteLog.e(context, build())
                }
        }
    }

    /**
     * handles page views
     */
    override fun trackState(
        name: String,
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    ) {
        if (!isSdkInitialized()) return

        if (BuildConfig.DEBUG) {
            Logger.d(tag, "Permutive, Original Map =>")
            printValues(name, measurementMap)
        }

        // Close Previous pageTracker object
        if (pageTracker != null) {
            pause()
            stop()
        }

        val pageTitle = trackingInfo?.title
        var contentUrl = measurementMap[Evars.CONTENT_URL.variable]?.toString()
        // page_path is there for sections. Otherwise fallback to regular content url.
        val pathOrContentUrl = trackingInfo?.pagePath ?: contentUrl

        val translatedMap = translator.translate(measurementMap, trackingInfo)
        if (translatedMap.isEmpty()) {
            Logger.w(
                tag,
                "Permutive, Skip event due to translated map is empty!" +
                    " CONTENT_TYPE=${measurementMap[Evars.CONTENT_TYPE.variable]}",
            )
            return
        }

        if (BuildConfig.DEBUG) {
            Logger.d(tag, "Permutive, Translated Map =>")
            printValues(name, translatedMap)
        }

        trackPermutiveEvent(name, pageTitle, pathOrContentUrl, translatedMap)
    }

    /**
     * handles page events
     */
    override fun trackEvent(
        eventName: String,
        measurementMap: MeasurementMap,
        trackingInfo: TrackingInfo?,
    ) {
        if (!isSdkInitialized()) return

        if (BuildConfig.DEBUG) {
            printValues(eventName, measurementMap)
        }

        when (eventName) {
            Events.EVENT_SCROLL_END.key -> trackPercentageViewed(1f)
            else -> {
                // no op
            }
        }
    }

    override fun pause() {
        pageTracker?.pause()
    }

    override fun resume() {
        pageTracker?.resume()
    }

    override fun stop() {
        pageTracker?.close()
        pageTracker = null
    }

    @Throws(IOException::class)
    override fun release() {
        stop()
        // close() call may throw IOException.
        permutive?.close()
        permutive = null
    }

    private fun trackPermutiveEvent(
        eventName: String,
        pageTitle: String?,
        contentUrl: String?,
        measurementMap: MeasurementMap,
    ) {
        Logger.d(tag, "trackPermutiveEvent(), eventName=$eventName, map=$measurementMap")
        pageTracker =
            permutive
                ?.trackPage(
                    convertToEventProperties(contentUrl, measurementMap),
                    pageTitle,
                    contentUrl?.toUri(),
                    null,
                )?.also {
                    it.resume()
                }
    }

    /**
     * Read key values from MeasurementMap and create [EventProperties] for all supported data types.
     */
    private fun convertToEventProperties(
        contentUrl: String?,
        translatedMap: MeasurementMap,
        rootLevel: Boolean = true,
    ): EventProperties {
        var propsBuilder = EventProperties.Builder()
        translatedMap.forEach { entry ->
            propsBuilder =
                when (entry.value) {
                    is Boolean -> {
                        propsBuilder.with(entry.key, entry.value as Boolean)
                    }
                    is Int -> {
                        propsBuilder.with(entry.key, entry.value as Int)
                    }
                    is String -> {
                        propsBuilder.with(entry.key, entry.value as String)
                    }
                    is Float -> {
                        propsBuilder.with(entry.key, entry.value as Float)
                    }
                    is Double -> {
                        propsBuilder.with(entry.key, entry.value as Double)
                    }
                    is Date -> {
                        propsBuilder.with(entry.key, entry.value as Date)
                    }
                    is List<*> -> {
                        val list = entry.value as List<*>
                        when (list.firstOrNull()) {
                            is Boolean ->
                                propsBuilder.withBooleans(
                                    entry.key,
                                    list.filterIsInstance<Boolean>(),
                                )
                            is Int -> propsBuilder.withInts(entry.key, list.filterIsInstance<Int>())
                            is String ->
                                propsBuilder.withStrings(
                                    entry.key,
                                    list.filterIsInstance<String>(),
                                )
                            is Float ->
                                propsBuilder.withFloats(
                                    entry.key,
                                    list.filterIsInstance<Float>(),
                                )
                            is Double ->
                                propsBuilder.withDoubles(
                                    entry.key,
                                    list.filterIsInstance<Double>(),
                                )
                            is Date -> propsBuilder.withDates(entry.key, list.filterIsInstance<Date>())
                            else -> propsBuilder
                        }
                    }
                    is MeasurementMap -> {
                        propsBuilder.with(
                            entry.key,
                            convertToEventProperties(contentUrl, entry.value as MeasurementMap, false),
                        )
                    }
                    else -> propsBuilder
                }
        }

        if (rootLevel) {
            propsBuilder = propsBuilder.with(SDK_ISP_INFO.propertyName, EventProperties.ISP_INFO)
            propsBuilder = propsBuilder.with(SDK_GEO_INFO.propertyName, EventProperties.GEO_INFO)
        }

        return propsBuilder.build()
    }

    private fun trackPercentageViewed(percentage: Float) {
        if (!isSdkInitialized()) return

        pageTracker?.updatePercentageViewed(percentage)
    }

    /**
     * Method to add permutive targeting values to the given [AdManagerAdRequest.Builder]
     */
    fun addCustomTargeting(adManagerAdRequestBuilder: AdManagerAdRequest.Builder?) {
        adManagerAdRequestBuilder ?: return
        if (!isSdkInitialized()) return

        Logger.i(
            tag,
            "Permutive data. userId=${permutive?.currentUserId()}" +
                ", sessionId=${permutive?.sessionId()}" +
                ", segments=${permutive?.currentSegments}" +
                ", reactions=${permutive?.currentReactions}",
        )

        if (permutive is PermutiveSdk) {
            adManagerAdRequestBuilder.addPermutiveTargeting(permutive as PermutiveSdk)
        } else {
            Logger.w(
                tag,
                "Permutive, Unable to add custom targeting to AdManagerAdRequest.Builder",
            )
        }
    }

    /**
     * Method to set identity
     */
    fun setLoginIdentity(id: String?) {
        Logger.d(tag, "Permutive, setIdentity(), id=$id")
        if (!isSdkInitialized()) return
        val idAlias =
            Alias.create(
                tag = "login_id",
                identity = id?.lowercase() ?: "",
                priority = 0,
                expiry = Alias.NEVER_EXPIRE,
            )
        permutive?.setIdentity(listOf(idAlias))
    }

    private fun printValues(
        eventName: String,
        measurementMap: MeasurementMap,
    ) {
        val builder = StringBuilder()
        builder.append("Permutive, trackEvent =>> $eventName")
        var parametersCount = 0
        measurementMap.forEach {
            if (it.key.isNotEmpty()) {
                builder.append("\n  ${it.key} =>> ${it.value}")
                parametersCount++
            }
        }
        builder.append("\n\n")
        Logger.d(tag, builder.toString())
        Logger.d(tag, "Permutive, Total: parametersCount: $parametersCount")
    }
}
