package com.wapo.flagship.features.articles3.models

import android.content.Context
import com.wapo.android.commons.config.ConfigHelper
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles3.models.ui.SubNavTabUiModel
import com.washingtonpost.android.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * Supplies the chips for a `sub_nav` element, in the same three tiers the old
 * `ConfigManager` path used:
 *
 * 1. **Disk** — the last payload that downloaded successfully, so a returning reader gets the
 *    real chips instantly and offline.
 * 2. **Bundled** — `R.raw.section_election_config`, for a first run or after an app upgrade.
 * 3. **Remote** — the `siteMap` endpoint, which refreshes both of the above.
 *
 * The payload is a site-service tree whose first child is the section and whose grandchildren
 * are the chips:
 * ```
 * { "children": [ { "name": "Elections 2026",
 *                   "children": [ { "name": "House", "path": "…" }, … ] } ] }
 * ```
 *
 * Parsed with [JSONObject] rather than a generated adapter on purpose: the contract is still in
 * flux, and only `name` + `path` are load-bearing. Unknown fields are ignored and a malformed
 * payload yields an empty strip instead of throwing, so a bad response costs the chips rather
 * than the whole article.
 */
object SubNavTabsLoader {

    private const val TAG = "SubNavTabsLoader"
    private val CONFIG_TYPE = Constants.ConfigType.ELECTION_SUB_NAV_CONFIG

    /** Hilt does not inject into objects, so reach the app's configured client the way the
     *  widget factories do. Building a bare OkHttpClient here would skip
     *  [com.wapo.android.commons.retrofit.DefaultHeadersInterceptor] (CLIENT-APP, User-Agent),
     *  the app's TLS setup, timeouts, and debug logging/mocking. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NetworkEntryPoint {
        fun okHttpClient(): OkHttpClient
    }

    private fun client(context: Context): OkHttpClient =
        EntryPointAccessors
            .fromApplication(context.applicationContext, NetworkEntryPoint::class.java)
            .okHttpClient()

    /** Survives scrolling the element in and out; the disk copy survives process death. */
    private val memory = mutableMapOf<String, SubNavStrip>()

    /**
     * Best strip available without touching the network. Delegates to
     * [ConfigHelper.updateAndLoadConfig], which is the same call the old `loadLocalConfig` path
     * used: it returns the last good download from `filesDir`, falls back to the bundled raw
     * resource when there is no file, and clears the file when the app version code has changed
     * so an upgrade picks up that build's bundled copy.
     */
    suspend fun loadCachedOrBundled(context: Context, url: String?): SubNavStrip =
        withContext(Dispatchers.IO) {
            if (url != null) memory[url]?.let { return@withContext it }

            try {
                val json = ConfigHelper.updateAndLoadConfig(
                    context,
                    R.raw.section_election_config,
                    CONFIG_TYPE,
                ) ?: return@withContext SubNavStrip.EMPTY

                parse(json.toString()).also { strip ->
                    if (url != null && !strip.isEmpty) memory[url] = strip
                }
            } catch (t: Throwable) {
                Logger.e(TAG, "SubNav local config load error", t)
                SubNavStrip.EMPTY
            }
        }

    /**
     * Fetches the strip from [url] and, on success, writes it to disk for the next launch.
     * Returns [SubNavStrip.EMPTY] on any failure so the caller can keep showing tier 1 or 2.
     */
    suspend fun loadRemote(context: Context, url: String): SubNavStrip = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client(context).newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.d(TAG, "SubNav tabs fetch failed, code=${response.code}, url=$url")
                    return@use SubNavStrip.EMPTY
                }
                val body = response.body.string()
                val strip = parse(body)
                if (!strip.isEmpty) {
                    memory[url] = strip
                    writeToDisk(context, body)
                }
                strip
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "SubNav tabs fetch error for $url", t)
            SubNavStrip.EMPTY
        }
    }

    // ------------------------------------------------------------------------
    // DISK CACHE
    // ------------------------------------------------------------------------

    /**
     * Writes the downloaded payload to the same file [ConfigHelper] reads from, so the next
     * launch picks it up as tier 1. Reading, version invalidation and the raw-resource fallback
     * all stay in ConfigHelper rather than being reimplemented here.
     */
    private fun writeToDisk(context: Context, json: String) {
        try {
            File(context.filesDir, ConfigHelper.getFileName(CONFIG_TYPE)).writeText(json)
        } catch (t: Throwable) {
            Logger.e(TAG, "SubNav local config write error", t)
        }
    }

    // ------------------------------------------------------------------------
    // PARSING
    // ------------------------------------------------------------------------

    internal fun parse(json: String): SubNavStrip {
        if (json.isBlank()) return SubNavStrip.EMPTY
        return try {
            val section = JSONObject(json)
                .optJSONArray("children")
                ?.optJSONObject(0)
                ?: return SubNavStrip.EMPTY

            // "Election\n2024" in the feed — the newline is a web-layout artifact.
            val sectionName = section.optString("name").replace('\n', ' ').trim()
            val children = section.optJSONArray("children")

            val tabs = buildList {
                for (i in 0 until (children?.length() ?: 0)) {
                    val child = children?.optJSONObject(i) ?: continue
                    val label = child.optString("name").replace('\n', ' ').trim()
                    if (label.isEmpty()) continue
                    val path = child.optString("path").takeIf { it.isNotBlank() }
                    add(
                        SubNavTabUiModel(
                            id = child.optString("id").takeIf { it.isNotBlank() } ?: label,
                            label = label,
                            contentUrl = path,
                            subtype = child.optString("subtype").takeIf { it.isNotBlank() },
                            behavior = child.optString("behavior").takeIf { it.isNotBlank() },
                            iconName = child.optString("icon").takeIf { it.isNotBlank() },
                        )
                    )
                }
            }
            SubNavStrip(
                sectionLabel = sectionName.takeIf { it.isNotEmpty() },
                sectionIconName = section.optString("icon").takeIf { it.isNotBlank() },
                tabs = tabs,
            )
        } catch (t: Throwable) {
            Logger.e(TAG, "SubNav tabs parse error", t)
            SubNavStrip.EMPTY
        }
    }
}

/**
 * [sectionLabel] is the strip's leading title ("Elections 2026"); [tabs] are the selectable chips.
 */
data class SubNavStrip(
    val sectionLabel: String?,
    val sectionIconName: String? = null,
    val tabs: List<SubNavTabUiModel>,
) {
    val isEmpty: Boolean get() = sectionLabel == null && tabs.isEmpty()

    companion object {
        val EMPTY = SubNavStrip(sectionLabel = null, sectionIconName = null, tabs = emptyList())
    }
}
