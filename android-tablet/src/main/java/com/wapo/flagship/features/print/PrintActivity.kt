package com.wapo.flagship.features.print

import android.os.Bundle
import androidx.activity.viewModels
import com.wapo.android.commons.util.getCanonicalUrl
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.di.app.modules.features.readinghistory.ReadingHistoryViewModel
import com.wapo.flagship.features.sections.ConnectivityActivity
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityPrintBinding
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import com.washingtonpost.android.save.views.ArticleListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class PrintActivity :
    BaseActivity(),
    ConnectivityActivity {
    private lateinit var binding: ActivityPrintBinding

    private lateinit var articleListViewModel: ArticleListViewModel
    private val readingHistoryViewModel: ReadingHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrintBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.apply {
            setSupportActionBar(this)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
        }
        articleListViewModel =
            FlagshipApplication.getInstance().savedArticleManager.getViewModel(this)
        addPrintToReadingHistory(getString(R.string.print_edition_path))
        Measurement.trackBottomTabNavigation("print")

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, ArchivesFragment())
                .commitNow()
        }
    }

    override fun checkConnectivity() {
        if (!ReachabilityUtil.isConnected(this)) {
            notifyNetworkProblem(binding.root, true)
        }
    }

    override fun onSupportNavigateUp(): Boolean = super.onSupportNavigateUp()

    private fun addPrintToReadingHistory(url: String) {
        val readingHistoryModel =
            ReadingHistoryModel(url, getCanonicalUrl(url), System.currentTimeMillis(), null, false)
        val meta =
            MetadataModel(
                readingHistoryModel.contentUrl,
                readingHistoryModel.lmt
            )
        readingHistoryViewModel.saveArticle(readingHistoryModel, meta)
    }
}
