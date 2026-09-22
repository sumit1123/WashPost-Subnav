package com.wapo.flagship.features.articles3.models

import android.content.Context
import com.wapo.android.commons.config.ConfigHelper
import com.wapo.android.commons.config.ConfigManager
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.util.Logger
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
                        SubNavConfig::class.java,
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
            { config -> trySend(map(config as? SubNavConfig)) },
            { error -> Logger.e(TAG, "SubNav config stream error", Exception(error)) },
        )

        awaitClose { subscription.unsubscribe() }
    }.catch { t ->
        // A failure here costs the chips, never the article.
        Logger.e(TAG, "SubNav config flow error", Exception(t))
        emit(SubNavStrip.EMPTY)
    }

    /**
     * Splits the flat item list into the leading title (the entry marked `style: "bold"`) and
     * the chips. Falls back to treating the first entry as the title when nothing is marked,
     * which is how the bundled file is ordered.
     */
    private fun map(config: SubNavConfig?): SubNavStrip {
        val items = config?.items.orEmpty().filter { !it.name.isNullOrBlank() }
        if (items.isEmpty()) return SubNavStrip.EMPTY

        val titleIndex = items.indexOfFirst { it.style.equals("bold", ignoreCase = true) }
            .takeIf { it >= 0 } ?: 0
        val title = items[titleIndex]

        return SubNavStrip(
            sectionLabel = title.name?.replace('\n', ' ')?.trim(),
            sectionIconName = title.icon,
            // Ids fall back to the label when an entry carries no url, so a config with a
            // repeated url or label would hand LazyRow duplicate keys and crash the article.
            tabs = items.filterIndexed { i, _ -> i != titleIndex }
                .mapNotNull { it.toTab() }
                .distinctBy { it.id },
        )
    }

    private fun SubNavConfigItem.toTab(): SubNavTabUiModel? {
        val label = name?.replace('\n', ' ')?.trim().orEmpty()
        if (label.isEmpty()) return null
        return SubNavTabUiModel(
            id = url?.takeIf { it.isNotBlank() } ?: label,
            label = label,
            contentUrl = url?.takeIf { it.isNotBlank() },
            subtype = subtype,
            behavior = behavior,
            iconName = icon,
            children = children.orEmpty().mapNotNull { it.toTab() }.distinctBy { it.id },
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
