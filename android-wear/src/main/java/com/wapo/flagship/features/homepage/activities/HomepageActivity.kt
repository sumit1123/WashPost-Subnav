/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.wapo.android.push.PushService
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.features.section.activities.SectionActivity
import com.wapo.flagship.features.homepage.adapters.CustomScrollingLayoutCallback
import com.wapo.flagship.features.homepage.adapters.HomepageItemAdapter
import com.wapo.flagship.features.homepage.models.MenuItem
import com.wapo.flagship.utils.WearUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityHomepageBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomepageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding

    private lateinit var homepageItemAdapter: HomepageItemAdapter

    // views
    private lateinit var rvHomepageItems: WearableRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initHomepageItems()

        // enable Airship registration
        if (!WearAppContext.isConnectedToPhone) {
            PushService.getInstance().pushManager.enableRegistration()
        }
    }

    private fun initHomepageItems() {
        rvHomepageItems = binding.rvHomepageItems

        homepageItemAdapter = HomepageItemAdapter { menuItem ->
            val intent = Intent(this, SectionActivity::class.java).apply {
                putExtra(SectionActivity.SECTION, menuItem.sectionName)
            }
            startActivity(intent)
        }
        rvHomepageItems.apply {
            adapter = homepageItemAdapter
            if (WearUtils.isScreenRound(context)) {
                isEdgeItemsCenteringEnabled = true
                layoutManager = WearableLinearLayoutManager(
                    this@HomepageActivity,
                    CustomScrollingLayoutCallback()
                )
            } else {
                isEdgeItemsCenteringEnabled = false
                layoutManager = WearableLinearLayoutManager(
                    this@HomepageActivity
                )
            }
        }
        homepageItemAdapter.submitList(initialHomepageItems)
    }

    companion object {
        private val initialHomepageItems = listOf(
            MenuItem(sectionName = "Top Stories", iconResId = R.drawable.ic_top_stories)
        )
    }

}