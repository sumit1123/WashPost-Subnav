package com.wapo.flagship

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.content.notifications.NotificationData
import com.wapo.flagship.data.FileMeta
import com.washingtonpost.android.R
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DebugReceiver : BroadcastReceiver() {
    companion object {
        private var alertsIndex = 1
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val TAG = "[d][receiver]"
        Logger.d(TAG, "onReceive")

        when (intent.action) {
            "dump" -> {
                val path = intent.getStringExtra("path") ?: Environment.getExternalStorageDirectory().path ?: return
                android.os.Debug.dumpHprofData(File(path, "dump.hprof").path)
            }

            "replace.section" -> {
                val cm = FlagshipApplication.getInstance().cacheManager
                val page = intent.getStringExtra("page") ?: return
                val pages = cm.getFileMetas("${FileMeta.UrlColumn} like '%/$page'", null, null)
                if (pages.size == 1) {
                    val fm = pages[0]
                    IOUtils.copy(
                        context.resources.openRawResource(R.raw.layout),
                        FileOutputStream(fm.path),
                    )
                    cm.dropFileMetaSoftTtl(fm.url)
                }
            }

            "drop_articles" -> {
                Thread {
                    FlagshipApplication.getInstance().articleDatabase.articlesDao().cleanUp(
                        Long.MAX_VALUE,
                    )
                }.start()
            }
        }
    }

    private fun createList(): List<NotificationData> = (1..3).map { createNotification("http://storyUrl-$it.com") }

    private fun createNotification(url: String): NotificationData =
        NotificationData("alert ${alertsIndex++}").apply {
            feed = "rainbowdatanet-a.wpdigital.net/flagship_test/ios_phone/thumb_300x300/top-stories-layout.json"
            storyUrl = url
            type = ""
            notifId = "${Random().nextInt(100) + 100}"
            timestamp = (System.currentTimeMillis() / 1000).toString()
        }
}
