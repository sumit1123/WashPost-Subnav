package com.wapo.flagship

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.wapo.android.push.PushNotification
import com.wapo.android.push.PushService
import com.wapo.flagship.push.PushListener
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

@AndroidEntryPoint
class PushNotificationDebugActivity :
    BaseDebugActivity(),
    View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var wrapContentLayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        var layout = LinearLayout(this)
        layout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
        layout.orientation = LinearLayout.VERTICAL

        var launchDoubleTabLiveImageAlert = Button(this)
        launchDoubleTabLiveImageAlert.layoutParams = wrapContentLayoutParams
        launchDoubleTabLiveImageAlert.text = "Notify Double Tab Live Image Alert"
        launchDoubleTabLiveImageAlert.tag = "launchDoubleTabLiveImageAlert"
        launchDoubleTabLiveImageAlert.setOnClickListener(this)
        layout.addView(launchDoubleTabLiveImageAlert)

        var launchSingleTabLiveImageAlert = Button(this)
        launchSingleTabLiveImageAlert.layoutParams = wrapContentLayoutParams
        launchSingleTabLiveImageAlert.text = "Notify Single Tab Live Image Alert"
        launchSingleTabLiveImageAlert.tag = "launchSingleTabLiveImageAlert"
        launchSingleTabLiveImageAlert.setOnClickListener(this)
        layout.addView(launchSingleTabLiveImageAlert)

        var launchPush = Button(this)
        launchPush.layoutParams = wrapContentLayoutParams
        launchPush.text = "Notify Alert"
        launchPush.tag = "launchPush"
        launchPush.setOnClickListener(this)
        layout.addView(launchPush)

        setContentView(layout)
    }

    override fun onClick(v: View?) {
        var pushMessage = ""
        when (v?.tag) {
            "launchPush" -> {
                pushMessage = "{\n" +
                    "    \"message\": \"Its usually difficult for people to agree on a crowds size. Heres why.\",\n" +
                    "    \"headline\": \"Its usually difficult for people to agree on a crowd’s size. Here’s why.\",\n" +
                    "    \"title\": \"Entertainment Alert\",\n" +
                    "    \"pushID\": \"entertainment_1488226374719\",\n" +
                    "    \"sequence\": 1,\n" +
                    "    \"action\": \"ADD\",\n" +
                    "    \"type\": \"news-alert\",\n" +
                    "    \"datetime\": ${System.currentTimeMillis()},\n" +
                    "    \"url\": \"https://www.washingtonpost.com/news/wonk/wp/2017/01/22/its-usually-difficult-for-people-to-agree-on-a-crowds-size-heres-why/?utm_term=.11d727e79d1d\",\n" +
                    "    \"analyticsTopic\": \"Entertainment:GCM\",\n" +
                        "    \"testGroups\": {\n" +
                        "        \"dr_2503\": \"qa_test\"\n" +
                        "    },\n" +
                    "    \"targetTopic\": \"breaking-news\"\n" +
                    "}"
            }

            "launchSingleTabLiveImageAlert" -> {
                pushMessage = "{\n" +
                    "    \"message\": \"Message: Single tab live image push\",\n" +
                    "    \"headline\": \"Headline: Single tab live image push\",\n" +
                    "    \"title\": \"Title: Single tab live image push\",\n" +
                    "    \"pushID\": \"entertainment_1488226374719\",\n" +
                    "    \"sequence\": 1,\n" +
                    "    \"action\": \"ADD\",\n" +
                    "    \"type\": \"news-alert\",\n" +
                    "    \"datetime\": ${System.currentTimeMillis()},\n" +
                    "    \"url\": \"https://www.washingtonpost.com/\",\n" +
                    "    \"analyticsTopic\": \"Entertainment:GCM\",\n" +
                        "    \"testGroups\": {\n" +
                        "        \"dr_2503\": \"qa_test\"\n" +
                        "    },\n" +
                    "    \"targetTopic\": \"breaking-news\",\n" +
                    "    \"interactionType\": \"SEGMENTED\",\n" +
                    "    \"segments\": [\n" +
                    "        {\n" +
                    "            \"name\": \"Tab 1\",\n" +
                    "            \"imageURL\": \"https://dohdeick6sqa6.cloudfront.net/screenshots/2018-test/general/head-to-head/house@lg.png\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"
            }

            "launchDoubleTabLiveImageAlert" -> {
                pushMessage = "{\n" +
                    "    \"message\": \"Message: Single tab live image push\",\n" +
                    "    \"headline\": \"Headline: Single tab live image push\",\n" +
                    "    \"title\": \"Title: Single tab live image push\",\n" +
                    "    \"pushID\": \"entertainment_1488226374719\",\n" +
                    "    \"sequence\": 1,\n" +
                    "    \"action\": \"ADD\",\n" +
                    "    \"type\": \"news-alert\",\n" +
                    "    \"datetime\": ${System.currentTimeMillis()},\n" +
                    "    \"url\": \"https://www.washingtonpost.com/\",\n" +
                    "    \"analyticsTopic\": \"Entertainment:GCM\",\n" +
                    "    \"targetTopic\": \"breaking-news\",\n" +
                        "    \"testGroups\": {\n" +
                        "        \"dr_2503\": \"qa_test\"\n" +
                        "    },\n" +
                    "    \"interactionType\": \"SEGMENTED\",\n" +
                    "    \"segments\": [\n" +
                    "        {\n" +
                    "            \"name\": \"Tab 1\",\n" +
                    "            \"imageURL\": \"https://dohdeick6sqa6.cloudfront.net/screenshots/2018-test/general/head-to-head/house@lg.png\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"name\": \"Tab 2\",\n" +
                    "            \"imageURL\": \"https://dohdeick6sqa6.cloudfront.net/screenshots/2018/general/race-table-S5100G@sm.png\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"
            }
        }

        generatePushNotification(pushMessage)
    }

    private fun generatePushNotification(pushMessage: String) {
        val pushService = PushService.getInstance()
        val pushListenerField = pushService::class.java.getDeclaredField("listener")
        pushListenerField.isAccessible = true
        val pushListener: PushListener = pushListenerField.get(pushService) as PushListener
        val testNotificationId = 767767
        pushListener.onMessage(testNotificationId, PushNotification(JSONObject(pushMessage)))
    }
}
