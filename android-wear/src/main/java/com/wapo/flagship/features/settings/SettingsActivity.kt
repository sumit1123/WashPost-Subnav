/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.settings


import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.wear.activity.ConfirmationActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.WearFlagshipApplication
import com.washingtonpost.android.databinding.ActivitySettingsBinding

/**
 *  Unused in new wear app. Reserve just in case.
 *
 */
class SettingsActivity : ComponentActivity() {
    private lateinit var binding: ActivitySettingsBinding

    // views
    private lateinit var settingsRecyclerView: WearableRecyclerView

    private var sectionList: List<String> = emptyList()
    private var settingsAdapter: SettingsAdapter? = null

    private var sectionChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sectionList = WearAppContext.config().availableSections.keys.toList()
        initRecyclerView()
    }

    override fun onResume() {
        super.onResume()

        if (sectionChanged) {
            finish()
        }
    }

    private fun initRecyclerView() {
        settingsAdapter = SettingsAdapter(sectionList) { section ->
            //WearAppContext.extraSection = section

            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, "extra section selected")
                putString(FirebaseAnalytics.Param.CONTENT, section)
            }
            FirebaseAnalytics.getInstance(WearFlagshipApplication.getInstance())
                .logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)

            sectionChanged = true

            val intent = Intent(this, ConfirmationActivity::class.java)
            intent.putExtra(
                ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION
            ).apply {
                putExtra(ConfirmationActivity.EXTRA_MESSAGE, "$section selected")
            }
            startActivity(intent)
        }
        settingsRecyclerView = binding.settings.apply {
            adapter = settingsAdapter
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(this@SettingsActivity)
        }
    }
}