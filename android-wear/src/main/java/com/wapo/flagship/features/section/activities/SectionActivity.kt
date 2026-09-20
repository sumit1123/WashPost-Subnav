/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */
package com.wapo.flagship.features.section.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.wapo.android.commons.util.LogUtil
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.WearFlagshipApplication
import com.wapo.flagship.common.InfoActivity
import com.wapo.flagship.features.section.adapters.HeadlineFragmentStateAdapter
import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.features.section.viewmodels.SectionViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.wapo.flagship.features.section.models.PageType
import com.wapo.flagship.models.UserEvent
import com.wapo.flagship.utils.WearUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivitySectionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.min

@AndroidEntryPoint
class SectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySectionBinding

    private lateinit var headlineFragmentStateAdapter: HeadlineFragmentStateAdapter

    private val sectionViewModel: SectionViewModel by viewModels()
    private val articlesPagerCollaborationViewModel
            : ArticlesPagerCollaborationViewModel by viewModels()

    private var loadTopStoriesJob: Job? = null
    private var toggleDotVisibilityJob: Job? = null
    private var userEventJob: Job? = null

    // views
    private lateinit var viewPager: ViewPager2
    private var isListeningToArticle = false

    private val notificationListReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            LogUtil.d(TAG, "Broadcast received")
            val notificationList =
                intent.getParcelableArrayListExtra<ArticleMeta>(WearFlagshipApplication.NOTIFICATION_LIST)
            if (notificationList.isNullOrEmpty()) {
                loadViewPager(
                    listOf(ArticleMeta("No alerts", "", ""))
                )
            } else {
                loadViewPager(notificationList)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init views
        viewPager = binding.viewPager

        // retrieve section name and load appropriate data
        if (intent.hasExtra(SECTION)) {
            intent.getStringExtra(SECTION)?.let {
                when (WearAppContext.config().availableSections[it]?.pageType) {
                    PageType.FUSION -> {
                        sectionViewModel.getFusionSection(it.toLowerCase())
                        LogUtil.d(TAG, "Fusion section")
                    }
                    PageType.PAGE_BUILDER -> {
                        sectionViewModel.getPageBuilderSection(it.toLowerCase())
                        LogUtil.d(TAG, "Page builder section")
                    }
                    null -> LogUtil.d(TAG, "Unknown section")
                }
            }
        } else {
            LogUtil.d(TAG, "This activity requires SECTION intent extra")
            finish()
        }

    }

    override fun onStart() {
        super.onStart()

        subscribeToStateUpdate()
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            notificationListReceiver, IntentFilter(WearFlagshipApplication.NOTIFICATION_INTENT)
        )
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationListReceiver)
    }

    override fun onStop() {
        super.onStop()

        unsubscribeToStateUpdate()
    }

    private fun subscribeToStateUpdate() {
        val viewPager = binding.viewPager
        loadTopStoriesJob = lifecycleScope.launch {
            launch {
                sectionViewModel.sectionState
                    .map { it.isLoading }
                    .collect { isLoading ->
                        if (isLoading) {
                            LogUtil.d(TAG, "Loading section...")
                        } else {
                            LogUtil.d(TAG, "Section loaded")
                        }
                    }
            }
            launch {
                sectionViewModel.sectionState
                    .map { it.articleMetaList }
                    .collect {
                        LogUtil.d(TAG, "Article meta list size: ${it.size}")
                        if (it.isNotEmpty()) {
                            loadViewPager(it)
                            LogUtil.d(TAG, it.toString())
                            WearAppContext.topHeadline = it.first().headline
                            WearAppContext.topHeadlineUrl = it.first().contentUrl
                        }
                    }
            }
        }

        userEventJob = lifecycleScope.launch {
            articlesPagerCollaborationViewModel.userEvent.collect { event ->
                when (event) {
                    is UserEvent.ListenToArticle -> {
                        if (WearUtils.speakerIsSupported(this@SectionActivity)) {
                        } else {
                            InfoActivity.show(
                                this@SectionActivity,
                                getString(R.string.prompt_to_connect_to_headset)
                            )
                        }
                    }
                    is UserEvent.SaveForLater -> {}
                    is UserEvent.Next -> {
                        viewPager.setCurrentItem(
                            viewPager.currentItem + 1,
                            true
                        )
                    }
                }
            }
        }

        toggleDotVisibilityJob = lifecycleScope.launch {
            articlesPagerCollaborationViewModel.dotVisibility.collect {
                binding.intoTabLayout.visibility =
                    if (it) View.VISIBLE
                    else View.GONE
            }
        }
    }

    private fun unsubscribeToStateUpdate() {
        loadTopStoriesJob?.cancel()
        toggleDotVisibilityJob?.cancel()
        userEventJob?.cancel()
    }

    private fun loadViewPager(articleMetaList: List<ArticleMeta>) {
        headlineFragmentStateAdapter = HeadlineFragmentStateAdapter(
            this,
            articleMetaList.take(min(articleMetaList.size, ARTICLE_LIMIT))
        )
        viewPager.adapter = headlineFragmentStateAdapter
        TabLayoutMediator(binding.intoTabLayout, viewPager) { _, _ -> }
            .attach()

        if (intent.hasExtra(ARTICLE_URL)) {
            val index = articleMetaList.indexOfFirst {
                intent.getStringExtra(ARTICLE_URL) == it.contentUrl
            }
            if (index != -1) {
                viewPager.setCurrentItem(index, true)
            }
        }
    }

    companion object {
        private const val TAG = "SectionActivity"
        private val ARTICLE_LIMIT = WearAppContext.config().articleLimit
        const val SECTION = "com.wapo.flagship.features.section.activities.extra.SECTION"
        const val ARTICLE_URL = "com.wapo.flagship.features.section.activities.extra.ARTICLE_URL"
    }

}