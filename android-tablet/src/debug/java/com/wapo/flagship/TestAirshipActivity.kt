package com.wapo.flagship

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.urbanairship.push.PushMessage
// import com.urbanairship.push.adm.AdmPushProvider
// import com.urbanairship.push.fcm.FcmPushProvider
import com.wapo.android.push.PushNotification
import com.wapo.android.push.PushService
import com.washingtonpost.android.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Class to test PushListener and UAirshipNotificationProvider classes
 * Note: AdmPushProvider & FcmPushProvider imports and related snippets are commented out by default.
 *       Un-comment any one of them while testing specific flavors.
 */
class TestAirshipActivity : AppCompatActivity() {
    enum class Provider {
        FCM,
        ADM,
    }

    companion object {
        const val TEST_URL = "https://www.washingtonpost.com/world/bolivia-to-hold-new-elections-after-protests-and-international-criticism/2019/11/10/4778e842-03b2-11ea-ac12-3325d49eacaa_story.html"
        const val TEST_DEV_URL = "https://dev.washpost.arcpublishing.com/pb/technology/2020/08/25/future-autonomous-delivery-may-be-unfolding-an-unlikely-place-suburban-houston/"
        const val TEST_IMAGE_URL = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/3UUG4JSEFMI6VGOHDX6UEQNC7Y.jpg"
        const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        val CURRENT_TIME_MILLIS = System.currentTimeMillis()
        val CURRENT_TIME_IN_WP_FORMAT =
            SimpleDateFormat(DATE_FORMAT)
                .apply {
                    timeZone =
                        TimeZone.getTimeZone(
                            "UTC",
                        )
                }.format(Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_airship)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.send_push_pushservice -> {
                createPushWithPushService()
            }
            R.id.send_segmented_push_pushservice -> {
                createSegmentedPushWithPushService()
            }
            R.id.send_mmp_push_pushservice -> {
                createMmpPushWithPushService()
            }
            R.id.send_mmp_push_airshipfcm -> {
                createMmpPushWithAirshipFcm(this)
            }
            R.id.send_airship_push_airshipfcm -> {
                createPushWithAirshipFcm(this)
            }
            R.id.send_revere_push_airshipfcm -> {
                createReverePushWithAirshipFcm(this)
            }
            R.id.send_revere_segmented_push_airshipfcm -> {
                createRevereSegmentedPushWithAirshipFcm(this)
            }
            R.id.send_airship_push_airshipadm -> {
                createPushWithAirshipAdm(this)
            }
            R.id.send_revere_push_airshipadm -> {
                createReverePushWithAirshipAdm(this)
            }
            R.id.send_revere_segmented_push_airshipadm -> {
                createRevereSegmentedPushWithAirshipAdm(this)
            }
        }
    }

    /**
     * to test PushListener onMessage method with DEFAULT/BREAKING-NEWS PushNotification object
     */
    private fun createPushWithPushService() {
        PushService.getInstance().listener.onMessage(
            Random.nextInt(),
            PushNotification(
                JSONObject().apply {
                    put(PushNotification.PARAM_TITLE, "The Washington Post")
                    put(
                        PushNotification.PARAM_HEADLINE,
                        "Perspective: These hearings will be the trickiest test of covering Trump",
                    )
                    // put(PushNotification.PARAM_BLURB, "The national media’s shortcomings have been all too obvious in recent years.") // fallback to Article's blurb when it is missing in push response
                    put(PushNotification.PARAM_TARGET_TOPIC, "breaking-news")
                    put(PushNotification.PARAM_URL, TEST_URL)
                    // put(PushNotification.PARAM_IMAGE_URL, TEST_IMAGE_URL) // fallback to Article's lead imageURL when it is missing in push response
                    put(PushNotification.PARAM_ANALYTICS_ID, "classic:android:breaking-news")
                    put(PushNotification.PARAM_SENT_TIMESTAMP, "$CURRENT_TIME_MILLIS")
                },
            ),
        )
    }

    /**
     * to test PushListener onMessage method with SEGMENTED PushNotification object
     */
    private fun createSegmentedPushWithPushService() {
        PushService.getInstance().listener.onMessage(
            Random.nextInt(),
            PushNotification(
                JSONObject().apply {
                    put(PushNotification.PARAM_TITLE, "The Washington Post")
                    put(
                        PushNotification.PARAM_HEADLINE,
                        "Perspective: These hearings will be the trickiest test of covering Trump",
                    )
                    put(PushNotification.PARAM_TARGET_TOPIC, "breaking-news")
                    put(PushNotification.PARAM_URL, TEST_URL)
                    put(PushNotification.PARAM_ANALYTICS_ID, "classic:android:breaking-news")
                    put(PushNotification.PARAM_SENT_TIMESTAMP, "$CURRENT_TIME_MILLIS")
                    put(
                        PushNotification.PARAM_INTERACTION_TYPE,
                        PushNotification.InteractionType.SEGMENTED,
                    )
                    put(
                        PushNotification.PARAM_SEGMENTED_IMAGES,
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("name", "A Name")
                                    put(
                                        "imageURL",
                                        "https://dohdeick6sqa6.cloudfront.net/screenshots/2018-test/general/head-to-head/house@lg.png",
                                    )
                                },
                            )
                            put(
                                JSONObject().apply {
                                    put("name", "B Name")
                                    put(
                                        "imageURL",
                                        "https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png",
                                    )
                                },
                            )
                            put(
                                JSONObject().apply {
                                    put("name", "C Name")
                                    put(
                                        "imageURL",
                                        "https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png",
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )
    }

    private fun jsonString(): String =
        """
         { "title": "The Washington Post",
        "message": "Article",
         "headline": "Perspective: These hearings will be the trickiest test of covering Trump",
          "targetTopic": "breaking-news",
          "type": "news-alert",
           "url": "$TEST_URL",
           "analyticsTopic":"Local:GCM",
            "datetime": "${System.currentTimeMillis()}" } 
        """.trimIndent()

    /**
     * to test PushListener old onMessage(deprecated) method with given Intent.
     * won't work as this method is empty in PushListener now
     */
    private fun createMmpPushWithPushService() {
        PushService.getInstance().listener.onMessage(
            Intent().apply {
                putExtra("default", jsonString())
            },
        )
    }

    /**
     * to test mmp push with UAirshipNotificationProvider class.
     * Expectation is that provider class should not process this non-airship/non-revere message
     */
    private fun createMmpPushWithAirshipFcm(context: Context) {
        val map = mutableMapOf<String, String>()
        map["google.delivered_priority"] = "normal"
        map["google.sent_time"] = "$CURRENT_TIME_MILLIS"
        map["google.ttl"] = "2419200"
        map["default"] = jsonString()
        sendPush(this, Provider.FCM, map)
    }

    /**
     * to test airship's direct push with UAirshipNotificationProvider class.
     * Expectation is that provider class should not process this non-airship/non-revere message
     */
    private fun createPushWithAirshipFcm(context: Context) {
        val map = mutableMapOf<String, String>()
        map["google.delivered_priority"] = "normal"
        map[PushMessage.EXTRA_METADATA] =
            "eyJ2ZXJzaW9uX2lkIjoxLCJ0aW1lIjoxNTk2MTUyMDk1ODk1LCJwdXNoX2lkIjoiNDkxNDA4ZmEtMjEzNC00NWE2LTk2MTgtNTY3ZGQ2ZTMwYzg3IiwiY2FtcGFpZ25zIjp7ImNhdGVnb3JpZXMiOltdfX0="
        map["google.sent_time"] = "$CURRENT_TIME_MILLIS"
        map["google.ttl"] = "2419200"
        map[PushMessage.EXTRA_ALERT] = "This is FCM Message"
        map[PushMessage.EXTRA_ACTIONS] = "{\n" +
            "    \"^d\": \"https://www.washingtonpost.com/nation/2020/07/30/coronavirus-covid-live-updates-us/\"\n" +
            "  }"
        sendPush(this, Provider.FCM, map)
    }

    /**
     * to test revere push with UAirshipNotificationProvider class.
     * Expectation is that message should be displayed in the notification.
     */
    private fun createReverePushWithAirshipFcm(context: Context) {
        val map = mutableMapOf<String, String>()
        map["google.delivered_priority"] = "normal"
        map[PushMessage.EXTRA_METADATA] =
            "eyJ2ZXJzaW9uX2lkIjoxLCJ0aW1lIjoxNTk2MTUyMDk1ODk1LCJwdXNoX2lkIjoiNDkxNDA4ZmEtMjEzNC00NWE2LTk2MTgtNTY3ZGQ2ZTMwYzg3IiwiY2FtcGFpZ25zIjp7ImNhdGVnb3JpZXMiOltdfX0="
        map["google.sent_time"] = "$CURRENT_TIME_MILLIS"
        map["google.ttl"] = "2419200"
        map[PushMessage.EXTRA_TITLE] = "Wash Post's"
        map[PushMessage.EXTRA_ALERT] = "The future of autonomous delivery may be unfolding in an unlikely place: Suburban Houston"
        map[PushMessage.EXTRA_SUMMARY] =
            "For months now, Nuro’s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy."
        map["custom"] =
            """
            {
            "text": "For months now, Nuro\u2019s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy.",
            "pushID": "5f4595f2331818300000000a",
            "datetime": "$CURRENT_TIME_IN_WP_FORMAT",
            "contentURL": "https://www.washingtonpost.com/technology/2019/11/07/future-autonomous-delivery-may-be-unfolding-an-unlikely-place-suburban-houston/",
            "imageURL": "$TEST_IMAGE_URL",
            "targetTopic": "breaking-news",
            "analyticsTopic": "classic:android:breaking-news",
            "title": "Wash Post's alert",
            "interactionType": "BREAKING_NEWS"
            }
            """.trimIndent()
        sendPush(this, Provider.FCM, map)
    }

    /**
     * to test revere segmented push with UAirshipNotificationProvider class.
     * Expectation is that message should be displayed in the notification.
     */
    private fun createRevereSegmentedPushWithAirshipFcm(context: Context) {
        val map = mutableMapOf<String, String>()
        map["google.delivered_priority"] = "normal"
        map[PushMessage.EXTRA_METADATA] =
            "eyJ2ZXJzaW9uX2lkIjoxLCJ0aW1lIjoxNTk2MTUyMDk1ODk1LCJwdXNoX2lkIjoiNDkxNDA4ZmEtMjEzNC00NWE2LTk2MTgtNTY3ZGQ2ZTMwYzg3IiwiY2FtcGFpZ25zIjp7ImNhdGVnb3JpZXMiOltdfX0="
        map["google.sent_time"] = "$CURRENT_TIME_MILLIS"
        map["google.ttl"] = "2419200"
        map[PushMessage.EXTRA_TITLE] = "Wash Post's"
        map[PushMessage.EXTRA_ALERT] = "The future of autonomous delivery may be unfolding in an unlikely place: Suburban Houston"
        map[PushMessage.EXTRA_SUMMARY] =
            "For months now, Nuro’s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy."
        map["custom"] =
            """
            {
            "text": "For months now, Nuro\u2019s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy.",
            "pushID": "5f4595f2331818300000000a",
            "datetime": "$CURRENT_TIME_IN_WP_FORMAT",
            "contentURL": "https://www.washingtonpost.com/technology/2019/11/07/future-autonomous-delivery-may-be-unfolding-an-unlikely-place-suburban-houston/",
            "imageURL": "$TEST_IMAGE_URL",
            "targetTopic": "breaking-news",
            "analyticsTopic": "classic:android:breaking-news",
            "title": "Wash Post's alert",
            "interactionType": "SEGMENTED",
            "segments": [{"name":"A Name","imageURL":"https://dohdeick6sqa6.cloudfront.net/screenshots/2018-test/general/head-to-head/house@lg.png"},{"name":"B Name","imageURL":"https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png"},{"name":"C Name","imageURL":"https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png"}]
            }
            """.trimIndent()
        sendPush(this, Provider.FCM, map)
    }

    /**
     * to test airship's direct ADM push with UAirshipNotificationProvider class.
     * Expectation is that provider class should not process this non-airship/non-revere message
     */
    private fun createPushWithAirshipAdm(context: Context) {
        val map = mutableMapOf<String, String>()
        // map["adm_message_md5"] = "464r4GZE/f057yfwa87DhQ=="
        // map["com.urbanairship.push.PUSH_ID"] = "69c6a700-d337-11ea-bacf-0242805e64d2"
        // map["com.urbanairship.push.CANONICAL_PUSH_ID"] = "207128e0-fd95-4731-9453-cb26e7bacc29"
        // map["com.urbanairship.push.APID"] = "49712fb7-0a06-4c17-a4ad-8d0aec7f9f50"
        map[PushMessage.EXTRA_METADATA] =
            "eyJ2ZXJzaW9uX2lkIjoxLCJ0aW1lIjoxNTk2MjA0NTUzOTUxLCJwdXNoX2lkIjoiMjA3MTI4ZTAtZmQ5NS00NzMxLTk0NTMtY2IyNmU3YmFjYzI5IiwiY2FtcGFpZ25zIjp7ImNhdGVnb3JpZXMiOltdfX0="
        map[PushMessage.EXTRA_STYLE] =
            "{\"type\":\"big_picture\",\"big_picture\":\"https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/3UUG4JSEFMI6VGOHDX6UEQNC7Y.jpg\"}"
        map[PushMessage.EXTRA_ALERT] = "Test Message"
        map[PushMessage.EXTRA_ACTIONS] =
            "{\"^u\":\"https://www.washingtonpost.com/graphics/2020/health/coronavirus-frequently-asked-questions/\"}"
        sendPush(this, Provider.ADM, map)
    }

    /**
     * to test revere ADM push with UAirshipNotificationProvider class.
     * Expectation is that message should be displayed in the notification.
     */
    private fun createReverePushWithAirshipAdm(context: Context) {
        val map = mutableMapOf<String, String>()
        map[PushMessage.EXTRA_METADATA] =
            "eyJ2ZXJzaW9uX2lkIjoxLCJ0aW1lIjoxNTk2MjA0NTUzOTUxLCJwdXNoX2lkIjoiMjA3MTI4ZTAtZmQ5NS00NzMxLTk0NTMtY2IyNmU3YmFjYzI5IiwiY2FtcGFpZ25zIjp7ImNhdGVnb3JpZXMiOltdfX0="
        map[PushMessage.EXTRA_ALERT] = "The future of autonomous delivery may be unfolding in an unlikely place: Suburban Houston"
        map["summary"] =
            "For months now, Nuro’s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy."
        map["custom"] =
            """
            {
            "text": "For months now, Nuro\u2019s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy.",
            "pushID": "5f4595f2331818300000000a",
            "datetime": "$CURRENT_TIME_IN_WP_FORMAT",
            "contentURL": "$TEST_URL",
            "imageURL": "$TEST_IMAGE_URL",
            "targetTopic": "breaking-news",
            "analyticsTopic": "classic:android:breaking-news",
            "title": "",
            "interactionType": "BREAKING_NEWS"
            }
            """.trimIndent()
        sendPush(this, Provider.ADM, map)
    }

    /**
     * to test airship's direct ADM segmented push with UAirshipNotificationProvider class.
     * Expectation is that provider class should not process this non-airship/non-revere message
     */
    private fun createRevereSegmentedPushWithAirshipAdm(context: Context) {
        val map = mutableMapOf<String, String>()
        map[PushMessage.EXTRA_METADATA] =
            "eyJ2ZXJzaW9uX2lkIjoxLCJ0aW1lIjoxNTk2MjA0NTUzOTUxLCJwdXNoX2lkIjoiMjA3MTI4ZTAtZmQ5NS00NzMxLTk0NTMtY2IyNmU3YmFjYzI5IiwiY2FtcGFpZ25zIjp7ImNhdGVnb3JpZXMiOltdfX0="
        map[PushMessage.EXTRA_ALERT] = "The future of autonomous delivery may be unfolding in an unlikely place: Suburban Houston"
        map["summary"] =
            "For months now, Nuro’s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy."
        map["custom"] =
            """
            {
            "text": "For months now, Nuro\u2019s robotically piloted vehicles have been quietly delivering groceries around Houston, laying the groundwork for a new era of autonomy.",
            "pushID": "5f4595f2331818300000000a",
            "datetime": "$CURRENT_TIME_IN_WP_FORMAT",
            "contentURL": "$TEST_DEV_URL",
            "imageURL": "$TEST_IMAGE_URL",
            "targetTopic": "breaking-news",
            "analyticsTopic": "classic:android:breaking-news",
            "title": "",
            "interactionType": "SEGMENTED",
            "segments": [{"name": "A Name","imageURL": "https://dohdeick6sqa6.cloudfront.net/screenshots/2018-test/general/head-to-head/house@lg.png"},{"name": "B Name","imageURL": "https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png"},{"name": "C Name","imageURL": "https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png"}]
            }
            """.trimIndent()
        sendPush(this, Provider.ADM, map)
    }

    private fun sendPush(
        context: Context,
        provider: Provider,
        map: MutableMap<String, String>,
    ) {
        when (provider) {
            Provider.FCM -> {
                // PushProviderBridge.processPush(FcmPushProvider::class.java, PushMessage(map)).executeSync(context)
            }
            Provider.ADM -> {
                // PushProviderBridge.processPush(AdmPushProvider::class.java, PushMessage(map)).executeSync(context)
            }
        }
    }
}
