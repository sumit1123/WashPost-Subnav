/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.activities

import android.widget.TextView
import android.os.Bundle
import android.content.Intent
import com.google.firebase.analytics.FirebaseAnalytics
import android.graphics.Typeface
import android.view.WindowManager
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import com.wapo.android.commons.util.LogUtil
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.activity.ConfirmationActivity
import androidx.wear.widget.drawer.WearableActionDrawerView
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.WearFlagshipApplication
import com.wapo.flagship.data.MobileMessenger
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.utils.SanitizedHtmlTextFormatter
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityArticleBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 *  Unused in new wear app. Reserve just in case.
 *
 */
@AndroidEntryPoint
class WearArticleActivity : FragmentActivity(),
    MenuItem.OnMenuItemClickListener {

    private lateinit var binding: ActivityArticleBinding

    private val articles2ViewModel: Articles2ViewModel by viewModels()
    private var loadArticleJob: Job? = null

    // views
    private lateinit var articleHeadline: TextView
    private lateinit var articleTimestamp: TextView
    private lateinit var articleByline: TextView
    private lateinit var articleImage: ImageView
    private lateinit var articleContent: TextView
    private lateinit var bottomActionDrawerView: WearableActionDrawerView

    private var MESSAGE_REQUEST: String? = null
    private var handler: Handler? = null
    private var autoScrollRunnable: Runnable? = null
    private var tapped = false
    private var articleMeta: ArticleMeta? = null

    // Month in text, day, hour:minutes AM/PM and EDT
    private val format = SimpleDateFormat("MMMM dd 'at' h:mm a z", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        articleMeta = intent.getParcelableExtra(REQUEST_CONTENT)
        LogUtil.d("MYTAG", articleMeta.toString())

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "article")
        bundle.putString(FirebaseAnalytics.Param.CONTENT, articleMeta?.contentUrl)
        FirebaseAnalytics.getInstance(WearFlagshipApplication.getInstance())
            .logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)

        // init views
        articleHeadline = binding.articleHeadline
        articleHeadline.typeface = Typeface.createFromAsset(this.assets, "Postoni-Bold.otf")
        articleByline = binding.articleByline
        articleTimestamp = binding.timestamp
        articleContent = binding.articleContent
        articleImage = binding.articleImage

        // Bottom Action Drawer
        bottomActionDrawerView = binding.bottomActionDrawerView
        menuInflater.inflate(R.menu.action_drawer_menu, bottomActionDrawerView.menu)
        bottomActionDrawerView.setOnMenuItemClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        articleMeta?.let { articles2ViewModel.getArticle2(it) }
        loadArticleJob = lifecycleScope.launch {
            launch {
                articles2ViewModel.article2State
                    .map { it.articleContentState }
                    .collect { articleContentState ->
                        when (articleContentState) {
                            is ArticleContentState.Loading -> {
                                LogUtil.d(TAG, "Loading article...")
                            }
                            is ArticleContentState.Success -> {
                                LogUtil.d(TAG, "Article loaded!")
                                showArticle(articleContentState.article)
                            }
                            is ArticleContentState.Failure -> {
                                LogUtil.d(TAG, "Error loading article!")
                            }
                            is ArticleContentState.Unsupported -> {
                                LogUtil.d(TAG, "Article unsupported!")
                            }
                            is ArticleContentState.UiTimedOut -> {
                                LogUtil.d(TAG, "Article timed out!")
                            }
                        }
                    }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // stop auto scroll runnable if application is paused
        handler?.removeCallbacks(autoScrollRunnable!!)
    }

    override fun onStop() {
        super.onStop()
        loadArticleJob?.cancel()
    }

    private fun showArticle(article2: Article2?) {
//        if (result == ArticlesAsyncTask.noNetworkKey) {
//            byline.setText(R.string.connect_to_network)
//            mWearableActionDrawer.visibility = View.GONE
//            return
//        }
        if (article2 == null) {
            articleByline.setText(R.string.content_error)
            bottomActionDrawerView.visibility = View.GONE
            return
        }

        bottomActionDrawerView.visibility = View.VISIBLE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val calendar = Calendar.getInstance()
        val items = article2.items!!
        val articleParagraphs = StringBuilder()
        var imageFound = false
        var paragraphCounter = 0
        var i = 0

        //get article paragraphs and image from the article content. As soon as 6 paragraphs are reached, we exit the loop.
        while (i < items.size && paragraphCounter < paragraphLimit) {
            if (items[i] is Title) {
                articleHeadline.text = (items[i] as Title).content
            } else if (items[i] is ByLine) {
                articleByline.text = (items[i] as ByLine).content
            } else if (items[i] is Date) {
                calendar.timeInMillis = (items[i] as Date).content!!
                articleTimestamp.text = format.format(calendar.time)
            } else if (items[i] is SanitizedHtml) {
                articleParagraphs.append("""
                    ${SanitizedHtmlTextFormatter.format(items[i] as SanitizedHtml)}
                    
                    
                    """.trimIndent())
                paragraphCounter++
            } else if (items[i] is Image && !imageFound) {
                //PicassoImageLoader.loadImage((items[i] as Image).imageURL, articleImage)
                imageFound = true
            }
            i++
        }
        articleContent.text = articleParagraphs

        handler = Handler(Looper.getMainLooper())
        // runnable for auto scrolling.
        // The auto scroll will be started after 3 seconds and will cancel on touch.
        autoScrollRunnable = Runnable {
            object : CountDownTimer(
                (binding.articleContainer.height * 30).toLong(),
                30
            ) {
                override fun onTick(millisUntilFinished: Long) {
                    val height = binding.articleContainer.height
                    val currentHeight = height - millisUntilFinished.toInt() / 30
                    binding.scrollView.scrollTo(0, currentHeight)
                    if (tapped) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        cancel()
                    }
                }

                override fun onFinish() {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    tapped = false
                }
            }.start()
        }
        binding.scrollView.setOnTouchListener { v, _ ->
            v.performClick()
            tapped = true
            false
        }
        handler!!.postDelayed(autoScrollRunnable!!, 3000)
    }

    // when the action drawer is clicked
    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        val itemId = menuItem.itemId
        //wearableActionDrawerView.closeDrawer()
        if (!WearAppContext.isConnectedToPhone) {
            WearFlagshipApplication.getAppStoreDialog(
                this,
                resources.getString(R.string.download_phone_app_message)
            ).show()
            return true
        }
        var intentMessage = ""

        //intent for a confirmation screen
        val intent = Intent(this, ConfirmationActivity::class.java)

        //if the user clicks save to phone/open article button in the action drawer
        when (itemId) {
            R.id.save_to_phone -> {
                intent.putExtra(
                    ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION
                )
                MESSAGE_REQUEST = SAVE_ARTICLE_MOBILE
                intentMessage = getString(R.string.saved_article_confirmation)
                intent.putExtra(
                    ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION
                )
            }
            R.id.read_more -> {
                intent.putExtra(
                    ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION
                )
                MESSAGE_REQUEST = OPEN_ARTICLE_MOBILE
                intentMessage = getString(R.string.open_mobile_app_confirmation)
            }
            else -> MESSAGE_REQUEST = ""
        }
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, intentMessage)
        startActivity(intent)

        val sender = MobileMessenger(this)
        sender.sendMessage(
            "DataLayerNotification",
            "$MESSAGE_REQUEST ARTICLE_TAG:$articleMeta HEADLINE:$articleHeadline"
        )

        return true
    }

    companion object {
        private const val TAG = "WearArticleActivity"
        const val REQUEST_CONTENT = "request_content"
        const val OPEN_ARTICLE_MOBILE = "open_article_mobile"
        const val SAVE_ARTICLE_MOBILE = "save_article_mobile"
        private val paragraphLimit = WearAppContext.config().paragraphLimit
    }

}