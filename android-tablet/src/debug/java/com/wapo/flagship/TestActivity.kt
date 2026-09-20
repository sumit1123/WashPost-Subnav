package com.wapo.flagship

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.wapo.flagship.content.ContentManager
import com.washingtonpost.android.R
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

private const val PageName = "entertainment"

class TestActivity : FragmentActivity() {
    private val bindButton by lazy { findViewById<Button>(R.id.bind_button) as Button }
    private val unbindButton by lazy { findViewById<Button>(R.id.unbind_button) as Button }
    private val syncButton by lazy { findViewById<Button>(R.id.start_sync_button) as Button }
    private val statusMsg by lazy { findViewById<TextView>(R.id.status_message) as TextView }

    private val updatePageBtn by lazy { findViewById<Button>(R.id.update_page) }
    private val frcUpdatePageBtn by lazy { findViewById<Button>(R.id.force_update_page) }
    private val clearPageBtn by lazy { findViewById<Button>(R.id.delete_page) }

    private var subscription: Subscription? = null

    private var contentManager: ContentManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        unbindButton.isEnabled = false
        syncButton.isEnabled = false

        updatePageBtn.setOnClickListener {
            statusMsg.text = ""
            contentManager
                ?.updatePage(PageName, false)
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(
                    { p -> statusMsg.text = statusMsg.text.toString() + "\nSection loaded" },
                    { e -> statusMsg.text = "Error: ${e.message}" },
                    { statusMsg.text = "${statusMsg.text}\nComplete" },
                )
        }

        frcUpdatePageBtn.setOnClickListener {
            statusMsg.text = ""
            contentManager
                ?.updatePage(PageName, true)
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(
                    { p -> statusMsg.text = statusMsg.text.toString() + "\nSection loaded" },
                    { e -> statusMsg.text = "Error: ${e.message}" },
                    { statusMsg.text = "${statusMsg.text}\nComplete" },
                )
        }

        clearPageBtn.setOnClickListener {
            val cm = FlagshipApplication.getInstance().cacheManager
            val fm =
                cm.getFileMetaByUrl(
                    "https://jsonapp.washingtonpost.com/api/prod/v1/tablet/" + PageName,
                ) ?: return@setOnClickListener
            cm.deleteFileMeta(fm.id)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (subscription != null && subscription?.isUnsubscribed?.not() ?: false) {
            subscription?.unsubscribe()
        }

        subscription = null
    }
}
