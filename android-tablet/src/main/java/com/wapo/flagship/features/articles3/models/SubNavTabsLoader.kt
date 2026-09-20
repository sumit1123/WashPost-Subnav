package com.wapo.flagship.features.articles3.models

import android.content.Context
import com.wapo.android.commons.config.ConfigManager
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.config.Section
import com.wapo.flagship.config.SiteServiceConfig
import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.features.articles3.models.ui.SubNavTabUiModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch

/**
 * Supplies the chips for a `sub_nav` element through the app's own config stack.
 *
 * [WapoConfigManager.loadElectionConfig] registers `R.raw.section_election_config` as the bundled
 * fallback alongside the element's `siteMap` url, loads whichever local copy exists (the last good
 * download, else the bundled resource, with the file cleared when the app version code changes),
 * then schedules a throttled remote refresh. Every stage lands on the same `BehaviorSubject`, so a
 * collector sees the local copy immediately and the remote one when it arrives.
 *
 * Disk cache, version invalidation, request headers and TLS are therefore the app's tested
 * implementations rather than a second set living here.
 *
 * Two limits are inherited from that stack and worth knowing:
 *  - `ConfigManager.configModelMap` is keyed by [Constants.ConfigType] alone, so two `sub_nav`
 *    elements with different `siteMap` urls share one slot and the later registration wins.
 *  - The remote refresh is throttled by a single timestamp in `ContentManager`, not per url.
 */
object SubNavTabsLoader {

    private const val TAG = "SubNavTabsLoader"
    private val CONFIG_TYPE = Constants.ConfigType.ELECTION_SUB_NAV_CONFIG

    /** Hilt does not inject into objects, so reach the singleton the way the widget factories do. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ConfigEntryPoint {
        fun wapoConfigManager(): WapoConfigManager
    }

    /**
     * Emits the strip whenever the underlying config changes — local copy first, then the remote
     * one once it downloads. Emits [SubNavStrip.EMPTY] and completes if the config subject is
     * unavailable.
     *
     * The RxJava 1 subscription is tied to the flow's lifetime via [awaitClose], so leaving the
     * article unsubscribes. The old `SubNavViewHolder` subscribed in `bind()` and never
     * unsubscribed, leaking a subscriber per rebind.
     */
    fun strips(context: Context, siteMapUrl: String): Flow<SubNavStrip> = callbackFlow {
        val configManager = EntryPointAccessors
            .fromApplication(context.applicationContext, ConfigEntryPoint::class.java)
            .wapoConfigManager()

        // Registers the config model, loads the local copy and schedules the remote refresh.
        // Must run before getConfigSubjectOfType, which returns null for an unregistered type.
        configManager.loadElectionConfig(context, CONFIG_TYPE, siteMapUrl)

        val subject = ConfigManager.instance()?.getConfigSubjectOfType(CONFIG_TYPE)
        if (subject == null) {
            Logger.d(TAG, "SubNav config subject missing for $CONFIG_TYPE")
            trySend(SubNavStrip.EMPTY)
            close()
            return@callbackFlow
        }

        val subscription = subject.subscribe(
            { config -> trySend(map(config as? SiteServiceConfig)) },
            { error -> Logger.e(TAG, "SubNav config stream error", Exception(error)) },
        )

        awaitClose { subscription.unsubscribe() }
    }.catch { t ->
        // A failure here costs the chips, never the article.
        Logger.e(TAG, "SubNav config flow error", Exception(t))
        emit(SubNavStrip.EMPTY)
    }

    /**
     * Flattens the site-service tree into the strip: the first child is the section, its own
     * children are the chips.
     */
    private fun map(config: SiteServiceConfig?): SubNavStrip {
        val section = config?.sections?.firstOrNull() ?: return SubNavStrip.EMPTY

        // "Election\n2024" in the feed — the newline is a web-layout artifact.
        val label = section.sectionName.replace('\n', ' ').trim()
        val tabs = section.sections.orEmpty().mapNotNull { it.toTab() }

        return SubNavStrip(
            sectionLabel = label.takeIf { it.isNotEmpty() },
            sectionIconName = section.icon,
            tabs = tabs,
        )
    }

    private fun Section.toTab(): SubNavTabUiModel? {
        val label = sectionName.replace('\n', ' ').trim()
        if (label.isEmpty()) return null
        return SubNavTabUiModel(
            id = sectionId.takeIf { it.isNotBlank() } ?: label,
            label = label,
            // Link children carry `path`; `path_fusion` is only set on real sections.
            contentUrl = sectionPath?.takeIf { it.isNotBlank() }
                ?: fusionPath?.takeIf { it.isNotBlank() },
            subtype = sectionSubType,
            behavior = behavior,
            iconName = icon,
        )
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
