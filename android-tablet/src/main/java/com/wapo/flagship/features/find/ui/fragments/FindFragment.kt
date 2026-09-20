package com.wapo.flagship.features.find.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.find.events.FindClickEvent
import com.wapo.flagship.features.find.model.FindScreenUIState
import com.wapo.flagship.features.find.model.HighlightBoxType
import com.wapo.flagship.features.find.ui.components.FindScreen
import com.wapo.flagship.features.find.viewmodel.FindViewModel
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.flagship.features.search2.navigation.SearchActivityNavigation
import com.wapo.flagship.features.search2.ui.Search2Activity
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavEvent
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FindFragment : Fragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private val findViewModel: FindViewModel by viewModels()
    private val sectionNavViewModel: SectionNavViewModel by activityViewModels()
    private val askQuestionsViewModel: AskQuestionsViewModel by activityViewModels()

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Measurement.setNavigationBehavior(NavigationBehavior.BACK_TO_FRONT)
            Measurement.trackBottomTabNavigation(BottomTab.Search.route)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.SearchRenderEvent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AndroidClassicTheme {
                    val uiState = findViewModel.uiState.collectAsStateWithLifecycle()

                    FindScreen(findViewModel, askQuestionsViewModel)

                    LaunchedEffect(uiState.value) {
                        if (uiState.value is FindScreenUIState.Success || uiState.value is FindScreenUIState.Failure) {
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SearchRenderEvent)
                        }
                    }
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        findViewModel.loadPage()
        observePageEngagement()
        observeClicks()
        observeSectionNavViewModelEvents()
    }

    override fun onResume() {
        super.onResume()
        findViewModel.reloadRecentsAndHighlights()
    }

    private fun observeSectionNavViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionNavViewModel.sectionNavEvent.collect { event ->
                    when(event) {
                        is SectionNavEvent.OpenInWebView -> {
                            activity?.let {
                                Utils.startWebActivity(event.url, it)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observePageEngagement() {
        lifecycle.addObserver(
            PageEngagementLifecycleObserver(
                pageName = Measurement.getTrackingPageName(BottomTab.Search.trackingName),
                tabName = BottomTab.Search.title,
                contentType = Measurement.CONTENT_TYPE_FRONT
            )
        )
    }

    private fun observeClicks() {
        findViewModel.clickEvent.observe(viewLifecycleOwner) { clickEvent: FindClickEvent ->
            when (clickEvent) {
                is FindClickEvent.HighlightClick -> {
                    when (clickEvent.type) {
                        HighlightBoxType.PRINT -> {
                            Measurement.trackPrintEditionSection(NavigationBehavior.FIND_TAB_HIGHLIGHT.value)
                            sectionNavViewModel.openPrint()
                        }
                        HighlightBoxType.COMICS -> sectionNavViewModel.openComics() // open comics
                        HighlightBoxType.RECIPES -> openRecipes() // open recipes page in search
                        HighlightBoxType.NEWSPRINT -> openNewsprint(clickEvent.navigationBehavior) // open newsprint web view
                        HighlightBoxType.CLIMATE -> openClimateBot() // open webview of climate answers
                        HighlightBoxType.ELECTIONS -> openElectionSearch() // open elections
                        HighlightBoxType.HOROSCOPES -> sectionNavViewModel.openHoroscope()
                        HighlightBoxType.RIPPLE -> sectionNavViewModel.openRipple()
                    }
                }

                FindClickEvent.SearchBarClick -> openSearch() // open section in find tab

                is FindClickEvent.SectionClick ->
                    sectionNavViewModel.openSection(
                        clickEvent.bundleId,
                        clickEvent.navType,
                    ) // open section in find tab

                is FindClickEvent.QuestionItemClick -> {
                    SearchActivityNavigation.searchQuestion(
                        clickEvent.id,
                        clickEvent.question,
                        requireActivity(),
                    )
                }

                is FindClickEvent.DeepLinkItemClick -> {
                    DeepLinksProcessor.processAsync(clickEvent.link, scope = lifecycleScope)
                }
            }
        }
    }

    private fun openElectionSearch() {
        val intent = Intent(activity, Search2Activity::class.java)
        intent.putExtra("nav", NavigationBehavior.FIND_TAB_HIGHLIGHT.value)
        intent.putExtra("type", "election")
        startActivity(intent)
    }

    private fun openClimateBot() {
        ArticlesParcel
            .builder()
            .setArticleSingleUrl(requireContext().resources.getString(R.string.climate_bot_url))
            .buildIntent(requireContext())
            .also { intent ->
                requireContext().startActivity(intent)
            }
    }

    private fun openSearch() {
        val intent = Intent(activity, Search2Activity::class.java)
        resultLauncher.launch(intent)
    }

    private fun openRecipes() {
        val intent = Intent(activity, Search2Activity::class.java)
        intent.putExtra("nav", NavigationBehavior.FIND_TAB_HIGHLIGHT.value)
        intent.putExtra("type", "recipes")
        resultLauncher.launch(intent)
    }

    private fun openNewsprint(navigationBehavior: NavigationBehavior) {
        Measurement.setNavigationBehaviorInDefaultMap(navigationBehavior.value)
        val link = NewsprintHelper.getNewsprintUrlWithItId(navigationBehavior.value)
        context?.findActivityOfType<SectionActivity>()?.openWeb(link)
    }
}
