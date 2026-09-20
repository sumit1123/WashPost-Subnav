package com.wapo.flagship.features.articles3.models

import android.content.Context
import com.wapo.android.commons.config.ConfigHelper
import com.wapo.android.commons.config.ConfigManager
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.config.Section
import com.wapo.flagship.config.SiteServiceConfig
import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.features.articles3.models.ui.SubNavTabUiModel
import com.washingtonpost.android.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch

object SubNavTabsLoader {

    private const val TAG = "SubNavTabsLoader"
    private val CONFIG_TYPE = Constants.ConfigType.ELECTION_SUB_NAV_CONFIG

    /**
     * Testing switch. When true the remote `siteMap` is ignored and the strip always renders
     * `R.raw.section_election_config`, so edits to that file show up without a server serving
     * the matching tree. Also clears any previously downloaded copy, which would otherwise win
     * as tier 1.
     *
     * Leave false in anything that ships.
     */
    private const val USE_BUNDLED_CONFIG_ONLY = true

    /** Hilt does not inject into objects, so reach the singleton the way the widget factories do. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ConfigEntryPoint {
        fun wapoConfigManager(): WapoConfigManager
    }

    fun strips(context: Context, siteMapUrl: String): Flow<SubNavStrip> = callbackFlow {
        val configManager = EntryPointAccessors
            .fromApplication(context.applicationContext, ConfigEntryPoint::class.java)
            .wapoConfigManager()

        // Registers the config model, loads the local copy and schedules the remote refresh.
        // Must run before getConfigSubjectOfType, which returns null for an unregistered type.
        if (USE_BUNDLED_CONFIG_ONLY) {
            ConfigHelper.clearOldConfig(context, CONFIG_TYPE)
            ConfigManager.instance()?.apply {
                addConfigModel(
                    CONFIG_TYPE,
                    ConfigManager.ConfigModel(
                        SiteServiceConfig::class.java,
                        R.raw.section_election_config,
                        null,
                    ),
                )
                loadLocalConfig(context, CONFIG_TYPE)
            }
        } else {
            configManager.loadElectionConfig(context, CONFIG_TYPE, siteMapUrl)
        }

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
            // A chip with its own children is a dropdown; the tree already nests this way, so
            // no new contract field is needed to mark one.
            children = sections.orEmpty().mapNotNull { it.toTab() },
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
