package com.wapo.flagship.navigation.ui

import android.os.Bundle
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.flagship.features.ask.ui.AskFragment
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.find.ui.fragments.FindFragment
import com.wapo.flagship.features.games.GamesFragment
import com.wapo.flagship.features.listen.ListenToThePostFragment
import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
import com.wapo.flagship.features.mypost.fragments.MyPost2Fragment
import com.wapo.flagship.features.print.ArchivesFragment
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.wapo.flagship.features.sections.SectionFrontsFragment
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel
import com.wapo.flagship.features.wpvideos.fragments.WatchVideoFragment
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarEvent
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavEvent
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.customnav.viewmodel.CustomNavViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomTabFragment : Fragment() {
    private val sectionNavViewModel: SectionNavViewModel by activityViewModels()
    private val sectionTrackingViewModel: SectionTrackingViewModel by activityViewModels()
    private val customNavViewModel: CustomNavViewModel by activityViewModels()
    private val navBarViewModel: NavBarViewModel by activityViewModels()
    private val lowDataModeViewModel: LowDataBannerViewModel by activityViewModels()
    private val sectionsRibbonViewModel: SectionsRibbonViewModel by activityViewModels()
    private val askThePostViewModel: AskThePostViewModel by activityViewModels()
    private val config get() = ConfigManager.getInstance().config

    companion object {
        const val LIMIT_RECENT_LIST = 5
        const val KEY = "FragmentKey"

        fun newInstance(item: BottomTab): Fragment {
            val fragment = BottomTabFragment()
            val argument = Bundle()
            argument.putString(KEY, item.route)
            fragment.arguments = argument
            return fragment
        }
    }

    private var count = 0
    private var key: String = "home"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (savedInstanceState != null) {
            count = childFragmentManager.backStackEntryCount
            navBarViewModel.showBackButton(count > 1)
        }
        return inflater.inflate(R.layout.fragment_container, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            key = it.getString(KEY) ?: "home"
            if (count == 0) {
                loadFragment()
            }
        }
        childFragmentManager.addOnBackStackChangedListener {
            count = childFragmentManager.backStackEntryCount
            navBarViewModel.showBackButton(count > 1)
            onCurrentFragmentLoad()
            sectionNavViewModel.setTopBarState(TopBarState.EXPANDED)
        }

        trackTabChangeForRoot()
        observeSectionNavViewModelEvents()
        observeLowDataMode()

        // using this to handle system config changes while user is in a conversation
        observeNavBarEvents()
    }

    /**
     * Currently only used for Watch tab page views.
     * Home, Listen, Games fire a page view on their section fronts.
     * My Post fires a page view depending on which My Post section user is in.
     */
    private fun trackTabChangeForRoot() {
        if (key != BottomTab.Watch.route || count > 1) {
            return
        }

        Measurement.trackBottomTabNavigation(key)
    }

    private fun loadFragment() {
        val fragment =
            when (key) {
                BottomTab.Listen.route -> {
                    sectionsRibbonViewModel.removeLastViewedSection()
                    sectionsRibbonViewModel.appOpenedOnSection()
                    ListenToThePostFragment().create(
                        config.listenContentUrl,
                        "audio",
                    )
                }
                BottomTab.Ask.route -> {
                    sectionsRibbonViewModel.removeLastViewedSection()
                    AskFragment()
                }

                BottomTab.Games.route -> {
                    sectionsRibbonViewModel.removeLastViewedSection()
                    sectionsRibbonViewModel.appOpenedOnSection()
                    GamesFragment().create(
                        config.gamesContentUrl,
                        BottomTab.Games.title
                    )
                }
                BottomTab.Search.route -> {
                    sectionsRibbonViewModel.removeLastViewedSection()
                    sectionsRibbonViewModel.appOpenedOnSection()
                    FindFragment()
                }
                BottomTab.Print.route -> {
                    sectionsRibbonViewModel.removeLastViewedSection()
                    sectionsRibbonViewModel.appOpenedOnSection()
                    ArchivesFragment()
                }
                BottomTab.Watch.route -> {
                    sectionsRibbonViewModel.removeLastViewedSection()
                    sectionsRibbonViewModel.appOpenedOnSection()
                    WatchVideoFragment().create(
                        "/video",
                        "Watch"
                    )
                }
                else ->
                    SectionFrontsFragment().apply {
                        updatePageWith(null, null, null)
                    }
            }
        childFragmentManager
            .beginTransaction()
            .replace(R.id.container_fragment, fragment, key)
            .addToBackStack(key)
            .commit()
    }

    private fun updateHome() {
        val fragment = childFragmentManager.findFragmentByTag("home")
        if (count == 1 && fragment is SectionFrontsFragment) {
            val updatedFrag =
                SectionFrontsFragment().apply {
                    updatePageWith(
                        null,
                        null,
                        null,
                        sectionsRibbonViewModel.getCurrentSection()?.id,
                    )
                }

            childFragmentManager
                .beginTransaction()
                .replace(R.id.container_fragment, updatedFrag, key)
                .commitAllowingStateLoss()
        }
    }

    override fun onResume() {
        if (customNavViewModel.reload()) {
            updateHome()
        }
        super.onResume()
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
                        SectionNavEvent.ReloadTitle -> {
                            onCurrentFragmentLoad()
                            sectionNavViewModel.setTopBarState(TopBarState.EXPANDED)
                        }
                        is SectionNavEvent.OpenInWebView -> {
                            DeepLinksProcessor.process(event.url)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeNavBarEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Needed to keep track of the last tab user was on
                var previousTab: BottomTab? = null

                lifecycleScope.launch {
                    navBarViewModel.navBarEvent.collect { event ->
                        when (event) {
                            is NavBarEvent.NavEvent -> {
                                // Only clears conversation data if the user was on the Ask tab
                                // and has just navigated to a different tab.
                                if (previousTab != null && previousTab?.route == BottomTab.Ask.route && event.tab.route != BottomTab.Ask.route) {
                                    askThePostViewModel.clearLiveConversationData()
                                }
                                previousTab = event.tab
                            }

                            is NavBarEvent.TabClickAgain -> {
                                if (event.route == tag) { // ensure handle this event on same fragment
                                    if (count > 1) { // pop to root if more than 1 item on stack
                                        this@BottomTabFragment.trackTabNavigation()
                                        popToRoot()
                                    } else {
                                        when (val fragment =
                                            childFragmentManager.findFragmentByTag(key)) {
                                            is SectionFrontsFragment -> {
                                                // page to top stories if not on top stories or scroll to the top if on top stories
                                                val topStoriesIndex = fragment.topStoriesIndex
                                                if (fragment.pager.currentItem != topStoriesIndex) {
                                                    fragment.setTrackSectionChangeTab()
                                                    fragment.pager.currentItem = topStoriesIndex
                                                } else {
                                                    fragment.scrollToTop(true)
                                                }
                                            }

                                            is BaseSectionFragment -> fragment.scrollToTop()
                                            is MyPost2Fragment -> { // fragment.scrollToTop()
                                            } // TODO
                                            is FindFragment -> { // fragment.scrollToTop()
                                            } // TODO
                                            is AskFragment -> {
                                                if (askThePostViewModel.uiState.value.shareUrlState is ShareUrlState.ShareProcessed) {
                                                    askThePostViewModel.clearLiveConversationData()
                                                    askThePostViewModel.hideOrShowResponse(false)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is NavBarEvent.ShouldPreserveState -> {
                                if (!event.value && count > 1 && key == navBarViewModel.getCurrentTab().route) {
                                    popToRoot()
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun observeLowDataMode() {
        lowDataModeViewModel.lowDataBannerState.observe(viewLifecycleOwner) {
            val isLowDataModeEnable = it.isLowDataBannerEnable
            when (val fragment = childFragmentManager.findFragmentByTag(key)) {
                is SectionFrontsFragment -> {
                    if (fragment.pager != null) {
                        fragment.pager.setIsLowDataMode(isLowDataModeEnable)
                        if (isLowDataModeEnable) {
                            fragment.loadDefaultSection()
                        }
                    }
                }
            }
        }
    }

    private fun openSection(menuSection: MenuSection) {
        // If section can be opened in ribbon, open it there
        if (!openSectionInRibbon(menuSection.databaseId)) {
            // Otherwise open section in a new fragment stack
            openSectionAsFragment(menuSection)
        }
    }

    private fun openSectionInRibbon(sectionId: String): Boolean {
        val fragment = childFragmentManager.findFragmentByTag(getCurrentKey())
        if (fragment is SectionFrontsFragment) {
            return fragment.loadSection(sectionId)
        }
        return false
    }

    private fun openSectionAsFragment(menuSection: MenuSection) {
        val fragment =
            SectionFrontsFragment().also {
                it.updatePageWith(menuSection.bundleName, menuSection.title, menuSection.databaseId)
            }
        val childKey = getNextKey()
        childFragmentManager
            .beginTransaction()
            .replace(R.id.container_fragment, fragment, childKey)
            .addToBackStack(childKey)
            .commitAllowingStateLoss()

        sectionNavViewModel.setSectionTitle()
        sectionNavViewModel.updateRecentSections(menuSection)
    }

    /**
     * Opens Print on the stack, not as a Tab.
     */
    private fun openPrint() {
        val fragment = ArchivesFragment()
        val childKey = getNextKey()
        childFragmentManager
            .beginTransaction()
            .replace(R.id.container_fragment, fragment, childKey)
            .addToBackStack(childKey)
            .commit()
    }

    private fun trackTabNavigation() {
        Measurement.setChangeTab(false) // navigation_behavior should be change_tab when we tap a Bottom Tab to return to the Tab
        Measurement.trackBottomTabNavigation(navBarViewModel.getCurrentTab().trackingName)
    }

    private fun popToRoot() {
        try {
            childFragmentManager.popBackStack(key, 0)
        } catch (ex: IllegalStateException) {
            CrashWrapper.sendException(ex)
        }
    }

    private fun getNextKey(): String = key + (count + 1).toString()

    private fun getCurrentKey(): String =
        if (count > 1) {
            key + count.toString()
        } else {
            key
        }

    private fun onCurrentFragmentLoad() {
        val fragment = childFragmentManager.findFragmentByTag(getCurrentKey())
        //need to figure out a new way to clear data
//        askThePostViewModel.clearLiveConversationData()
        if (count > 1 && fragment != null && fragment is SectionFrontsFragment) {
            fragment.sectionTitle?.let {
                sectionNavViewModel.setSectionTitle(it)
                return
            }
        }

        if (fragment is ArchivesFragment) {
            sectionNavViewModel.setSectionTitle("Print Edition")
            return
        }

        sectionNavViewModel.setSectionTitle(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Logger.d("Track", "ContainerFragment onSaveInstanceState")
    }

    override fun onPause() {
        super.onPause()
        Logger.d("Track", "ContainerFragment onPause")
    }

    override fun onStop() {
        super.onStop()
        Logger.d("Track", "ContainerFragment onStop")
    }

    fun getSectionBundleId(): String? {
        val fragment = childFragmentManager.findFragmentByTag(getCurrentKey())
        if (fragment != null && fragment is SectionFrontsFragment) {
            return fragment.pager?.currentFragment?.bundleName
        }
        return null
    }
}
