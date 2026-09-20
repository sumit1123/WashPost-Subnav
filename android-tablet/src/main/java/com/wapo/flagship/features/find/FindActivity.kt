package com.wapo.flagship.features.find

import android.content.Intent
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.print.PrintActivity
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavEvent
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION_FIND
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_BUNDLE_NAME
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_OPEN_WITHOUT_STACK
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_TITLE
import com.washingtonpost.android.databinding.ActivityFindBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FindActivity : BaseActivity() {

    private val sectionNavViewModel: SectionNavViewModel by viewModels()

    private val sectionTrackingViewModel: SectionTrackingViewModel by viewModels()

    private lateinit var binding: ActivityFindBinding

    private lateinit var ribbonSections: List<String>

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        ribbonSections = intent.getStringArrayListExtra(SECTION_LIST_KEY) ?: emptyList()

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        onBackPressedDispatcher.addCallback(this) {
            onSupportNavigateUp()
        }

        observeSectionNavViewModelEvents()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    private fun observeSectionNavViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionNavViewModel.sectionNavEvent.collect { event ->
                    when(event) {
                        is SectionNavEvent.OpenSection -> {
                            event.navType?.let { nav ->
                                sectionTrackingViewModel.setNavigationBehavior(nav)
                            }

                            when (event.menuSection.type) {
                                MenuSection.WEB_TYPE -> DeepLinksProcessor.process(event.menuSection.bundleName)
                                else -> openSection(event.menuSection)
                            }
                        }
                        SectionNavEvent.OpenPrint -> {
                            openPrint()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun openSection(menuSection: MenuSection) {
        // If section can be opened in ribbon, open it there
        if (menuSection.databaseId in ribbonSections) {
            openSectionInRibbon(menuSection.databaseId)
        } else {
            // Otherwise open section in a new fragment stack
            openSectionAsFragment(menuSection)
        }
    }

    /**
     * Opens Print on the stack, not as a Tab.
     */
    private fun openPrint() {
        val intent = Intent(this, PrintActivity::class.java)
        this.startActivity(intent)
    }

    private fun openSectionInRibbon(sectionId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            setAction(ACTION_OPEN_SECTION_RIBBON)
            putExtra(SECTION_ID_KEY, sectionId)
            putExtra(EXTRAS_SECTION_OPEN_WITHOUT_STACK, true)
        }
        startActivity(intent)
    }

    private fun openSectionAsFragment(menuSection: MenuSection) {

        val intent = Intent(this, MainActivity::class.java).apply {
            setAction(ACTION_OPEN_SECTION_FIND)
            putExtra(EXTRAS_SECTION_BUNDLE_NAME, menuSection.bundleName)
            putExtra(EXTRAS_SECTION_TITLE, menuSection.title)
            putExtra(EXTRAS_SECTION_ID, menuSection.databaseId)
        }
        startActivity(intent)
    }
}

const val ACTION_OPEN_SECTION_RIBBON = "android.intent.action.sectionribbon"
const val SECTION_ID_KEY = "section_id"
const val SECTION_LIST_KEY = "sections_list"