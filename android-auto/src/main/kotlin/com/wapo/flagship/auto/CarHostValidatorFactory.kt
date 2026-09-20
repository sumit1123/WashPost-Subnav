package com.wapo.flagship.auto

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.car.app.validation.HostValidator
import com.wapo.flagship.features.audio.R as AudioR
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

internal object CarHostValidatorFactory {
    fun create(context: Context): HostValidator {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        }

        val allowedHosts = readReleaseHosts(context)
        if (allowedHosts.isEmpty()) {
            Log.e(TAG, "No release Android Auto hosts were found; host validation will fail closed")
        }
        return HostValidator
            .Builder(context)
            .apply {
                allowedHosts.forEach { host ->
                    addAllowedHost(host.packageName, host.certificateDigest)
                }
            }.build()
    }

    private fun readReleaseHosts(context: Context): Set<AllowedHost> {
        val parser = context.resources.getXml(AudioR.xml.allowed_media_browser_callers)
        return try {
            approvedCarHosts(parseReleaseHosts(parser))
        } catch (error: Exception) {
            Log.e(TAG, "Unable to read the Android Auto host allowlist", error)
            emptySet()
        } finally {
            parser.close()
        }
    }

    internal fun parseReleaseHosts(parser: XmlPullParser): Set<AllowedHost> {
        val hosts = linkedSetOf<AllowedHost>()
        var currentPackage: String? = null
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "signature" -> currentPackage = parser.getAttributeValue(null, "package")
                        "key" -> {
                            val packageName = currentPackage
                            val isRelease =
                                parser.getAttributeValue(null, "release")?.toBooleanStrictOrNull() == true
                            val digest =
                                parser
                                    .nextText()
                                    .replace(NON_HEX_REGEX, "")
                                    .lowercase(Locale.US)
                            if (isRelease && !packageName.isNullOrBlank() && digest.isNotBlank()) {
                                hosts += AllowedHost(packageName, digest)
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "signature") currentPackage = null
                }
            }
            eventType = parser.next()
        }
        return hosts
    }

    internal fun approvedCarHosts(hosts: Set<AllowedHost>): Set<AllowedHost> =
        hosts.filterTo(linkedSetOf()) { it.packageName in CAR_HOST_PACKAGES }

    internal data class AllowedHost(
        val packageName: String,
        val certificateDigest: String,
    )

    private const val TAG = "CarHostValidator"
    private val NON_HEX_REGEX = "[^0-9a-fA-F]".toRegex()
    private val CAR_HOST_PACKAGES =
        setOf(
            "com.google.android.projection.gearhead",
            "com.google.android.autosimulator",
            "com.google.android.carassistant",
        )
}
