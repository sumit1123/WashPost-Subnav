package com.wapo.flagship.features.preferencesapi.state

import android.content.Context
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

object PreferencesSyncCoordinator {

    const val CONTENT_PACKS = "content-packs"
    const val NEWSPRINT_ATTRIBUTES = "newsprint-attributes"
    const val NEWSPRINT_STATE = "newsprint-state"
    const val WALL_DISMISSAL = "wall-dismissal"
    const val TOPIC_NOTIFICATIONS = "topic-notifications"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val supported = listOf(
        CONTENT_PACKS,
        NEWSPRINT_ATTRIBUTES,
        NEWSPRINT_STATE,
        WALL_DISMISSAL,
        TOPIC_NOTIFICATIONS
    )

    private var running = AtomicBoolean(false)
    fun isRunning() = running.get()
    fun guard(scope: CoroutineScope, block: suspend () -> Unit) {
        if (!running.compareAndSet(false, true)) return
        scope.launch {
            try {
                block()
            } finally {
                running.set(false)
            }
        }
    }

    fun markDirty(name: String) {
        if (name in supported) {
            AppContext.markPreferenceDirty(name)
        }
    }

    private fun clearDirty(name: String) {
        AppContext.clearPreferenceDirty(name)
    }

    fun dirty(context: Context): List<String> = AppContext.getDirtyPreferences().toList()

    private fun globalLastModified(context: Context): Long = PrefUtils.getCheckPrefLmt(context)
    fun setGlobalLastModified(context: Context, value: Long) =
        PrefUtils.setCheckPrefLmt(context, value)

    fun stale(context: Context): List<String> {
        val global = globalLastModified(context)
        return supported.filter { name ->
            val prefUpdated = when (name) {
                CONTENT_PACKS -> PrefUtils.getContentPacksLmt(context)
                NEWSPRINT_ATTRIBUTES -> PrefUtils.getNewsprintAttributesLmt(context)
                NEWSPRINT_STATE -> PrefUtils.getNewsprintStateLmt(context)
                WALL_DISMISSAL -> PrefUtils.getWallDismissalLmt(context)
                TOPIC_NOTIFICATIONS -> PrefUtils.getTopicNotificationsLmt(context)
                else -> 0L
            }
            prefUpdated < global
        }
    }

    fun updateFromRemote(context: Context, name: String, remoteLastUpdated: Long) {
        val timestamp = maxOf(remoteLastUpdated, globalLastModified(context))
        when (name) {
            CONTENT_PACKS -> PrefUtils.setContentPacksLmt(context, timestamp)
            NEWSPRINT_ATTRIBUTES -> PrefUtils.setNewsprintAttributesLmt(context, timestamp)
            NEWSPRINT_STATE -> PrefUtils.setNewsprintStateLmt(context, timestamp)
            WALL_DISMISSAL -> PrefUtils.setWallDismissalLmt(context, timestamp)
            TOPIC_NOTIFICATIONS -> PrefUtils.setTopicNotificationsLmt(context, timestamp)
        }
        clearDirty(name)
    }

    // --- Master Sync ---
    fun synchronize(context: Context) {
        if (!ReachabilityUtil.isConnected(context)) return
        if (isRunning()) return

        val hasDirty = dirty(context).isNotEmpty()
        val hasStale = stale(context).isNotEmpty()
        if (!hasDirty && !hasStale) return

        guard(scope) {
            pushDirty(context)
            fetchStale(context)
        }
    }

    private suspend fun pushDirty(context: Context) {
        dirty(context).forEach { name ->
            when (name) {
                CONTENT_PACKS -> {
                    val localSelection = PrefUtils.getSelectedContentPacks(context) ?: emptyList()
                    FlagshipApplication.getInstance().contentPacksRepo.setUserContentPacks(
                        localSelection
                    )
                }

                WALL_DISMISSAL -> {
                    val localMap = PrefUtils.getWallDismissal(context)
                    FlagshipApplication.getInstance().wallDismissalRepo.setWallDismissal(localMap)
                }

                TOPIC_NOTIFICATIONS -> {
                    TopicNotificationsRepo.getInstance().syncTopicsWithPreferencesApi()
                }
            }
        }
    }

    private suspend fun fetchStale(context: Context) {
        stale(context).forEach { name ->
            when (name) {
                CONTENT_PACKS -> FlagshipApplication.getInstance().contentPacksRepo.getUserContentPacks()
                NEWSPRINT_ATTRIBUTES -> FlagshipApplication.getInstance().newsprintRepo.getUserNewsprintAttributes()
                NEWSPRINT_STATE -> FlagshipApplication.getInstance().newsprintRepo.getUserNewsprintState()
                WALL_DISMISSAL -> FlagshipApplication.getInstance().wallDismissalRepo.getWallDismissal()
                TOPIC_NOTIFICATIONS -> TopicNotificationsRepo.getInstance()
                    .syncTopicsWithPreferencesApi(isStale = true)
            }
        }
    }
}