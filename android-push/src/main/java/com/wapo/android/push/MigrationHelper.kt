package com.wapo.android.push

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.washingtonpost.android.config.domain.models.config.PushConfigStub
import com.washingtonpost.android.config.domain.models.config.SubscriptionTopic

/**
 * Helper class for migration push segments. Currently supports the migration of
 * single segment to one or more segments.
 */
abstract class MigrationHelper {

    private var migrateMap:Map<String, List<String>>? = null

    /**
     * Get list of segments that will be migrated. It is based on topic having [reference] to deprecated topic
     */
    private fun getMigrationMap(config: PushConfigStub): Map<String, List<String>> {
        // Filter list of all topics with reference
        val refList = config.availableSubscriptionTopics.filter { it.reference != null }
        val migrateMap = mutableMapOf<String, List<String>>()
        refList.map { it.reference }.distinct().forEach { refKey ->
            // Check if reference topic exists in list of available topics.
            val refTopicExists = config.availableSubscriptionTopics.find { it.key == refKey } != null
            if(refTopicExists) {
                // Add list of migration topics to map for given reference topic
                migrateMap[refKey] = refList.filter { it.reference == refKey }.map { it.key }
            }
        }
        return migrateMap
    }

    /**
     * Sync segment when user clicks on toggle.
     */
    fun syncSegment(fromKey: String, isEnabled: Boolean, config: PushConfigStub) {
        migrateMap?.get(fromKey)?.let { toKeys ->
            val toTopicsInList = config.availableSubscriptionTopics.filter { toKeys.contains(it.key) }
            syncSegments(toTopicsInList, isEnabled, fromKey)
        }
    }

    /**
     * Migrate all segments that have reference
     */
    fun migrateSyncSegments(config: PushConfigStub) {
        migrateMap = getMigrationMap(config)
        migrateMap?.forEach {
            migrateSyncSegment(it.key, it.value, config.availableSubscriptionTopics)
        }
    }

    /**
     * Migrate a specific segment to a list of other segments.
     */
    private fun migrateSyncSegment(
        fromKey: String,
        toKey: List<String>,
        topicList: List<SubscriptionTopic>
    ) {
        val isFromEnabled = isTopicEnabled(fromKey)
        val fromTopicInList = topicList.find { it.key == fromKey }
        val isFromVisible = fromTopicInList?.let {
            !it.isHidden
        } ?: false
        val toTopicsInList = topicList.filter { toKey.contains(it.key) }

        // if From (deprecated topic) is visible, the To (migrated topics)
        // are assumed to be invisible and so we keep the topics in sync
        if (isFromVisible) {
            syncSegments(toTopicsInList, isFromEnabled, fromKey)
            return
        }

        // Check to see if deprecated topic is enabled
        // AND if migrated topics exist in config
        if (!isFromEnabled || toTopicsInList.isEmpty()) {
            return
        }

        // At this point, deprecated topic is no longer visible so we do one last migration
        migrateSegments(fromKey, toTopicsInList)
    }

    /**
     * Migrate segments one last time when deprecated segment is no longer visible
     */
    private fun migrateSegments(
        fromKey: String,
        toTopicsInList: List<SubscriptionTopic>
    ) {
        // Disable deprecated topic
        updateAlertsTopic(fromKey, false)

        // Enable migrated topic(s) for the last time.
        toTopicsInList.forEach {
            updateAlertsTopic(it.key, true)
        }

        //Log Migrate
        remoteLogMigrateStatus(
            MigrateStatus.FINAL,
            Pair(fromKey, isTopicEnabled(fromKey)),
            toTopicsInList.map { Pair(it.key, isTopicEnabled(it.key)) })
    }

    /**
     * Sync Segments as long as deprecated Topic is still visible
     */
    private fun syncSegments(
        toTopicsInList: List<SubscriptionTopic>,
        isFromEnabled: Boolean,
        fromKey: String
    ) {
        var logSync = toTopicsInList.firstOrNull()?.key?.let {
            isTopicEnabled(it) != isFromEnabled
        } ?: false

        // Enable migrated topic(s)
        toTopicsInList.forEach {
            updateAlertsTopic(it.key, isFromEnabled)
        }

        // Log Sync
        remoteLogMigrateStatus(
            MigrateStatus.SYNC,
            Pair(fromKey, isFromEnabled),
            toTopicsInList.map { Pair(it.key, isTopicEnabled(it.key)) },
            logSync
        )
    }

    /**
     * Log migration to remote to track users in sync or migration status.
     */
    private fun remoteLogMigrateStatus(
        status: MigrateStatus,
        fromTopic: Pair<String, Boolean>,
        toTopics: List<Pair<String, Boolean>>,
        shouldLog: Boolean = true
    ) {
        if (!shouldLog) {
            return
        }
        val fromMessage = "${fromTopic.first}:${fromTopic.second}"
        var toMessage = ""
        toTopics.forEachIndexed { index, pair ->
            toMessage += "${pair.first}:${pair.second}"
            toMessage += if (toTopics.lastIndex == index) "" else "|"
        }

        EventLog.Builder().apply {
            setMessage("Push Migration Status")
            setModule(LogModules.ALERTS)
            set("id", status.id)
            set("from_topic", fromMessage)
            set("to_topics", toMessage)
        }.run {
            remoteLog(this)
        }
    }

    abstract fun updateAlertsTopic(key: String, isEnabled: Boolean)

    abstract fun isTopicEnabled(key: String): Boolean

    abstract fun remoteLog(eventLogBuilder: EventLog.Builder?)

    internal enum class MigrateStatus(val id: String) {
        NONE("none"),
        SYNC("sync"),
        FINAL("final")
    }
}