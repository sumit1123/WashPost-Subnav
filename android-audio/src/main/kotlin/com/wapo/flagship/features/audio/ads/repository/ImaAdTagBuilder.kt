package com.wapo.flagship.features.audio.ads.repository

import com.wapo.flagship.features.audio.ads.model.AdRequestContext
import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import jakarta.inject.Inject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ImaAdTagBuilder @Inject constructor() {
    fun buildAudioAdTagUrl(
        audioMediaAdConfig: AudioMediaAdConfig,
        adBreak: AudioMediaAdBreak,
        adBreakIndex: Int,
        adRequestContext: AdRequestContext,
    ): String {
        val originalUrl = audioMediaAdConfig.adSetUrl

        val baseUrl = originalUrl.substringBefore('?')
        val rawQuery = originalUrl.substringAfter('?', "")

        if (rawQuery.isEmpty()) return originalUrl

        val params = rawQuery.split('&')
        val queryBuilder = StringBuilder()
        for (param in params) {
            val keyValue = param.split("=", limit = 2)
            val key = keyValue.getOrNull(0) ?: continue
            val value = keyValue.getOrNull(1) ?: continue

            val newValue = resolveQueryParamValue(
                value = value,
                audioMediaAdConfig = audioMediaAdConfig,
                adBreak = adBreak,
                adBreakIndex = adBreakIndex,
                adRequestContext = adRequestContext,
            ) ?: continue

            if (queryBuilder.isNotEmpty()) {
                queryBuilder.append("&")
            }
            queryBuilder
                .append(key)
                .append("=")
                .append(newValue)
        }

        return buildString {
            append(baseUrl)
            if (queryBuilder.isNotEmpty()) {
                append("?")
                append(queryBuilder)
            }
        }
    }

    private fun resolveQueryParamValue(
        value: String,
        audioMediaAdConfig: AudioMediaAdConfig,
        adBreak: AudioMediaAdBreak,
        adBreakIndex: Int,
        adRequestContext: AdRequestContext,
        macroKeys: Pair<Char, Char> = Pair('[', ']'),
        encodingKeys: Pair<Char, Char> = Pair('[', ']'),
    ): String? {
        var start = 0
        var end = value.length
        var encodeDepth = 0

        while (start < end) {
            val startChar = value[start]
            val endChar = value[end - 1]
            when {
                startChar == encodingKeys.first && endChar == encodingKeys.second -> {
                    encodeDepth++
                    start++
                    end--
                }

                startChar == macroKeys.first && endChar == macroKeys.second -> {
                    start++
                    end--
                    break
                }

                else -> break
            }
        }
        if (encodingKeys.first == macroKeys.first && encodeDepth > 0) encodeDepth--

        //  Validate macro wrapper
        val isValidMacro = start > 0 && end < value.length &&
                value[start - 1] == macroKeys.first &&
                value[end] == macroKeys.second
        if (!isValidMacro) return value

        val key = value.substring(start, end)
        val baseResult = resolveMacro(
            key,
            audioMediaAdConfig,
            adBreak,
            adBreakIndex,
            adRequestContext,
        )

        return baseResult?.let { resolved ->
            var result = resolved
            repeat(encodeDepth) {
                result = encode(result)
            }
            result
        }
    }

    private fun resolveMacro(
        value: String,
        audioMediaAdConfig: AudioMediaAdConfig,
        adBreak: AudioMediaAdBreak,
        adBreakIndex: Int,
        adRequestContext: AdRequestContext,
    ): String? = when (value) {
        "triton-ext-stid" -> audioMediaAdConfig.tritonExtStid
        "triton-ttag" -> getTtag(audioMediaAdConfig, adBreak, adBreakIndex)
        "break-type" -> adBreak.tritonValue
        "break-number" -> getMidrollPositionOrNull(audioMediaAdConfig, adBreak, adBreakIndex)?.toString()
        "max-ads" -> adBreak.maxAds.toString()
        "series-name" -> audioMediaAdConfig.seriesName?.let { encode(it) }
        "triton-delivery-method" -> audioMediaAdConfig.tritonDeliveryMethod
        "triton-feed-type" -> audioMediaAdConfig.tritonFeedType
        "content-language" -> audioMediaAdConfig.contentLanguage
        "bundle-id" -> adRequestContext.appIdentity.bundleId
        "store-id" -> adRequestContext.appIdentity.storeId
        "store-url" -> adRequestContext.appIdentity.storeUrl?.let { encode(it) }
        "site-url" -> adRequestContext.appIdentity.siteUrl?.let { encode(it) }
        "gdpr" -> adRequestContext.userPrivacyConsent?.gdpr
        "gdpr-consent" -> adRequestContext.userPrivacyConsent?.gdprConsent
        "us-privacy" -> adRequestContext.userPrivacyConsent?.usPrivacy
        "gpp" -> adRequestContext.userPrivacyConsent?.gpp
        "gpp-sid" -> adRequestContext.userPrivacyConsent?.gppSid
        else -> value
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private val AudioMediaAdBreak.tritonValue: String
        get() = when (this) {
            is AudioMediaAdBreak.Preroll -> "preroll"
            is AudioMediaAdBreak.Midroll -> "midroll"
            is AudioMediaAdBreak.Postroll -> "postroll"
        }

    private fun getMidrollPositionOrNull(
        audioMediaAdConfig: AudioMediaAdConfig,
        adBreak: AudioMediaAdBreak,
        adBreakIndex: Int,
    ): Int? {
        if (adBreak is AudioMediaAdBreak.Midroll) {
            val containsPreroll =
                audioMediaAdConfig.adBreaks?.any { it is AudioMediaAdBreak.Preroll } == true
            return if (containsPreroll) adBreakIndex - 1 else adBreakIndex
        }
        return null
    }

    private fun getTtag(
        audioMediaAdConfig: AudioMediaAdConfig,
        adBreak: AudioMediaAdBreak,
        adBreakIndex: Int,
    ): String? {
        val ttagList = mutableListOf<String>()
        val midrollPositionOrNull = getMidrollPositionOrNull(audioMediaAdConfig, adBreak, adBreakIndex)
        if (midrollPositionOrNull != null) {
            ttagList.add("ads_midroll_position:$midrollPositionOrNull")
        }
        val primarySectionId = audioMediaAdConfig.primarySectionId
        if (!primarySectionId.isNullOrEmpty()) {
            val sections = primarySectionId.split("/").filter { it.isNotEmpty() }
            for (i in sections.indices) {
                val key = if (i == 0) "section" else "subsection$i"
                ttagList.add("$key:${sections.subList(0, i + 1).joinToString("/")}")
            }
        }
        return if (ttagList.isEmpty()) null else ttagList.joinToString(",")
    }
}