package com.wapo.flagship.features.articles3.models

import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles3.models.ui.SubNavTabUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches the chips for a `sub_nav` element from the endpoint named by its `siteMap`.
 *
 * The payload is a site-service tree whose first child is the section and whose grandchildren
 * are the chips:
 * ```
 * { "children": [ { "name": "Election 2024",
 *                   "children": [ { "name": "Find results", "path": "…" }, … ] } ] }
 * ```
 *
 * Parsed with [JSONObject] rather than a generated adapter on purpose: the contract is still in
 * flux, and only `name` + `path` are load-bearing. Unknown fields are ignored and a malformed
 * payload yields an empty list instead of throwing, so a bad response degrades to "no chips"
 * rather than an article that fails to render.
 */
object SubNavTabsLoader {

    private const val TAG = "SubNavTabsLoader"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /** In-memory cache so scrolling the element in and out doesn't re-hit the network. */
    private val cache = mutableMapOf<String, SubNavStrip>()

    suspend fun load(url: String): SubNavStrip = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }

        val strip = try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.d(TAG, "SubNav tabs fetch failed, code=${response.code}, url=$url")
                    return@use SubNavStrip.EMPTY
                }
                parse(response.body.string())
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "SubNav tabs fetch error for $url", t)
            SubNavStrip.EMPTY
        }

        if (strip.tabs.isNotEmpty()) cache[url] = strip
        strip
    }

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
 * [sectionLabel] is the strip's leading title ("Election 2024"); [tabs] are the selectable chips.
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
