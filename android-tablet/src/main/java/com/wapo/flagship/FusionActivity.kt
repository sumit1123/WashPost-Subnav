package com.wapo.flagship

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.wapo.flagship.config.DefaultSectionFragmentFactory
import com.wapo.flagship.config.isTopStoriesSection
import com.wapo.flagship.features.pagebuilder.ClassicAdViewFactory
import com.wapo.flagship.features.ads.targeting.ui.ContentUIState
import com.wapo.flagship.features.ads.targeting.viewmodels.ContentViewModel
import com.wapo.flagship.features.aixp.viewmodels.ArticleSummaryCollaborationViewModel
import com.wapo.flagship.features.aixp.viewmodels.FeedbackCollaborationViewModel
import com.wapo.flagship.features.articles2.activities.ARTICLES_URL_PARAM
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.activities.CURRENT_APP_SECTION
import com.wapo.flagship.features.articles2.activities.CURRENT_TAB_NAME
import com.wapo.flagship.features.articles2.activities.LIVE_VIDEO_ORIGINATED
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfigList
import com.wapo.flagship.features.audio.fragments.AudioPagerFragment
import com.wapo.flagship.features.audio.playlist.toEllipsisActionItem
import com.wapo.flagship.features.audio.playlist.toPlaylistAudio
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel
import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.fusion.FusionEnvironment
import com.wapo.flagship.features.fusion.fragments.EllipsisMenuFragment
import com.wapo.flagship.features.fusion.viewmodel.EllipsisMenuViewModel
import com.wapo.flagship.features.gifting.viewmodels.GiftCollaborationViewModel
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.grid.events.ActionButtonEvent
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import com.wapo.flagship.features.grid.viewmodel.ActionsHelperViewModel
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperViewModel
import com.wapo.flagship.features.grid.viewmodel.sectionsHabitTiles.SectionsHabitTilesViewModel
import com.wapo.flagship.features.grid.views.SavedVerifierActivity
import com.wapo.flagship.features.habittiles.HabitTilesPlugin
import com.wapo.flagship.features.inlineoffer.model.SectionInlineOfferViewModel
import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
import com.wapo.flagship.features.main.viewmodel.FusionActivityViewModel
import com.wapo.flagship.features.mypost.fragments.RemoveConfirmationFragment
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.wapo.flagship.features.nightmode.NightModeProvider
import com.wapo.flagship.features.pagebuilder.AdViewFactory
import com.wapo.flagship.features.pagebuilder.AdViewFactoryProvider
import com.wapo.flagship.features.pagebuilder.AudioView
import com.wapo.flagship.features.pagebuilder.InlineAudioView
import com.wapo.flagship.features.podcast.AudioViewImpl
import com.wapo.flagship.features.podcast.InlineAudioViewImpl
import com.wapo.flagship.features.sections.ConnectivityActivity
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.SectionFragmentFactory
import com.wapo.flagship.features.sections.SectionsPagerView
import com.wapo.flagship.features.sections.SubscribeButton
import com.wapo.flagship.features.sections.model.TargetingContent
import com.wapo.flagship.features.sections.model.TargetingContentUIState
import com.wapo.flagship.features.sections.viewmodels.AudioPlaybackViewModel
import com.wapo.flagship.features.sections.viewmodels.SectionAudioMediaActivityViewModel
import com.wapo.flagship.features.sections.viewmodels.SectionVideoActivityViewModel
import com.wapo.flagship.features.sections.viewmodels.SectionWallHelperViewModel
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.shared.activities.SearchableArticlesActivity
import com.wapo.flagship.features.splash.SplashViewModel
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.sdk.iterable.IterablePlugin
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.wapo.flagship.util.ConnectivityMonitor
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.UIUtil
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.wrappers.CrashWrapper
import com.wapo.view.habittiles.Tile
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.CustomePlaylistSnackbarBinding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import com.washingtonpost.android.recirculation.carousel.viewmodels.CarouselAudioMediaActivityViewModel
import com.washingtonpost.android.save.SaveActivity
import com.washingtonpost.android.save.SaveProvider
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.types.MyPostSection
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import com.washingtonpost.customnav.CustomNavActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class FusionActivity :
    BaseActivity(),
    GridActivity,
    NightModeProvider,
    ImageLoaderProvider,
    ConnectivityActivity,
    SectionActivity,
    AdViewFactoryProvider,
    SaveActivity,
    SavedVerifierActivity,
    IterablePlugin.IterableActivity {

    protected val globalBannerViewModel: GlobalBannerViewModel by viewModels()
    protected val lowDataBannerViewModel: LowDataBannerViewModel by viewModels()
    protected val splashViewModel: SplashViewModel by viewModels()
    protected val giftCollaborationViewModel: GiftCollaborationViewModel by viewModels()
    protected val ellipsisHelperViewModel: EllipsisHelperViewModel by viewModels()
    private val actionsHelperViewModel: ActionsHelperViewModel by viewModels()
    protected val playlistActivityViewModel: PlaylistActivityViewModel by viewModels()
    protected val mainViewModel: FusionActivityViewModel by viewModels()
    protected val summaryCollaborationViewModel: ArticleSummaryCollaborationViewModel by viewModels()
    protected val feedbackCollaborationViewModel: FeedbackCollaborationViewModel by viewModels()
    private val ellipsisMenuViewModel: EllipsisMenuViewModel by viewModels()
    protected val myPost2ViewModel: MyPost2ViewModel by viewModels()
    private val sectionsHabitTilesViewModel: SectionsHabitTilesViewModel by viewModels()
    private val sectionVideoActivityViewModel: SectionVideoActivityViewModel by viewModels()
    private val sectionNavViewModel: SectionNavViewModel by viewModels()
    private val adsContentViewModel: ContentViewModel by viewModels()
    lateinit var habitTilesPlugin: HabitTilesPlugin
    // Iterable
    protected val iterableActivityViewModel: IterableActivityViewModel by viewModels()
    protected lateinit var iterablePlugin: IterablePlugin

    private val sectionWallHelperViewModel: SectionWallHelperViewModel by viewModels()

    val audioPlaybackViewModel: AudioPlaybackViewModel by viewModels()

    private val sectionInlineOfferViewModel: SectionInlineOfferViewModel by viewModels()

    protected lateinit var fusionEnvironment: FusionEnvironment

    private val fusionViewPool = RecyclerView.RecycledViewPool()

    protected val intentHelper = IntentHelper()

    private lateinit var adViewFactory: AdViewFactory

    var recipeBookmarkView: ImageView? = null

    open val pageNameFromTracking: String?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adViewFactory = ClassicAdViewFactory(this)
        fusionEnvironment = FusionEnvironment(this)
        iterablePlugin =
            IterablePlugin(this, iterableActivityViewModel, getPaywallSheetHelper()).also {
                lifecycle.addObserver(it)
            }
    }

    private val hideSubscribeBanner: Boolean
        get() {
            // If value is missing from config, default to showing the banner
            val subscribeBannerEnabled =
                ConfigManager.getInstance().config.paywallConf
                    .frontSubscriptionBanner
                    ?.enabled ?: true
            return !subscribeBannerEnabled
        }

    protected fun observeGiftTapEvents() {
        giftCollaborationViewModel.giftTrackingEvent.observe(this) { pair ->
            val contentUrl = getUrlWithoutParameters(pair.first)
            val appSection = getCurrentSectionDisplayName()
            Measurement.trackGiftSendClicked(
                contentUrl,
                null,
                pair.second.trackingName,
                appSection,
                true,
            )
        }
    }

    protected fun observeEllipsisClick() {
        ellipsisHelperViewModel.ellipsisClickEvent.observe(this) {
            val appSection = getCurrentSectionDisplayName()
            val pageName = getCurrentSectionDisplayName()
            if (it.menuType == EllipsisMenu.ActionButton) {
                Measurement.trackActionButtonUtilityMenuOpen(
                    appSection,
                    Measurement.getTrackingPageName(pageName),
                ) // send tab name if FY Tab, section name if FY Section
            }

            // hide button if url is in audio off list
            val isAudioDisabled = audioManager.isAudioDisabled(it.url)
            EllipsisMenuFragment
                .create(
                    it.menuType,
                    appSection,
                    it.isAudioArticle && isAudioDisabled == false,
                    it.url,
                    it.playlist?.playerType == PlayerType.PODCAST.name ||
                            it.audioMediaConfig?.getPlayerType() == PlayerType.PODCAST,
                    showConfirmationOnly = it.menuType is EllipsisMenu.RemoveActionButton
                ).show(supportFragmentManager, EllipsisMenuFragment.tag)
        }
    }

    protected fun observeActionShare() {
        actionsHelperViewModel.actionEvent.observe(this) {
            when (it) {
                is ActionButtonEvent.Share -> {
                    it.run {
                        Share
                            .Builder()
                            .headline(headline)
                            .byline(byline)
                            .shareUrl(url)
                            .appSection(getCurrentSectionDisplayName())
                            .isActionButton(true)
                            .build()
                            .shareItem(this@FusionActivity)
                    }
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    protected fun observeActionComments() {
        actionsHelperViewModel.actionEvent.observe(this) {
            when (it) {
                is ActionButtonEvent.Comments -> {
                    CommentBottomSheetFragment.showComments(
                        supportFragmentManager,
                        storyID = it.storyId,
                        storyUrl = it.url,
                        storyTitle = it.storyTitle,
                        commentSource = "front - homepage",
                        trackingInfo = it.trackingInfo
                    )
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    protected fun observeSaveOrRemoveArticle() {
        actionsHelperViewModel.actionEvent.observe(this) {
            if (it is ActionButtonEvent.Save || it is ActionButtonEvent.Remove){
                if (PaywallService.getInstance() != null && PaywallService.getInstance().isWpUserLoggedIn) {
                    when (it) {
                        is ActionButtonEvent.Save -> {
                            ellipsisMenuViewModel.saveArticle(it.ellipsisActionItem)
                            Measurement.trackSaveUnsave(
                                "",
                                it.ellipsisActionItem.arcId,
                                it.ellipsisActionItem.contentType,
                                "",
                                true,
                                true,
                                false,
                                it.ellipsisActionItem.url,
                            )
                        }

                        is ActionButtonEvent.Remove -> {
                            // don't track here; wait for confirmation dialog
                            ellipsisHelperViewModel.handleEllipsisClick(
                                EllipsisActionItem(
                                    menuType = EllipsisMenu.RemoveActionButton,
                                    url = it.ellipsisActionItem.url,
                                ),
                            )
                        }

                        else -> {
                            // no-op
                        }
                    }
                } else {
                    getPaywallSheetHelper().showWall(
                        wallName = PaywallConstants.WALL_NAME_SAVE_REGWALL,
                        wallType = PaywallConstants.WallType.SAVE_REGWALL,
                        wallReason = PaywallConstants.WallType.SAVE_REGWALL.ordinal,
                    )
                }
            }
        }
    }

    /**
     * Recipes use the confirmation dialog from MyPost so they need their own observer.
     */
    protected fun observeSaveOrRemoveRecipe() {
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
                        recipeBookmarkView?.setImageResource(com.wapo.view.R.drawable.ic_bookmark_unsaved)
                        myPost2ViewModel.clearSaveConfirmClickEvent()
                        Measurement.trackSaveUnsave(
                            Measurement.PAGE_RECIPE_FINDER_LANDING,
                            "",
                            "",
                            "",
                            true,
                            false,
                            false,
                            it.contentURL,
                        )
                    }
                }
            }
        }
    }

    /**
     * When Add to playlist is clicked from the Sections Ellipsis menu updating the Database
     */
    protected fun observeAddToPlayListTapEvents() {
        audioMediaActivityViewModel.addPlaylistArticleEvent.observe(this) { url ->
            mainViewModel
                .fetchArticleAudioConfig(
                    url,
                    activeTabName,
                    getCurrentSectionDisplayName(),
                    true,
                    AudioPreferences.getAudioPlaybackSpeed(applicationContext),
                    true,
                    "",
                    true,
                    true,
                ).observe(this) {
                    val playlistAudio = it?.toPlaylistAudio()
                    if (playlistAudio != null) {
                        playlistActivityViewModel.addToPlaylist(playlistAudio)
                        Measurement.trackActionButtonAddToPlaylist(getAppSection(), it.arcId)
                    }
                }
        }

        audioMediaActivityViewModel.addPlaylistPodcastEvent.observe(this) {
            playlistActivityViewModel.addToPlaylist(it)
        }
    }

    /**
     * Observe video click event to fetch contextual targeting data from ads content api
     */
    protected fun observeVideoClickEvent() {
        sectionVideoActivityViewModel.videoClickEvent.observe(this) { videoId ->
            adsContentViewModel.getContent(videoId)
        }
    }

    /**
     * Observe Ads contextual content api call response to send its data to pre roll ads
     */
    protected fun observeAdsContentState() {
        adsContentViewModel.uiState.observe(this) { state ->
            if (state.id.isEmpty() || state.id != sectionVideoActivityViewModel.videoClickEvent.value) return@observe
            val uiState =
                when (state) {
                    is ContentUIState.Content ->
                        TargetingContentUIState.Content(
                            state.id,
                            state.items.map {
                                TargetingContent(
                                    id = it.id,
                                    adCall = it.adCall,
                                    permutive = it.permutive,
                                )
                            },
                        )

                    is ContentUIState.Loading -> TargetingContentUIState.Loading(state.id)
                    is ContentUIState.Error -> TargetingContentUIState.Error(state.id)
                    is ContentUIState.Cancelled -> TargetingContentUIState.Cancelled(state.id)
                    is ContentUIState.UITimeout -> TargetingContentUIState.UITimeout(state.id)
                }
            sectionVideoActivityViewModel.updateContentUIState(state.id, uiState)
        }
    }

    /**
     * When User clicks Add to playlist, Popping up custom Bottom sheet snack-bar for the add Playlist confirmation
     */
    protected fun addToPlaylistSnackbar(
        container: View,
        onClickLink: () -> Unit,
    ) {
        val inflater = LayoutInflater.from(this)
        val customAudioSnackbarBinding = CustomePlaylistSnackbarBinding.inflate(inflater)
        val snackBar =
            Snackbar
                .make(
                    container,
                    "",
                    Snackbar.LENGTH_LONG,
                ).setDuration(5000)
        val snackBarView = snackBar.view
        snackBarView.setPadding(0, 0, 0, 0)
        customAudioSnackbarBinding.playlistDescription.setOnClickListener {
            snackBar.dismiss()
            onClickLink()
        }
        customAudioSnackbarBinding.snackbarClose.setOnClickListener {
            snackBar.dismiss()
        }
        (snackBarView as Snackbar.SnackbarLayout).addView(customAudioSnackbarBinding.root, 0)
        (snackBarView).getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT
        snackBar.show()
    }

    /**
     *  Recipe Carousel handler for Save / Unsave actions in Saved Stories.
     *  If user is not logged in and not subscribed, show the regwall for recipe save.
     */
    fun recipeCarouselBookmarkClicked(
        ellipseActionItem: EllipsisActionItem,
        view: ImageView,
        isStatusChecked: Boolean,
    ) {
        val vm = ellipsisMenuViewModel  //  Initialize viewModel on main thread
        if (isStatusChecked) {
            if (!shouldWallRecipeBookmark()) {
                var isSaved: Boolean
                lifecycleScope.launch(Dispatchers.IO) {
                    isSaved = vm.isSaved(ellipseActionItem.url)
                    saveOrUnsaveCarouselRecipe(view, isSaved, isStatusChecked, ellipseActionItem)
                }
            }
        } else {
            var isSaved: Boolean
            lifecycleScope.launch(Dispatchers.IO) {
                isSaved = vm.isSaved(ellipseActionItem.url)
                saveOrUnsaveCarouselRecipe(view, isSaved, isStatusChecked, ellipseActionItem)
            }
        }
    }

    open fun openHabitTileLink(link: String?) {
        habitTilesPlugin.addClickedToHabitTileViewedItem(link)
        DeepLinksProcessor.processAsync(
            link,
            sourceType = DeepLinksProcessor.SourceType.HABIT_TILES,
            scope = lifecycleScope
        )
    }

    open fun openInlineOfferLink(url: String?) {
        url?.let {
            Measurement.trackInlineOfferHomepageClick(url)
            Utils.startWebActivity(url, this, false, true)
        }
    }

    /**
     * Check if Recipe Bookmark should show a wall
     */
    protected fun shouldWallRecipeBookmark(): Boolean =
        if (PaywallService.getInstance()?.isWpUserLoggedIn == false &&
            PaywallService.getInstance()?.isPremiumUser == false
        ) {
            // if user is neither logged in nor subscribed, show Save Recipe regwall
            getPaywallSheetHelper().showWall(
                wallName = PaywallConstants.WALL_NAME_SAVE_RECIPE_REGWALL,
                wallType = PaywallConstants.WallType.SAVE_RECIPE_REGWALL,
                wallReason = PaywallConstants.WallType.SAVE_RECIPE_REGWALL.ordinal,
            )
            true
        } else {
            // if user is logged in or subscribed, show no wall
            false
        }

    /**
     *  Saves or Unsaves a recipe from a carousel
     */
    private fun saveOrUnsaveCarouselRecipe(
        view: ImageView,
        isCurrentlySaved: Boolean,
        isStatusChecked: Boolean,
        ellipsisActionItem: EllipsisActionItem,
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                if (isStatusChecked) {
                    if (isCurrentlySaved) { // if already saved, remove from saved stories
                        // don't track here; wait for confirmation dialog
                        recipeBookmarkView = view
                        val action =
                            ArticleActionItem(
                                MyPostSection.SAVED_STORIES,
                                ellipsisActionItem.url,
                                false,
                                null,
                                Measurement.PAGE_RECIPE_FINDER_LANDING,
                            )
                        myPost2ViewModel.handleSaveClickEvent(action)
                    } else { // if not saved, add to saved stories
                        view.setImageResource(com.wapo.view.R.drawable.ic_bookmark_saved)
                        ellipsisMenuViewModel.saveArticle(ellipsisActionItem)
                        Measurement.trackSaveUnsave(
                            Measurement.PAGE_RECIPE_FINDER_LANDING,
                            ellipsisActionItem.arcId,
                            ellipsisActionItem.contentType,
                            "",
                            true,
                            true,
                            false,
                            ellipsisActionItem.url,
                        )
                    }
                } else {
                    if (isCurrentlySaved) {
                        view.setImageResource(com.wapo.view.R.drawable.ic_bookmark_saved)
                    } else {
                        view.setImageResource(com.wapo.view.R.drawable.ic_bookmark_unsaved)
                    }
                }
            }
        }
    }

    /**
     * When user is clicking the Ellipsis icon from the Current playing view from playlist Adapter
     */
    protected fun observeAudioPlayerEllipsisClick() {
        audioMediaActivityViewModel.audioPlayerEllipsisClickEvent.observe(
            this,
            Observer { article ->
                ellipsisHelperViewModel.handleEllipsisClick(
                    article.toPlaylistAudio()!!.toEllipsisActionItem(
                        menuType = EllipsisMenu.AudioPlayerEllipsisButton,
                    ),
                )
            },
        )
    }

    /**
     * When user is clicking on the Remove from playlist from the ellipsis button of current playing view updating the Database to delete the audio record
     */
    protected fun observeRemoveFromPlayListEvents() {
        audioMediaActivityViewModel.removePlayListEvent.observe(this) {
            playlistActivityViewModel.removePlaylistByIdAudio(it)
        }
    }

    /**
     * When user is clicking the Ellipsis icon from the Now playing view from playlist Adapter
     */
    protected fun observeAudioCurrentPlayingEllipsisClick() {
        audioMediaActivityViewModel.audioPlayerCurrentPlayingEllipsisEvent.observe(this) { article ->
            ellipsisHelperViewModel.handleEllipsisClick(
                article
                    .toPlaylistAudio()!!
                    .toEllipsisActionItem(menuType = EllipsisMenu.AudioPlayerEllipsisButton),
            )
        }
    }

    protected fun observePlaylistClickTrackEvent() {
        playlistActivityViewModel.playlistClickTrackEvent.observe(this) {
            Measurement.trackPlayListButtonClick(getAppSection())
        }
    }

    open fun getCurrentSectionDisplayName(): String? {
        return "Food" // TODO: Add proper logic for getting section
    }

    fun isNightMode(): Boolean = nightModeManager.immediateNightModeStatus

    /**
     * GridActivity Interface Implementation
     */
    override fun getGridEnvironment(): GridEnvironment = fusionEnvironment

    /**
     * ImageLoaderProvider Implementation
     */
    override fun getImageLoader(): AnimatedImageLoader =
        FlagshipApplication.getInstance().animatedImageLoader

    /**
     * ConnectivityActivity Implementation
     */

    /**
     * SectionActivity Implementation
     */

    override fun isPhone(): Boolean = UIUtil.isPhone(this)

    override fun getLiveBlogSvcUrl(): String = ConfigManager.getInstance().config.liveBlogServiceURL

    override fun openBreakingNews(url: String?) {
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(url)
                .setTabName(activeTabName)
                .breakingNewsOriginated(true)
                .setNavigationBehavior(Measurement.BREAKING_NEWS_BANNER)
                .setOmniturePathToView(Measurement.PATH_TO_VIEW_PUSH_BREAKING)
                .buildIntent(this)
        startActivity(intent)
    }

    // TODO: Override in Child Class
    open val activeTabName: String
        get() = ""

    override fun openLiveVideo(url: String?) {
        val intent = Intent(this, SearchableArticlesActivity::class.java)
        val url2 = url + "?" + Measurement.TRACKING_ID
        intent.putExtra(ARTICLES_URL_PARAM, url2)
        intent.putExtra(LIVE_VIDEO_ORIGINATED, true)
        val activeTabName = activeTabName
        intent.putExtra(CURRENT_TAB_NAME, activeTabName)
        intent.putExtra(CURRENT_APP_SECTION, activeTabName)
        startActivity(intent)
        Measurement.trackExternalLink(url2)
    }

    override fun openLiveBlog(url: String?) {
        Utils.startWeb(url, this)
    }

    override fun openWeb(url: String?) {
        Utils.startWeb(url, this)
    }

    override fun openWebEmbed(url: String) {
        Utils.startWeb(url, this)
    }

    override fun logExtras(str: String) {
        CrashWrapper.logExtras(str)
    }

    override fun sendException(t: Throwable) {
        CrashWrapper.sendException(t)
    }

    override fun onRefreshSFPage(pageName: String?) {
        FlagshipApplication.getInstance().releaseVideoManager()
        FlagshipApplication.getInstance().releaseVideoManager2()
        // Check back end health if page is top stories
        if (isTopStoriesSection(pageName)) {
            sectionsHabitTilesViewModel.fetchData(true)
            checkBackendHealth()
        }
    }

    override fun onStartSFFragment(pageName: String?) {
        // Check back end health if page is top stories
        if (isTopStoriesSection(pageName)) {
            sectionsHabitTilesViewModel.fetchData(true)
            checkBackendHealth()
        }
    }

    override fun getPager(): SectionsPagerView? = null

    override fun getSectionFragmentFactory(): SectionFragmentFactory =
        DefaultSectionFragmentFactory(
            getContentManagerObs().toBlocking().first().wapoConfigManager,
        )

    override fun getAudioView(): AudioView? = AudioViewImpl(this)

    override fun getInlineAudioView(): InlineAudioView? = InlineAudioViewImpl(this)

    // TODO: Override accordingly in child class
    override fun getSubscribeButton(sectionDisplayName: String?): SubscribeButton? = null

    fun playCarouselAudio(
        audioMediaConfigList: AudioMediaConfigList,
        position: Int,
        currentPositionPageName: String?,
        currentPositionArcId: String?,
    ) {
        Measurement.setNavigationBehavior(NavigationBehavior.AUDIO_CAROUSEL, position)
        if (audioPlaybackViewModel.hasAccessToAudioArticle()) {
            val audioMediaConfig = audioMediaConfigList.list.getOrNull(position)
            val isAlreadyPlaying = audioMediaActivityViewModel.nowPlayingAudioItem.value?.audioMediaConfig?.mediaId == audioMediaConfig?.mediaId
            audioMediaActivityViewModel.playMedia(audioMediaConfigList, position)
            val audioType = audioMediaConfig?.audioType
            if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(audioType) && !isAlreadyPlaying) {
                showAudioPlayer()
            }
        } else {
            // show regwall
            Measurement.setPaywallArticle(currentPositionPageName, currentPositionArcId)
            sectionWallHelperViewModel.dispatchShowRegwall(
                PaywallConstants.WALL_NAME_AUDIO_CAROUSEL,
                PaywallConstants.WallType.AUDIO_CAROUSEL_PAYWALL,
            )
        }
    }

    fun playAudioArticle(
        audioMediaConfig: AudioMediaConfig,
        currentPositionArcId: String?,
    ) {
        sectionNavViewModel.showBottomSheetPrompt(null)
        if (audioPlaybackViewModel.hasAccessToAudioArticle()) {
            val isAlreadyPlaying = audioMediaActivityViewModel.nowPlayingAudioItem.value?.audioMediaConfig?.mediaId == audioMediaConfig.mediaId
            audioMediaActivityViewModel.playMedia(audioMediaConfig)
            if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(audioMediaConfig.audioType) && !isAlreadyPlaying) {
                showAudioPlayer()
            }
        } else {
            // show regwall
            Measurement.setPaywallArticle(this.pageNameFromTracking, currentPositionArcId)
            sectionWallHelperViewModel.dispatchShowRegwall(
                PaywallConstants.WALL_NAME_AUDIO_ACTION_BUTTON,
                PaywallConstants.WallType.AUDIO_ACTION_BUTTON_PAYWALL,
            )
        }
    }

    private fun showAudioPlayer() {
        sectionNavViewModel.showBottomSheetPrompt(null)
        AudioPagerFragment.newInstance().show(
            supportFragmentManager,
            AudioPagerFragment.FRAGMENT_TAG
        )
    }

    override fun canAutoPlayCarouselVideo(): Boolean = AppPreferences.isAutoplayVideosOn()

    override fun canAutoPlayInlineVideo(): Boolean = AppPreferences.isAutoplayVideosOn()

    override fun hasDeviceLevelDataRestriction(): Boolean =
        ConnectivityMonitor.getInstance(applicationContext).hasDeviceLevelDataRestriction()

    override fun isDataUsageRestricted(): Boolean =
        ConnectivityMonitor.getInstance(applicationContext).isDataUsageRestricted()

    override fun isLowDataModeEnable(): Boolean = AppPreferences.isLowDataModeEnabled()

    override fun isPremiumAccount(): Boolean =
        PaywallService.getInstance().wapoAccessServiceInstance.hasPremiumAccess

    override fun getSectionInlineMessage(): SectionInlineMessage? {
        return iterableActivityViewModel.getSectionInlineMessageFromMessages(this)
    }

    override fun shouldSuppressAds(): Boolean = FlagshipApplication.getInstance().shouldSuppressAds()

    override fun getSectionAudioMediaActivityViewModel(): SectionAudioMediaActivityViewModel? =
        sectionAudioMediaActivityViewModel

    override fun getCarouselAudioMediaActivityViewModel(): CarouselAudioMediaActivityViewModel? =
        carouselAudioMediaActivityViewModel

    override fun getPlayListViewModel(): PlaylistActivityViewModel? = null

    override fun isAudioPlayerSheetVisible(): Boolean =
        supportFragmentManager.findFragmentByTag(AudioPagerFragment.FRAGMENT_TAG)?.isAdded
            ?: false

    override fun onAllReadingListArticlesDeleted() {}

    override fun getSaveProvider(): SaveProvider =
        FlagshipApplication.getInstance().savedArticleManager.getSaveProvider()

    open fun isCurrentSection(menuSection: MenuSection?): Boolean = false

    override fun openSectionByUrl(
        url: String,
        defaultToWeb: Boolean,
    ): Boolean = false

    override fun getAdViewFactory(): AdViewFactory = adViewFactory

    override fun getPlayer2ContainerResId(): Int = R.layout.carousel_video_player_view

    override fun hasAudioPlayerSupportInThisScreen(): Boolean = true

    override fun isActivityFinishing(): Boolean = super.isFinishing()

    override fun openCustomNavSettings() {
        val intent = Intent(this, CustomNavActivity::class.java)
        startActivity(intent)
    }


    open fun getHabitTiles(): List<Tile> = emptyList()

    /**
     * Verify if an article is saved from grid
     */
    override fun isArticleSaved(url: String): LiveData<Boolean> {
        val article =
            FlagshipApplication.getInstance().savedArticleManager.getLiveArticleByUrl(
                url
            )
        return article.map { it != null }
    }

    open fun showPaywallDialogByName(
        paywallReason: Int,
        wallType: WallType,
        wallName: String,
    ) = Unit

    open fun showPaywallDialog(
        paywallReason: Int,
        paywallType: WallType?,
    ) = Unit
}
