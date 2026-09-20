package com.wapo.flagship.debug

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.wapo.android.commons.util.Logger
import android.widget.Toast
import androidx.core.content.FileProvider
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.navigation.ui.BottomTabFragment
import com.washingtonpost.android.BuildConfig
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileReader

/**
 * Get the current section json response from cache and send it via email
 */
class SectionSnapshot(
    private val contentManager: ContentManager,
    private val cacheManager: CacheManager,
    private val mainActivity: MainActivity,
) {
    fun run() {
        val sectionId = getCurrentSectionId(mainActivity) ?: return

        contentManager
            .getPageUrlObs(sectionId)
            .map { pageUrl -> readFromCache(pageUrl) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { text -> sendEmail(text, sectionId) },
                {
                    Toast
                        .makeText(
                            mainActivity,
                            "Could not create dump for $sectionId",
                            Toast.LENGTH_LONG,
                        ).show()
                    Logger.e("SectionSnapshot", "Could not create dump for $sectionId", it)
                },
            )
    }

    private fun readFromCache(pageUrl: String?): String? {
        pageUrl ?: return null
        val fileMeta = cacheManager.getFileMetaByUrl(pageUrl)
        return if (fileMeta != null) {
            val path = fileMeta.path
            val text = FileReader(path).readText()
            text
        } else {
            null
        }
    }

    private fun getCurrentSectionId(mainActivity: MainActivity): String? {
        mainActivity.supportFragmentManager.fragments.forEach { fragment ->
            if (fragment != null && fragment.isVisible) {
                if (fragment is BottomTabFragment) {
                    return fragment.getSectionBundleId()
                }
            }
        }

        return null
    }

    private fun sendEmail(
        text: String?,
        sectionId: String,
    ) {
        text ?: return

        val dir = mainActivity.filesDir
        val debugDir =
            File(
                dir,
                if (BuildConfig.DEBUG) {
                    "debug/"
                } else if (AppContextUtils.isBetaBuild()) {
                    "beta/"
                } else {
                    ""
                },
            )
        debugDir.mkdirs()
        val file = File(debugDir, "section-dump.json")
        file.delete()
        file.writeText(text)

        val i = Intent(Intent.ACTION_SEND_MULTIPLE)
        i.type = "message/rfc822"
        i.putExtra(Intent.EXTRA_EMAIL, arrayOf("recipient@example.com"))
        i.putExtra(Intent.EXTRA_SUBJECT, "Dump for $sectionId")
        i.putExtra(Intent.EXTRA_TEXT, "Section dump is attached")

        val uris: ArrayList<Uri> = ArrayList()
        val uri =
            FileProvider.getUriForFile(
                mainActivity,
                "com.washingtonpost.android.cacheprovider",
                file,
            )
        uris.add(uri)
        i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        try {
            mainActivity.startActivity(Intent.createChooser(i, "Send mail..."))
        } catch (ex: ActivityNotFoundException) {
            Toast
                .makeText(
                    mainActivity,
                    "There are no email clients installed.",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }
}
