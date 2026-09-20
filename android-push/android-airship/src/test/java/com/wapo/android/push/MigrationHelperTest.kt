package com.wapo.android.push


import com.wapo.android.commons.logs.EventLog
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters
import com.washingtonpost.android.config.data.datasources.utils.SafeBooleanAdapter
import com.washingtonpost.android.config.domain.models.config.PushConfigStub
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

internal class MigrationHelperTest{

    internal data class TopicUi(
        var enabled:Boolean = false,
        var isHidden:Boolean = false
    )

    val pushCurrent:PushConfigStub = getPushConfig(currentJson)

    val pushSync:PushConfigStub = getPushConfig(syncJson)

    val pushMigrate:PushConfigStub = getPushConfig(migrateJson)

    var liveConfig:PushConfigStub = pushCurrent

    lateinit var migrationHelper: MigrationHelper

    var prefAlertsMap = mutableMapOf<String, TopicUi>()

    private fun launchApp(configStub: PushConfigStub, fresh:Boolean = false) {
        if(fresh) {
            prefAlertsMap.clear()
        }

        liveConfig = configStub
        configStub.availableSubscriptionTopics.forEach {
            if(prefAlertsMap.containsKey(it.key)) {
                prefAlertsMap[it.key]?.isHidden = it.isHidden
            } else {
                prefAlertsMap[it.key] = TopicUi(false, it.isHidden)
            }
        }
        migrationHelper.migrateSyncSegments(liveConfig)
    }

    private fun enableAlert(key:String) {
        prefAlertsMap[key]?.enabled = true
        migrationHelper.syncSegment(key, true, liveConfig)
    }

    private fun disableAlert(key:String) {
        prefAlertsMap[key]?.enabled = false
        migrationHelper.syncSegment(key, false, liveConfig)
    }

    private fun printAlerts(header:String) {
        println("-------------$header------------")
        prefAlertsMap.forEach{
            println("${it.key} : enabled = ${it.value.enabled} | hidden = ${it.value.isHidden} ")
        }
    }

    @Before
    fun setup() {
        migrationHelper = object: MigrationHelper() {
            override fun updateAlertsTopic(key: String, isEnabled: Boolean) {
                prefAlertsMap[key]?.enabled = isEnabled
            }

            override fun isTopicEnabled(key: String): Boolean {
                return prefAlertsMap[key]?.enabled ?: false
            }

            override fun remoteLog(eventLogBuilder: EventLog.Builder?) {
                // Do nothing
            }
        }
    }

    @Test
    fun `from current to sync test`() {
        // Launch App with existing config (No migrated topics)
        launchApp(pushCurrent, true)
        printAlerts("current config")
        // business_and_tech will be false by default
        assert(prefAlertsMap[BUS_TECH_KEY]?.enabled == false)
        enableAlert(BUS_TECH_KEY)
        printAlerts("current config | enable business_and_tech")
        // enabled alert will be true
        assert(prefAlertsMap[BUS_TECH_KEY]?.enabled == true)

        // Launch App with updated config (Hidden Migrated topics)
        launchApp(pushSync)
        printAlerts("sync config")
        // Migrated topics (business and tech) are hidden and synced with deprecated (business_and_tech) which is visible
        assert(prefAlertsMap[BUS_KEY]?.enabled == true && prefAlertsMap[TECH_KEY]?.enabled == true)
        assert(prefAlertsMap[BUS_KEY]?.isHidden == true && prefAlertsMap[TECH_KEY]?.isHidden == true && prefAlertsMap[BUS_TECH_KEY]?.isHidden == false)

        disableAlert(BUS_TECH_KEY)
        printAlerts("sync config | disable business_and_tech")
        // Migrated topics (business and tech) are hidden and synced with deprecated (business_and_tech) which is visible
        assert(prefAlertsMap[BUS_KEY]?.enabled == false && prefAlertsMap[TECH_KEY]?.enabled == false)
        assert(prefAlertsMap[BUS_KEY]?.isHidden == true && prefAlertsMap[TECH_KEY]?.isHidden == true && prefAlertsMap[BUS_TECH_KEY]?.isHidden == false)
    }

    @Test
    fun `from sync to migrate test`() {
        // Launch App with sync config (Hidden Migrated topics)
        launchApp(pushSync, true)
        printAlerts("sync config")
        // business_and_tech will be false by default
        assert(prefAlertsMap[BUS_TECH_KEY]?.enabled == false)
        enableAlert(BUS_TECH_KEY)
        printAlerts("sync config | enable business_and_tech")
        // Migrated topics (business and tech) are hidden and synced with deprecated (business_and_tech) which is visible
        assert(prefAlertsMap[BUS_KEY]?.enabled == true && prefAlertsMap[TECH_KEY]?.enabled == true && prefAlertsMap[BUS_TECH_KEY]?.enabled == true)
        assert(prefAlertsMap[BUS_KEY]?.isHidden == true && prefAlertsMap[TECH_KEY]?.isHidden == true && prefAlertsMap[BUS_TECH_KEY]?.isHidden == false)

        // Launch App with final config (Hidden deprecated topic)
        launchApp(pushMigrate)
        printAlerts("final config")
        // Migrated topics (business and tech) are now visible (and correctly enabled) and deprecated (business_and_tech) is hidden and set to false
        assert(prefAlertsMap[BUS_KEY]?.enabled == true && prefAlertsMap[TECH_KEY]?.enabled == true && prefAlertsMap[BUS_TECH_KEY]?.enabled == false)
        assert(prefAlertsMap[BUS_KEY]?.isHidden == false && prefAlertsMap[TECH_KEY]?.isHidden == false && prefAlertsMap[BUS_TECH_KEY]?.isHidden == true)
    }

    @Test
    fun `from current to migrate test`() {
        // Launch App with existing config (No migrated topics)
        launchApp(pushCurrent, true)
        printAlerts("current config")
        // business_and_tech will be false by default
        assert(prefAlertsMap[BUS_TECH_KEY]?.enabled == false)
        enableAlert(BUS_TECH_KEY)
        printAlerts("current config | enable business_and_tech")

        // Launch App with final config (Hidden deprecated topic)
        launchApp(pushMigrate)
        printAlerts("final config")
        // Migrated topics (business and tech) are now visible (and correctly enabled) and deprecated (business_and_tech) is hidden and set to false
        assert(prefAlertsMap[BUS_KEY]?.enabled == true && prefAlertsMap[TECH_KEY]?.enabled == true && prefAlertsMap[BUS_TECH_KEY]?.enabled == false)
        assert(prefAlertsMap[BUS_KEY]?.isHidden == false && prefAlertsMap[TECH_KEY]?.isHidden == false && prefAlertsMap[BUS_TECH_KEY]?.isHidden == true)
    }



    private fun getPushConfig(json:String) : PushConfigStub {
        val adapter = ConfigMoshiAdapters.moshi.adapter(PushConfigStub::class.java)
        return adapter.fromJson(json)!!
    }


    companion object{

        private const val BUS_TECH_KEY = "business_and_tech"
        private const val BUS_KEY = "business"
        private const val TECH_KEY = "tech"
        private const val HEALTH_SCIENCE_KEY = "health_and_science"
        private const val HEALTH_KEY = "health"
        private const val SCIENCE_KEY = "science"

        private const val currentJson = "" +
                "{\n" +
                "        \"groups\": [\n" +
                "            {\n" +
                "                \"id\": \"latest\",\n" +
                "                \"label\": \"Latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"sections\",\n" +
                "                \"label\": \"Sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"oftp\",\n" +
                "                \"label\": \"Only From The Post\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"availableSubscriptionTopics\": [\n" +
                "            {\n" +
                "                \"id\": \"breaking-news\",\n" +
                "                \"displayName\": \"Breaking News\",\n" +
                "                \"key\": \"breaking-news\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"bn\",\n" +
                "                \"imageName\": \"alert_breaking_news\",\n" +
                "                \"description\": \"Several alerts daily\",\n" +
                "                \"group\": \"latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"editors_picks\",\n" +
                "                \"displayName\": \"Editors' Picks\",\n" +
                "                \"key\": \"editors_picks\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"ep\",\n" +
                "                \"imageName\": \"alert_editors_picks\",\n" +
                "                \"description\": \"Several alerts weekly\",\n" +
                "                \"group\": \"oftp\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"the7_briefs\",\n" +
                "                \"displayName\": \"The 7 Briefing\",\n" +
                "                \"key\": \"the7_briefs\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"7b\",\n" +
                "                \"imageName\": \"alert_the7_briefs\",\n" +
                "                \"description\": \"One alert daily\",\n" +
                "                \"group\": \"latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"health_and_science\",\n" +
                "                \"displayName\": \"Health and Science\",\n" +
                "                \"key\": \"health_and_science\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"business_and_tech\",\n" +
                "                \"key\": \"business_and_tech\",\n" +
                "                \"displayName\": \"Business & Tech\",\n" +
                "                \"alias\": \"bt\",\n" +
                "                \"imageName\": \"alert_business_and_tech\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\"\n" +
                "            }\n" +
                "        ]\n" +
                "    },"


        private const val syncJson = "" +
                "{\n" +
                "        \"groups\": [\n" +
                "            {\n" +
                "                \"id\": \"latest\",\n" +
                "                \"label\": \"Latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"sections\",\n" +
                "                \"label\": \"Sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"oftp\",\n" +
                "                \"label\": \"Only From The Post\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"availableSubscriptionTopics\": [\n" +
                "            {\n" +
                "                \"id\": \"breaking-news\",\n" +
                "                \"displayName\": \"Breaking News\",\n" +
                "                \"key\": \"breaking-news\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"bn\",\n" +
                "                \"imageName\": \"alert_breaking_news\",\n" +
                "                \"description\": \"Several alerts daily\",\n" +
                "                \"group\": \"latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"editors_picks\",\n" +
                "                \"displayName\": \"Editors' Picks\",\n" +
                "                \"key\": \"editors_picks\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"ep\",\n" +
                "                \"imageName\": \"alert_editors_picks\",\n" +
                "                \"description\": \"Several alerts weekly\",\n" +
                "                \"group\": \"oftp\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"the7_briefs\",\n" +
                "                \"displayName\": \"The 7 Briefing\",\n" +
                "                \"key\": \"the7_briefs\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"7b\",\n" +
                "                \"imageName\": \"alert_the7_briefs\",\n" +
                "                \"description\": \"One alert daily\",\n" +
                "                \"group\": \"latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"health_and_science\",\n" +
                "                \"displayName\": \"Health and Science\",\n" +
                "                \"key\": \"health_and_science\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"health\",\n" +
                "                \"displayName\": \"Health\",\n" +
                "                \"key\": \"health\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_health\",\n" +
                "                \"reference\": \"health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\",\n" +
                "                \"hide\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"climate\",\n" +
                "                \"displayName\": \"Climate\",\n" +
                "                \"key\": \"climate\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_climate\",\n" +
                "                \"reference\": \"health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\",\n" +
                "                \"hide\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"business_and_tech\",\n" +
                "                \"key\": \"business_and_tech\",\n" +
                "                \"displayName\": \"Business & Tech\",\n" +
                "                \"alias\": \"bt\",\n" +
                "                \"imageName\": \"alert_business_and_tech\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"business\",\n" +
                "                \"key\": \"business\",\n" +
                "                \"alias\": \"bs\",\n" +
                "                \"reference\": \"business_and_tech\",\n" +
                "                \"imageName\": \"alert_business_and_tech\",\n" +
                "                \"group\": \"sections\",\n" +
                "                \"hide\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"teçh\",\n" +
                "                \"key\": \"tech\",\n" +
                "                \"alias\": \"tc\",\n" +
                "                \"reference\": \"business_and_tech\",\n" +
                "                \"imageName\": \"alert_technology\",\n" +
                "                \"group\": \"sections\",\n" +
                "                \"hide\": true\n" +
                "            }\n" +
                "        ]\n" +
                "    },"

        private const val migrateJson = "" +
                "{\n" +
                "        \"groups\": [\n" +
                "            {\n" +
                "                \"id\": \"latest\",\n" +
                "                \"label\": \"Latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"sections\",\n" +
                "                \"label\": \"Sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"oftp\",\n" +
                "                \"label\": \"Only From The Post\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"availableSubscriptionTopics\": [\n" +
                "            {\n" +
                "                \"id\": \"breaking-news\",\n" +
                "                \"displayName\": \"Breaking News\",\n" +
                "                \"key\": \"breaking-news\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"bn\",\n" +
                "                \"imageName\": \"alert_breaking_news\",\n" +
                "                \"description\": \"Several alerts daily\",\n" +
                "                \"group\": \"latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"editors_picks\",\n" +
                "                \"displayName\": \"Editors' Picks\",\n" +
                "                \"key\": \"editors_picks\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"ep\",\n" +
                "                \"imageName\": \"alert_editors_picks\",\n" +
                "                \"description\": \"Several alerts weekly\",\n" +
                "                \"group\": \"oftp\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"the7_briefs\",\n" +
                "                \"displayName\": \"The 7 Briefing\",\n" +
                "                \"key\": \"the7_briefs\",\n" +
                "                \"isOptional\": false,\n" +
                "                \"alias\": \"7b\",\n" +
                "                \"imageName\": \"alert_the7_briefs\",\n" +
                "                \"description\": \"One alert daily\",\n" +
                "                \"group\": \"latest\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"health_and_science\",\n" +
                "                \"displayName\": \"Health and Science\",\n" +
                "                \"key\": \"health_and_science\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\",\n" +
                "                \"hide\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"health\",\n" +
                "                \"displayName\": \"Health\",\n" +
                "                \"key\": \"health\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_health\",\n" +
                "                \"reference\": \"health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"climate\",\n" +
                "                \"displayName\": \"Climate\",\n" +
                "                \"key\": \"climate\",\n" +
                "                \"alias\": \"hs\",\n" +
                "                \"imageName\": \"alert_climate\",\n" +
                "                \"reference\": \"health_and_science\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"business_and_tech\",\n" +
                "                \"key\": \"business_and_tech\",\n" +
                "                \"displayName\": \"Business & Tech\",\n" +
                "                \"alias\": \"bt\",\n" +
                "                \"imageName\": \"alert_business_and_tech\",\n" +
                "                \"description\": \"A few alerts weekly\",\n" +
                "                \"group\": \"sections\",\n" +
                "                \"hide\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"business\",\n" +
                "                \"key\": \"business\",\n" +
                "                \"alias\": \"bs\",\n" +
                "                \"reference\": \"business_and_tech\",\n" +
                "                \"imageName\": \"alert_business_and_tech\",\n" +
                "                \"group\": \"sections\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"teçh\",\n" +
                "                \"key\": \"tech\",\n" +
                "                \"alias\": \"tc\",\n" +
                "                \"reference\": \"business_and_tech\",\n" +
                "                \"imageName\": \"alert_technology\",\n" +
                "                \"group\": \"sections\"\n" +
                "            }\n" +
                "        ]\n" +
                "    },"
    }

}