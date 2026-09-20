package com.wapo.flagship.features.search2.ui

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.chip.Chip
import com.wapo.Utils.convertDpToPixel
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.FusionActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.fusion.viewmodel.EllipsisMenuViewModel
import com.wapo.flagship.features.grid.FusionSectionFragment
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.mypost.fragments.RemoveConfirmationFragment
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.fragments.PostAnswersInfoBottomSheetFragment
import com.wapo.flagship.features.search2.fragments.SearchFilterFragment
import com.wapo.flagship.features.search2.model.FilterHeaderItem
import com.wapo.flagship.features.search2.model.QueryFilter
import com.wapo.flagship.features.search2.model.RecipeItem
import com.wapo.flagship.features.search2.model.SearchQueryItem
import com.wapo.flagship.features.search2.navigation.SearchActivityNavigation
import com.wapo.flagship.features.search2.navigation.SearchActivityNavigation.QUESTION_ID
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.viewmodel.FilterViewModel
import com.wapo.flagship.features.search2.viewmodel.SearchMode
import com.wapo.flagship.features.search2.viewmodel.SearchViewModel
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.util.tracking.Evars
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_URL
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.ActivitySearch2Binding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.types.MyPostSection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Search Feature Activity. Allows users to search for sections / articles
 * and also filter search results
 */
@AndroidEntryPoint
class Search2Activity :
    FusionActivity() {
    private lateinit var binding: ActivitySearch2Binding

    private val searchViewModel: SearchViewModel by viewModels()
    private val filterViewModel: FilterViewModel by viewModels()
    private val ellipsisMenuViewModel: EllipsisMenuViewModel by viewModels()

    /**
     * Keyword is set to true by default. This is used to determine what search type was used (voice_search, search_recent_search...)
     * If other type besides keyword was used, keywordSearch is set to false. Once the event is fired, keywordSearch is set back to true.
     */
    private var keywordSearch = true

    private var recipeDeeplink: URLParser? = null

    private var electionDeeplink: URLParser? = null

    private lateinit var systemBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearch2Binding.inflate(layoutInflater)
        observerUiState()
        observerUserEvent()
        onSearchHandle()
        observeFilterUpdate()
        observeEllipsisClick()
        observeGiftTapEvents()
        observeActionShare()
        onAppBarBack()
        onSystemBackPressed()
        observeAddedToPlaylistEvent()
        observeAddToPlayListTapEvents()
        observeSaveRecipeClickEvent()

        setContentView(binding.root)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.searchBar.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        binding.searchBar.requestFocus()
        binding.filter.setOnClickListener {
            when (searchViewModel.searchMode.value) {
                SearchMode.Recipe -> onRecipeFilterClick(null)
                else -> performFiltering()
            }
        }

        binding.voiceSearchButton.setOnClickListener {
            launchVoiceRecognition()
        }

        loadRecipeQuickFilters()
        setupRecipeButton()
        handleSearchViewFocus()

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    /**
     * Generate Recipe Quick Filter Chips based on Config data
     */
    private fun loadRecipeQuickFilters() {
        binding.recipeFilterContainer.removeAllViews()
        filterViewModel.recipesFilterMapCache?.keys?.filter { it.isQuickFilter }?.forEach { group ->
            val chip = Chip(ContextThemeWrapper(this, R.style.search_filter_chip))
            val layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            layoutParams.marginStart = convertDpToPixel(8f, this).toInt()
            chip.layoutParams = layoutParams
            chip.chipIconSize = convertDpToPixel(16f, this)
            chip.iconStartPadding = convertDpToPixel(12f, this)
            chip.text = group.label
            // We want the tag to be FilterHeaderItem so we can access which queryId it belongs to
            // and use that id to set it's active state.
            chip.tag = group
            chip.setChipBackgroundColorResource(R.color.filter_chip_fill)
            chip.setChipStrokeWidthResource(R.dimen.chip_stroke_width)
            chip.setVisible(false)
            chip.setOnClickListener {
                onRecipeFilterClick(group)
            }
            updateQuickFilterChip(chip)
            binding.recipeFilterContainer.addView(chip)
        }
    }

    /**
     * Update quick filter chips to reflect which ones have active filters.
     */
    private fun updateQuickFiltersChips() {
        binding.recipeFilterContainer.children.forEach {
            updateQuickFilterChip(it)
        }
    }

    /**
     * Update quick filter chip if it has any active filters
     */
    private fun updateQuickFilterChip(it: View) {
        (it as? Chip)?.apply {
            val header = tag as? FilterHeaderItem
            val isActive =
                header?.let { headerItem ->
                    filterViewModel.isQuickFilterActive(headerItem.queryName)
                } ?: false

            when {
                isActive -> {
                    setChipStrokeColorResource(R.color.filter_chip_active_border)
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.filter_chip_active_text_color,
                        ),
                    )
                }

                else -> {
                    setChipStrokeColorResource(R.color.filter_chip_border)
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.filter_chip_text_color,
                        ),
                    )
                }
            }

            header?.let { h ->
                getQuickFilterIconId(h, isActive)?.let { id ->
                    setChipIconResource(id)
                }
            }
        }
    }

    /**
     * Get appropriate icon for quick filter based on active state
     */
    private fun getQuickFilterIconId(
        group: FilterHeaderItem,
        isActive: Boolean = false,
    ): Int? =
        when (group.queryName) {
            "time" -> if (isActive) R.drawable.recipe_ready_in_active else R.drawable.recipe_ready_in
            "course" -> if (isActive) R.drawable.recipe_course_type_active else R.drawable.recipe_course_type
            "diet" -> if (isActive) R.drawable.recipe_diet_active else R.drawable.recipe_diet
            else -> null
        }

    private fun onRecipeFilterClick(filterGroup: FilterHeaderItem?) {
        filterViewModel.recipesFilterMapCache?.forEach {
            it.key.isCollapsed =
                when (filterGroup) {
                    null -> false
                    else -> it.key.label != filterGroup.label
                }
        }
        filterViewModel.recipesFilterMapCache?.let {
            filterViewModel.loadFilters(it, false)
        }
        performFiltering()
    }

    /**
     * Implementing the confirmation pop up when user un_save from the book mark
     */
    private fun observeSaveRecipeClickEvent() {
        myPost2ViewModel.saveClickEvent.observe(this) {
            RemoveConfirmationFragment().show(
                supportFragmentManager,
                RemoveConfirmationFragment.tag,
            )
        }
        myPost2ViewModel.liveUnsavedArticle.observe(this) {
            if (myPost2ViewModel.unsaveArticle.value != null) {
                it?.let {
                    val actionItem = myPost2ViewModel.saveClickEvent.value
                    actionItem?.section?.let { section ->
                        myPost2ViewModel.removeArticleFromList(it)
                        myPost2ViewModel.clearSaveConfirmClickEvent()
                        recipeBookmarkView?.setImageResource(com.wapo.view.R.drawable.ic_bookmark_unsaved)
                        Measurement.trackSaveUnsave(
                            actionItem.recipePageName,
                            "",
                            "",
                            "",
                            true,
                            false,
                            false,
                            actionItem.url,
                        )
                    }
                }
            }
        }
    }

    private fun setupRecipeButton() {
        searchViewModel.searchMode.observe(this) {
            // when switching search modes, resetting filters and clearing cache
            filterViewModel.queryFilters.resetFilters()
            filterViewModel.resetFilters()
            filterViewModel.filterMapCache.clear()

            updateFilterVisibility(it)
            binding.filter.setVisible(searchViewModel.isFilterVisible)
            binding.filterLabel.setVisible(searchViewModel.isFilterVisible)
            when (it) {
                SearchMode.Regular -> {
                    filterViewModel.defaultSearchModeChanged = false
                    binding.searchBar.queryHint = resources.getString(R.string.search_hint)
                }

                SearchMode.Recipe -> {
                    filterViewModel.defaultSearchModeChanged = true
                    binding.searchBar.queryHint = resources.getString(R.string.recipe_search_hint)
                    // TODO: Hookup config filter list
                    filterViewModel.recipesFilterMapCache?.let { map ->
                        filterViewModel.loadFilters(map, false)
                        handleRecipeDeeplink() // filters must be loaded first
                    }
                }

                SearchMode.Election -> {
                    binding.searchBar.queryHint = resources.getString(R.string.election_search_hint)
                }

                else -> {}
            }
        }
    }

    private fun updateFilterVisibility(searchMode: SearchMode?) {
        for (view in binding.recipeFilterContainer.children) {
            if (view.tag is FilterHeaderItem) {
                when (searchMode) {
                    SearchMode.Recipe -> view.visibility = View.VISIBLE
                    else -> view.visibility = View.GONE
                }
            }
        }
    }

    private fun handleRecipeDeeplink() {
        recipeDeeplink?.let { urlParser ->
            if (urlParser.isValid()) {
                var shouldPerformSearch = false
                urlParser.getQueryParameterNames().forEach { key ->
                    val value = urlParser.getQueryParameter(key)?.trim()
                    Logger.d("RecipeDeeplink", "$key - $value")
                    if (!value.isNullOrBlank()) {
                        when (key) {
                            QUERY -> {
                                // populate search bar with query but wait for all filters before performing search
                                binding.searchBar.setQuery(value, false)
                                searchViewModel.searchQuery =
                                    QueryFilter(
                                        value,
                                    )
                                shouldPerformSearch = true
                            }

                            FOCUS -> {
                                binding.searchBar.requestFocus()
                            }

                            else -> {
                                // treat any parameter other than Query as a Filter Group
                                val filterIds = value.split(',')
                                filterViewModel.updateActiveFiltersById(key, filterIds)
                                shouldPerformSearch = true
                            }
                        }
                    }
                }
                if (shouldPerformSearch) {
                    filterViewModel.filterClicked(FilterEvent.Apply) // submit search with filters
                }
            }
            recipeDeeplink = null // clear deeplink data after it has been used
        }
    }

    private fun launchVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        voiceRecognitionLauncher.launch(intent)
    }

    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val query = results?.firstOrNull()
            if (!query.isNullOrEmpty()) {
                Intent(Intent.ACTION_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)

                    searchViewModel.addEventType(Evars.SEARCH_VOICE.variable)
                    keywordSearch = false
                    binding.searchBar.setQuery(query, true)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let {
            setIntent(it)
            handleIntent(it)
        }
    }

    private fun handleIntent(intent: Intent) {
        Logger.d("Search2Activity", "Intent data=${intent.data?.toString()}")
        if (intent.data?.toString()?.contains("recipes") == true ||
            intent.getStringExtra(
                "type",
            ) == "recipes"
        ) {
            recipeDeeplink = intent.getParcelableExtra(DeepLinksProcessor.ARG_URL_PARSER)
            searchViewModel.recipeNavigationBehavior =
                if (intent.getStringExtra("nav") != null) {
                    intent.getStringExtra("nav") ?: ""
                } else if (recipeDeeplink?.getQueryParameterNames()?.contains(FOCUS) != true) {
                    Measurement.RECIPE_FINDER_DEEPLINK_NAVIGATION
                } else {
                    Measurement.RECIPE_FINDER_INLINE_SEARCH_NAVIGATION
                }
            searchViewModel.setSearchMode(SearchMode.Recipe)
        } else if (intent.data?.toString()?.contains("election") == true ||
            intent.getStringExtra(
                "type",
            ) == "election"
        ) {
            electionDeeplink = intent.getParcelableExtra(DeepLinksProcessor.ARG_URL_PARSER)
            searchViewModel.setSearchMode(SearchMode.Election)
            if (electionDeeplink?.getQueryParameterNames()?.contains(FOCUS) == true) {
                Measurement.trackSubNavItemClick(Measurement.ELECTION_INLINE_SEARCH_NAVIGATION)
            }
            searchViewModel.electionSearchPageName = "election_search_main"
        } else {
            // if we didn't deeplink to Recipes, initial state is Regular
            searchViewModel.setSearchMode(SearchMode.Regular)
        }
    }

    private fun onAppBarBack() {
        binding.backButton.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun performFiltering() {
        supportFragmentManager.let {
            val filterFragment = SearchFilterFragment()
            filterFragment.show(it, "FilterFragment")
        }
    }

    // Handling the SearchView focus after the user interacts with it.
    private fun handleSearchViewFocus() {
        val searchEditText = binding.searchBar.getCustomAutoCompleteTextView()
        binding.searchBar.setOnQueryTextFocusChangeListener { view, hasFocus ->
            searchEditText.post {
                if (hasFocus) {
                    searchEditText.setSelection(searchEditText.text.length)
                }
            }
        }
    }

    // Extension function to retrieve the SearchAutoComplete field from SearchView
    private fun SearchView.getCustomAutoCompleteTextView(): AppCompatAutoCompleteTextView = findViewById(
        com.prolificinteractive.materialcalendarview.R.id.search_src_text)

    private fun observerUiState() {
        searchViewModel.searchUiState.observe(this) {
            updateSearchCount(it)
            when (it) {
                is SearchUiState.Landing ->
                    findNavController(R.id.nav_host_fragment).navigate(
                        R.id.action_to_landing,
                    )
                is SearchUiState.RecipeLanding -> {
                    ConfigManager.getInstance().config.recipesConfig.landingPage.also {
                        val bundle =
                            Bundle().apply {
                                putString(FusionSectionFragment.ARG_BUNDLE_NAME, it)
                                putString(FusionSectionFragment.ARG_DISPLAY_NAME, "Recipes")
                                putString(FusionSectionFragment.ARG_DISPLAY_CONTEXT, "Search")
                            }
                        findNavController(R.id.nav_host_fragment).navigate(
                            R.id.action_to_recipe_landing,
                            bundle,
                        )
                    }
                }

                is SearchUiState.ElectionLanding -> {
                    findNavController(R.id.nav_host_fragment).navigate(
                        R.id.action_to_election_landing,
                    )
                }

                is SearchUiState.Success ->
                    findNavController(R.id.nav_host_fragment).navigate(
                        R.id.action_to_search,
                    )
                is SearchUiState.Failure, is SearchUiState.NoMatches ->
                    findNavController(
                        R.id.nav_host_fragment,
                    ).navigate(
                        R.id.action_to_no_matches,
                    )

                else -> {
                    // no-op
                }
            }
        }
    }

    private fun updateSearchCount(uiState: SearchUiState) {
        when (uiState) {
            is SearchUiState.Success -> {
                if (searchViewModel.searchMode.value != SearchMode.Recipe) {
                    // Add filters from response to viewmodel cache
                    if (filterViewModel.defaultSearchModeChanged) {
                        searchViewModel.searchFor(
                            QueryFilter(
                                searchViewModel.searchQuery?.query ?: "",
                            ),
                        )
                    }
                    filterViewModel.loadFilters(uiState.filters)
                }

                // Update search bar UI
                binding.filter.setVisible(searchViewModel.isFilterVisible)
                binding.filterLabel.setVisible(searchViewModel.isFilterVisible)
            }

            is SearchUiState.NextPageLoaded, is SearchUiState.NextPageLoading -> {} // no-op
            else -> {
                binding.filter.setVisible(searchViewModel.isFilterVisible)
                binding.filterLabel.setVisible(searchViewModel.isFilterVisible)
            }
        }
    }

    private fun observerUserEvent() {
        searchViewModel.userEvent.observe(this) {
            when (it) {
                is UserEvent.ArticleItemClick -> openArticle(it.item.contentUrl, it.position)
                is UserEvent.RecipeItemClick -> openRecipeArticle(it.item.contentUrl, it.position)
                is UserEvent.SearchElectionItemClick ->
                    it.item.path?.let { it1 ->
                        openElectionArticle(
                            it1,
                            it.position,
                        )
                    }
                is UserEvent.SearchQueryItemClick -> {
                    searchViewModel.addEventType(
                        Evars.SEARCH_RECENT_SEARCH.variable,
                    )
                    keywordSearch = false
                    binding.searchBar.setQuery(it.item.query, true)
                }

                is UserEvent.SectionItemClick ->
                    if (it.item.type.contentEquals("web")) {
                        DeepLinksProcessor.processAsync(it.item.path, scope = lifecycleScope)
                    } else {
                        openSection(
                            it.item.url,
                            searchViewModel.searchQuery?.query ?: "",
                        )
                    }

                is UserEvent.ExpandableItemClick ->
                    searchViewModel.loadMoreSections(
                        it.item.expanded,
                    )
                is UserEvent.RemoveAllItemsClick ->
                    when (it.group) {
                        SearchQueryItem::class -> searchViewModel.clearAllRecentQueries()
                        else -> {} // no-op
                    }

                is UserEvent.RemoveItemClick ->
                    when (it.item) {
                        is SearchQueryItem -> searchViewModel.removeRecentQuery(it.item.id)
                        else -> {} // no-op
                    }
                //    Search Recipe Results Bookmark Event
                is UserEvent.RecipeBookmarkClick -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val isSaved = ellipsisMenuViewModel.isSaved(it.item.contentUrl)
                        withContext(Dispatchers.Main) {
                            if (it.isLoading) {
                                updateBookmarkIcon(it.view, isSaved)
                            } else {
                                manageRecipeBookmarkClick(isSaved, it.item, it.view)
                            }
                        }
                    }
                }

                is UserEvent.PostAnswersCarouselItemClick -> {
                    searchViewModel.searchQuery?.query?.let { query ->
                        Measurement.setPostAnswersCarouselInfo(
                            it.carouselPosition,
                            query,
                            searchViewModel.searchQuery?.queryId,
                        )
                    }
                    searchViewModel.articleWasOpened = true
                    openArticle(it.url, it.carouselPosition)
                }
                is UserEvent.PostAnswersFeedbackItemClick -> {
                    searchViewModel.initiatePostAnswersFeedback(
                        it.endpoint,
                        it.responseId,
                        it.reaction,
                    )
                }
                is UserEvent.PostAnswerShowMoreClick -> {
                    searchViewModel.searchQuery?.query?.let { query ->
                        Measurement.trackPostAnswersShowMoreClick(
                            searchViewModel?.searchQuery?.queryId,
                            query,
                            searchViewModel.searchType,
                        )
                    }
                }
                is UserEvent.PostAnswersInfoClick -> {
                    PostAnswersInfoBottomSheetFragment().show(
                        supportFragmentManager,
                        PostAnswersInfoBottomSheetFragment.TAG,
                    )
                }
                is UserEvent.AskQuestionItemClick -> {
                    keywordSearch = false
                    searchViewModel.navigationBehavior = NavigationBehavior.FIND_TAB_QUESTIONS.value
                    intent.putExtra(QUESTION_ID, it.id)
                    binding.searchBar.setQuery(it.question, true)
                }

                is UserEvent.DeepLinkItemClick -> {
                    DeepLinksProcessor.processAsync(it.link, scope = lifecycleScope)
                }

                else -> {}
            }
        }
    }

    private fun manageRecipeBookmarkClick(
        isCurrentlySaved: Boolean,
        item: RecipeItem,
        view: ImageView,
    ) {
        if (!shouldWallRecipeBookmark()) {
            if (isCurrentlySaved) { // if already saved, remove from saved stories
                // don't track here; wait for confirmation dialog
                recipeBookmarkView = view
                val action =
                    ArticleActionItem(
                        MyPostSection.SAVED_STORIES,
                        item.contentUrl,
                        false,
                        null,
                        Measurement.PAGE_SEARCH_RESULTS,
                    )
                myPost2ViewModel.handleSaveClickEvent(action)
            } else { // if not saved, add to saved stories
                view.setImageResource(com.wapo.view.R.drawable.ic_bookmark_saved)
                val action =
                    EllipsisActionItem(
                        url = item.contentUrl,
                        imageUrl = item.imageUrl ?: "",
                        menuType = EllipsisMenu.Carousel,
                    )
                ellipsisMenuViewModel.saveArticle(action)
                Measurement.trackSaveUnsave(
                    Measurement.PAGE_SEARCH_RESULTS,
                    "",
                    "",
                    "",
                    true,
                    true,
                    false,
                    action.url,
                )
            }
        }
    }

    private fun updateBookmarkIcon(
        view: ImageView,
        isSaved: Boolean,
    ) {
        if (isSaved) {
            view.setImageResource(com.wapo.view.R.drawable.ic_bookmark_saved)
        } else {
            view.setImageResource(com.wapo.view.R.drawable.ic_bookmark_unsaved)
        }
    }

    private fun observeFilterUpdate() {
        filterViewModel.filterEvent.observe(this) {
            when (it) {
                FilterEvent.Apply -> {
                    val isQueryFilterSame =
                        searchViewModel.searchQuery?.isFilterSame(filterViewModel.queryFilters)
                    if (searchViewModel.searchQuery == null && searchViewModel.searchMode.value == SearchMode.Recipe) {
                        searchViewModel.searchQuery =
                            QueryFilter("")
                    }
                    searchViewModel.searchQuery?.query?.let { query ->
                        filterViewModel.queryFilters.query = query
                    }
                    if (isQueryFilterSame != true) {
                        searchViewModel.searchQuery?.apply {
                            copyFilters(filterViewModel.queryFilters)
                            searchViewModel.searchFor(this)
                        }
                    }
                    updateQuickFiltersChips()
                    dismissKeyboard()
                }

                else -> {} // no-op
            }
        }
    }

    /**
     * Observing events for the given playlist audio is added [PlaylistActivityViewModel.addToPlaylist] to the DB.
     * Right now the event is dispatched only for the successful cases. No events for failures.
     */
    private fun observeAddedToPlaylistEvent() {
        playlistActivityViewModel.addedToPlaylistEvent.observe(this) {
            addToPlaylistSnackbar(binding.root) {
                Measurement.trackActionButtonArticleAddToPlaylist()
                val intent = IntentHelper.getMainActivityIntent(this)
                intent.putExtra(
                    EXTRAS_SECTION_URL,
                    "https://www.washingtonpost.com/tablet/listen-to-the-post/",
                )
                intent.setAction(ACTION_OPEN_SECTION)
                startActivity(intent)
            }
        }
    }

    private fun onSearchHandle() {
        binding.searchBar.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(text: String?): Boolean {
                    text?.trimEnd()?.let {
                        var queryId: String? = null
                        if (keywordSearch) {
                            searchViewModel.addEventType(Evars.SEARCHED_KEYWORD.variable)
                            searchViewModel.navigationBehavior = null
                        } else {
                            queryId = intent.getStringExtra(QUESTION_ID)
                        }
                        keywordSearch = true
                        searchViewModel.searchQuery?.apply {
                            query = it
                            searchViewModel.searchFor(this)
                        } ?: searchViewModel.searchFor(
                            QueryFilter(
                                query = it,
                                queryId = queryId,
                            ),
                        )
                        searchViewModel.addRecentQuery(queryId ?: it, it)
                        filterViewModel.queryFilters.apply {
                            query = it
                            this.queryId = queryId
                        }
                    }
                    updateMicVisibility()
                    dismissKeyboard()
                    val searchEditText = binding.searchBar.getCustomAutoCompleteTextView()
                    searchEditText.setSelection(0)
                    return true
                }

                override fun onQueryTextChange(text: String?): Boolean {
                    if (text.isNullOrEmpty()) {
                        if (searchViewModel.searchMode.value == SearchMode.Recipe &&
                            filterViewModel.queryFilters.hasRecipeQueryOrFilters()
                        ) {
                            searchViewModel.searchQuery?.query = ""
                            filterViewModel.filterClicked(FilterEvent.Apply)
                        } else {
                            searchViewModel.searchQuery = null
                            searchViewModel.showLandingPage()
                        }
                    }
                    updateMicVisibility()
                    return true
                }
            },
        )
    }

    private fun updateMicVisibility() {
        binding.voiceSearchButton.visibility =
            if (binding.searchBar.query.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    private fun dismissKeyboard() {
        binding.searchBar.clearFocus()
    }

    private fun openArticle(
        url: String,
        position: Int,
    ) {
        if (searchViewModel.searchMode.value == SearchMode.Election) {
            searchViewModel.searchType = "sr_elections_$position"
            searchViewModel.articleWasOpened = true
        }
        searchViewModel.articleWasOpened = true
        SearchActivityNavigation.openArticle(
            url,
            this,
            searchViewModel.navigationBehavior ?: searchViewModel.searchType,
        )
    }

    private fun openElectionArticle(
        url: String,
        position: Int,
    ) {
        searchViewModel.articleWasOpened = true
        SearchActivityNavigation.openElectionArticle(
            url,
            this,
            searchViewModel.searchType,
            position,
        )
    }

    private fun openRecipeArticle(
        url: String,
        position: Int,
    ) {
        searchViewModel.articleWasOpened = true
        SearchActivityNavigation.openRecipeArticle(url, this, searchViewModel.searchType, position)
    }


    fun onSystemBackPressed() {
        if (::systemBackPressedCallback.isInitialized) {
            return
        }
        systemBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        onBackPressedDispatcher.addCallback(this, systemBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        searchViewModel.articleStatus()
    }

    private fun openSection(
        id: String,
        searchQuery: String,
    ) {
        SearchActivityNavigation.openSection(id, searchQuery, this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun checkConnectivity() {
    }

    override fun getPersoPodcastViewModel(): PersonalizedPodcastViewModel? {
        return null
    }


    // TODO: Provide proper app section name for analytics
    override fun getAppSection(): String? = "Recipes"

    override fun getCustomizedSections(): MutableList<Section> = mutableListOf()

    override fun getPersistentPlayerFrame(): FrameLayout = binding.persistentPlayerFrame

    companion object {
        const val SCREEN_NAME = "Search"
        const val QUERY = "q"
        const val FOCUS = "_focus"
    }
}
