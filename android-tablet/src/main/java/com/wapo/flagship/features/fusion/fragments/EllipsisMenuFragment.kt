package com.wapo.flagship.features.fusion.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.playlist.toPlaylistAudio
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel
import com.wapo.flagship.features.fusion.event.EllipsisMenuAction
import com.wapo.flagship.features.fusion.viewmodel.EllipsisMenuViewModel
import com.wapo.flagship.features.gifting.tracking.GiftTrackingDetails
import com.wapo.flagship.features.gifting.viewmodels.GiftCollaborationViewModel
import com.wapo.flagship.features.gifting.views.GiftArticleSenderFragment
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wapo.view.RippleHelper
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.FragmentEllipsisMenuBinding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Bottomsheet menu that gives options to
 * - Read Article
 * - Saved Article
 * - Unsave Article
 * - Share Article
 */
@AndroidEntryPoint
class EllipsisMenuFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private lateinit var binding: FragmentEllipsisMenuBinding

    /**
     * Menu view model that handles logic for various menu actions
     */
    private val ellipsisMenuViewModel: EllipsisMenuViewModel by viewModels()

    /**
     * Helper view model tied to article data that is transferred from ellipsis clicked on card
     */
    private val ellipsisHelperViewModel: EllipsisHelperViewModel by activityViewModels()

    private val giftCollaborationViewModel: GiftCollaborationViewModel by activityViewModels()

    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()

    private val playlistActivityViewModel: PlaylistActivityViewModel by viewModels()

    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel by activityViewModels()

    private val navBarViewModel: NavBarViewModel by activityViewModels()

    private val sectionNavViewModel: SectionNavViewModel by activityViewModels()


    private val isLoggedInUser
        get() =
            PaywallService.getInstance()?.isWpUserLoggedIn == true

    private val isSaveEnabled
        get() = ConfigManager.getInstance().config.actionButtonsConfig.saveEnabled

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentEllipsisMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            // cases where activity is being recreated and view model also lost its state.
            dismiss()
            return
        }

        // Register all button click listeners
        binding.buttonAddToSavedStories.setOnClickListener(this)
        binding.buttonReadArticle.setOnClickListener(this)
        binding.buttonRemoveFromSavedStories.setOnClickListener(this)
        binding.buttonShare.setOnClickListener(this)
        binding.buttonGift.setOnClickListener(this)
        binding.buttonConfirm.setOnClickListener(this)
        binding.buttonCancel.setOnClickListener(this)
        binding.buttonAddToPlaylist.setOnClickListener(this)
        binding.buttonRemoveFromPlaylist.setOnClickListener(this)
        binding.buttonListen.setOnClickListener(this)

        // Show menu screen or confirmation screen
        if (showConfirmationOnly()) {
            showConfirmationDialog()
            return
        } else {
            binding.selectionList.setVisible(true)
            binding.confirmationView.setVisible(false)
        }

        if (getMenuType() == EllipsisMenu.Carousel) {
            // Enable Read and Share
            // Disable Add, Remove and Gift
            binding.buttonReadArticle.setVisible(!isPodcast())
            binding.buttonShare.setVisible(!isPodcast())
            binding.buttonAddToSavedStories.setVisible(false)
            binding.buttonRemoveFromSavedStories.setVisible(false)
            binding.buttonGift.setVisible(false)
            binding.buttonRemoveFromPlaylist.setVisible(false)
            binding.buttonListen.setVisible(false)
            // remove bottom divider for the last item
        } else if (getMenuType() == EllipsisMenu.ActionButton) {
            // Default is EllipsisMenu.Flex
            // Enable Share and Gift`
            // Disable Read and Save
            if (getIsAudioArticle()) {
                binding.buttonAddToPlaylist.setVisible(true)
            } else {
                binding.buttonAddToPlaylist.setVisible(false)
            }
            binding.buttonReadArticle.setVisible(false)
            binding.buttonShare.setVisible(true)
            binding.buttonGift.setVisible(true)
            binding.buttonRemoveFromPlaylist.setVisible(false)
            binding.buttonAddToSavedStories.setVisible(false)
            binding.buttonRemoveFromSavedStories.setVisible(false)
            binding.buttonListen.setVisible(false)

            // Set current article for menu
            ellipsisHelperViewModel.ellipsisClickEvent.value?.let {
                ellipsisMenuViewModel.setCurrentPageUrl(it.url)
            }
        } else if (getMenuType() == EllipsisMenu.EllipsisButton) {
            if (getIsAudioArticle()) {
                binding.buttonAddToPlaylist.setVisible(true)
                binding.buttonListen.setVisible(true)
            } else {
                binding.buttonAddToPlaylist.setVisible(false)
                binding.buttonListen.setVisible(false)
            }
            binding.buttonReadArticle.setVisible(false)
            binding.buttonShare.setVisible(false)
            binding.buttonGift.setVisible(true)
            binding.buttonRemoveFromPlaylist.setVisible(false)
            // Update visibility of save/unsave action based on article meta
            ellipsisMenuViewModel.liveArticleByUrl.observe(viewLifecycleOwner) {
                updateBookmarkItems(it != null)
                removeLastDivider()
            }
            // Set current article for menu
            ellipsisHelperViewModel.ellipsisClickEvent.value?.let {
                ellipsisMenuViewModel.setCurrentPageUrl(it.url)
            }
        } else if (getMenuType() == EllipsisMenu.AudioPlayerEllipsisButton) {
            binding.buttonRemoveFromPlaylist.setVisible(true)
            if (isPodcast()) {
                binding.buttonReadArticle.setVisible(false)
                binding.buttonShare.setVisible(false)
                binding.buttonGift.setVisible(false)
            } else {
                binding.buttonReadArticle.setVisible(true)
                binding.buttonShare.setVisible(true)
                binding.buttonGift.setVisible(true)
            }
            binding.buttonListen.setVisible(false)
            binding.buttonAddToSavedStories.setVisible(false)
            binding.buttonRemoveFromSavedStories.setVisible(false)
            binding.buttonAddToPlaylist.setVisible(false)
        } else if (getMenuType() == EllipsisMenu.CurrentPlayingViewEllipsisButton) {
            // Enable Read and Share
            // Disable Add, Remove and Gift
            binding.buttonReadArticle.setVisible(!isPodcast())
            binding.buttonShare.setVisible(!isPodcast())
            binding.buttonAddToSavedStories.setVisible(false)
            binding.buttonRemoveFromSavedStories.setVisible(false)
            binding.buttonGift.setVisible(false)
            binding.buttonRemoveFromPlaylist.setVisible(false)
            // remove bottom divider for the last item
        } else if (getMenuType() == EllipsisMenu.VideoActionButton) {
            binding.buttonReadArticle.setVisible(false)
            binding.buttonShare.setVisible(true)
            binding.buttonAddToSavedStories.setVisible(false)
            binding.buttonRemoveFromSavedStories.setVisible(false)
            binding.buttonGift.setVisible(false)
            binding.buttonRemoveFromPlaylist.setVisible(false)
            binding.buttonAddToPlaylist.setVisible(false)
            binding.buttonListen.setVisible(false)
        } else if (getMenuType() == EllipsisMenu.ForYouEllipsisButton) {
            // Enable Share, Save, Gift, Add to Playlist
            if (getIsAudioArticle()) {
                binding.buttonAddToPlaylist.setVisible(true)
            } else {
                binding.buttonAddToPlaylist.setVisible(false)
            }
            binding.buttonReadArticle.setVisible(false)
            binding.buttonShare.setVisible(true)
            binding.buttonGift.setVisible(true)
            binding.buttonRemoveFromPlaylist.setVisible(false)
            binding.buttonAddToSavedStories.setVisible(true)
            binding.buttonListen.setVisible(false)

            ellipsisMenuViewModel.liveArticleByUrl.observe(viewLifecycleOwner) {
                updateBookmarkItems(it != null)
                removeLastDivider()
            }
            // Set current article for menu
            ellipsisHelperViewModel.ellipsisClickEvent.value?.let {
                ellipsisMenuViewModel.setCurrentPageUrl(it.url)
            }
        }
        lifecycleScope.launch {
            try {
                updatePlayListStateFromDatabase()
            } finally {
                removeLastDivider()
            }
        }

        RippleHelper.addRippleEffectToView(binding.buttonAddToSavedStories)
        RippleHelper.addRippleEffectToView(binding.buttonReadArticle)
        RippleHelper.addRippleEffectToView(binding.buttonRemoveFromSavedStories)
        RippleHelper.addRippleEffectToView(binding.buttonShare)
        RippleHelper.addRippleEffectToView(binding.buttonGift)

        observeMenuClick()
    }

    /**
     * Updating the Ellipsis state based on the audio exist in the DB and updating the UI
     */
    private suspend fun updatePlayListStateFromDatabase() {
        val isCurrentlyPlaying =
            audioMediaActivityViewModel.nowPlayingAudioItem.value?.audioMediaConfig?.let {
                audioMediaActivityViewModel.isMediaActive(it)
            } ?: false
        val isInPlayList =
            getIsAudioArticle() &&
                playlistActivityViewModel.getPlaylistFromDatabase(
                    getAudioID(),
                )
        val isPersonalizedPodcast = PersonalizedPodcastHelper.isPersonalizedPodcastItem(audioMediaActivityViewModel.nowPlayingAudioItem.value?.audioMediaConfig?.audioType)
        when {
            !getIsAudioArticle() -> {
                binding.buttonRemoveFromPlaylist.setVisible(false)
                binding.buttonAddToPlaylist.setVisible(false)
            }
            isPersonalizedPodcast -> {
                binding.buttonRemoveFromPlaylist.setVisible(false)
                binding.buttonAddToPlaylist.setVisible(false)
            }
            isInPlayList -> {
                if (getMenuType() == EllipsisMenu.CurrentPlayingViewEllipsisButton) {
                    binding.buttonRemoveFromPlaylist.setVisible(false)
                } else {
                    binding.buttonRemoveFromPlaylist.setVisible(true)
                }
                binding.buttonAddToPlaylist.setVisible(false)
            }
            else -> {
                binding.buttonAddToPlaylist.setVisible(true)
                binding.buttonRemoveFromPlaylist.setVisible(false)
            }
        }
    }

    /**
     * Handle click response for save, remove, read, share, confirm and cancel
     */
    override fun onClick(view: View?) {
        val actionItem = ellipsisHelperViewModel.ellipsisClickEvent.value ?: return
        when (view?.id) {
            R.id.button_add_to_saved_stories -> {
                ellipsisMenuViewModel.handleSave(actionItem)
                dismiss()
            }
            R.id.button_remove_from_saved_stories -> {
                ellipsisMenuViewModel.handleRemove(actionItem)
            }
            R.id.button_read_article -> {
                ellipsisMenuViewModel.handleRead(actionItem)
                ellipsisHelperViewModel.setBackToFront()
                dismiss()
            }
            R.id.button_share -> {
                ellipsisMenuViewModel.handleShare(actionItem)
                dismiss()
            }
            R.id.button_gift -> {
                ellipsisMenuViewModel.handleGift(actionItem)
                dismiss()
            }
            R.id.button_confirm -> { // confirmation dialog for removing a saved story
                val isActionButton = actionItem.menuType == EllipsisMenu.RemoveActionButton
                Measurement.trackSaveUnsave(
                    actionItem.pageName,
                    actionItem.arcId,
                    actionItem.contentType,
                    "",
                    isActionButton,
                    false,
                    false,
                    actionItem.url,
                )
                ellipsisMenuViewModel.removeSavedArticle(actionItem.url)
                dismiss()
            }
            R.id.button_cancel -> {
                dismiss()
            }
            R.id.button_add_to_playlist -> {
                dismiss()
                ellipsisMenuViewModel.handleAddToPlaylist(actionItem)
            }
            R.id.button_remove_from_playlist ->
                {
                    dismiss()
                    ellipsisMenuViewModel.handleRemoveFromPlaylist(actionItem)
                    Toast.makeText(requireActivity(), "Removed from User Playlist", Toast.LENGTH_LONG).show()
                    Measurement.trackActionButtonRemoveFromPlaylist(getAppSection())
                }
            R.id.button_listen -> {
                playAudio()
                dismiss()
            }
        }
    }

    private fun playAudio() {
        val audioMediaConfig = constructAudioMediaConfig()
        if (audioMediaConfig != null) {
            articlesPagerCollaborationViewModel.dispatchAudioClickEvent(audioMediaConfig)
        }
    }

    private fun constructAudioMediaConfig(): AudioMediaConfig? {
        val contentState = articlesPagerCollaborationViewModel.articleContentState.value
        return if (contentState is ArticleContentState.Success) {
            val article = contentState.article
            val audio = article.audio ?: return null
            articlesPagerCollaborationViewModel.assembleConfigWithChildren(
                audio,
                article,
                requireContext(),
                false
            )
        } else {
            null
        }
    }

    /**
     * Handle various actions for menu items being clicked
     */
    private fun observeMenuClick() {
        ellipsisMenuViewModel.choiceClickEvent.observe(viewLifecycleOwner) {
            when (it) {
                is EllipsisMenuAction.ActionRead -> {
                    ellipsisHelperViewModel.handleActionRead()
                    val list = it.articleItem.articleList ?: listOf()
                    val url = it.articleItem.url

                    val intent =
                        if (list.isNotEmpty()) {
                            ArticlesParcel.builder().setArticleUrls(list, list.indexOf(url))
                        } else {
                            ArticlesParcel.builder().setArticleSingleUrl(url)
                        }.setTabName(BottomTab.Listen.title)
                            .setSectionDisplayName(LISTEN_PAGE_NAME)
                            .setItId(
                                Measurement.NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_OPEN + (list.indexOf(url) + 1),
                            ).setArticleOpenedFromSectionFront(true)
                            .setShouldNotSuppressPageView(navBarViewModel.isCurrentTab(BottomTab.Listen))
                            .buildIntent(requireContext())
                    val launchedFromAudioPlayerEllipsis =
                        it.articleItem.menuType == EllipsisMenu.AudioPlayerEllipsisButton
                    intent.putExtra(EXTRA_FROM_AUDIO_READ_ARTICLE, launchedFromAudioPlayerEllipsis)
                    requireContext().startActivity(intent)
                    dismiss()

                    if (!it.articleItem.articleLinkIsWebType) {
                        Measurement.setAudioCarouselPageViewValues(
                            list.indexOf(url) + 1,
                            it.articleItem.pageName,
                        )
                    }
                }
                is EllipsisMenuAction.ActionRemove -> {
                    // don't track here; wait for confirmation dialog
                    showConfirmationDialog()
                    ellipsisHelperViewModel.handleActionRemove()
                }
                is EllipsisMenuAction.ActionSave -> {
                    if (isLoggedInUser) {
                        it.articleItem.let { actionItem ->
                            val isActionButton = actionItem.menuType == EllipsisMenu.ActionButton
                            Measurement.trackSaveUnsave(
                                actionItem.pageName,
                                actionItem.arcId,
                                actionItem.contentType,
                                "",
                                isActionButton,
                                true,
                                false,
                                actionItem.url,
                            )
                        }
                        ellipsisMenuViewModel.saveArticle(it.articleItem)
                        ellipsisHelperViewModel.handleActionSave()
                    } else {
                        // if user is neither logged in nor subscribed, show Save Regwall
                        (activity as? BaseActivity)?.getPaywallSheetHelper()?.showWall(
                            wallName = PaywallConstants.WALL_NAME_SAVE_REGWALL,
                            wallType = PaywallConstants.WallType.SAVE_REGWALL,
                            wallReason = PaywallConstants.WallType.SAVE_REGWALL.ordinal,
                        )
                    }
                }
                is EllipsisMenuAction.ActionShare -> {
                    performShare(it.articleItem)
                    ellipsisHelperViewModel.handleActionShare()
                }
                is EllipsisMenuAction.ActionGift -> {
                    performGift(it.articleItem)
                    ellipsisHelperViewModel.handleActionGift()
                }
                is EllipsisMenuAction.ActionAddToPlayList -> {
                    performAddToPlayList(it.articleItem)
                    ellipsisHelperViewModel.handleActionAddToPlayList()
                }
                is EllipsisMenuAction.ActionRemoveFromPlaylist -> {
                    performRemoveFromPlaylist(it.articleItem)
                    ellipsisHelperViewModel.handleActionRemoveFromPlaylist()
                }
            }
        }
    }

    private fun performAddToPlayList(articleItem: EllipsisActionItem) {
        // if playlist is available, use playlist to add audio
        articleItem.playlist?.let {
            audioMediaActivityViewModel.dispatchAddPlayListPodcastEvent(it)
            return
        }

        // if audioMediaConfig is available, convert to playlist then use to add audio
        articleItem.audioMediaConfig?.toPlaylistAudio()?.let { playlistAudio ->
            audioMediaActivityViewModel.dispatchAddPlayListPodcastEvent(playlistAudio)
            return
        }

        // else fetch audio
        audioMediaActivityViewModel.dispatchAddPlayListArticleEvent(articleItem.url)
    }

    private fun performRemoveFromPlaylist(articleItem: EllipsisActionItem) {
        audioMediaActivityViewModel.dispatchRemovePlayListArticleEvent(articleItem.url)
    }

    private fun getAudioID(): String = arguments?.getString(AUDIO_ID) ?: ""

    /**
     * Show confirmation dialog for removing an article from saved list. Prevent
     * accidental removal
     */
    private fun showConfirmationDialog() {
        binding.selectionList.setVisible(false)
        binding.confirmationView.setVisible(true)
    }

    /**
     * Perform share action for article.
     */
    private fun performShare(articleItem: EllipsisActionItem) {
        articleItem
            .run {
                Share
                    .Builder()
                    .headline(headline)
                    .byline(byline)
                    .shareUrl(url)
                    .build()
                    .shareItem(requireActivity())
            }
    }

    private fun performGift(articleItem: EllipsisActionItem) {
        giftCollaborationViewModel.let {
            it.giftArticleUrl = articleItem.url
            it.isActionButton = articleItem.menuType == EllipsisMenu.ActionButton
            it.appSection = getAppSection()
        }
        activity?.supportFragmentManager?.let {
            val giftFragment = GiftArticleSenderFragment()
            sectionNavViewModel.showBottomSheetPrompt(null)
            giftFragment.show(it, "GiftBottomSheet")
        }
        giftCollaborationViewModel.dispatchGiftTrackingEvent(GiftTrackingDetails.GIFT_CLICK)
    }

    /**
     * Update save options based on user state.
     */
    private fun updateBookmarkItems(isBookmarked: Boolean) {
        val canDisableSave =
            getMenuType() == EllipsisMenu.Carousel ||
                getMenuType() == EllipsisMenu.ActionButton ||
                getMenuType() == EllipsisMenu.EllipsisButton
        if (!isSaveEnabled && canDisableSave) {
            binding.buttonAddToSavedStories.visibility = View.GONE
            binding.buttonRemoveFromSavedStories.visibility = View.GONE
        } else {
            if (isBookmarked) {
                binding.buttonAddToSavedStories.visibility = View.GONE
                binding.buttonRemoveFromSavedStories.visibility = View.VISIBLE
            } else {
                binding.buttonRemoveFromSavedStories.visibility = View.GONE
                binding.buttonAddToSavedStories.visibility = View.VISIBLE
            }
        }
    }

    private fun removeLastDivider() {
        when {
            binding.buttonRemoveFromPlaylist.isVisible -> binding.buttonRemoveFromPlaylist.background = null
            binding.buttonRemoveFromSavedStories.isVisible -> binding.buttonRemoveFromSavedStories.background = null
            binding.buttonReadArticle.isVisible -> binding.buttonReadArticle.background = null
            binding.buttonAddToPlaylist.isVisible -> binding.buttonAddToPlaylist.background = null
            binding.buttonShare.isVisible -> binding.buttonShare.background = null
            binding.buttonAddToSavedStories.isVisible -> binding.buttonAddToSavedStories.background = null
            binding.buttonGift.isVisible -> binding.buttonGift.background = null
        }
    }

    private fun getMenuType(): EllipsisMenu = arguments?.getParcelable(ARG_MENU_TYPE) ?: EllipsisMenu.ActionButton

    private fun getAppSection(): String? = arguments?.getString(ARG_APP_SECTION)

    private fun getIsAudioArticle(): Boolean = arguments?.getBoolean(IS_AUDIO_ARTICLE) ?: false

    private fun isPodcast(): Boolean = arguments?.getBoolean(IS_PODCAST) ?: false

    private fun showConfirmationOnly(): Boolean = arguments?.getBoolean(ARG_SHOW_CONFIRMATION_ONLY) ?: false

    companion object {
        val tag: String = EllipsisMenuFragment::class.java.simpleName
        private const val LISTEN_PAGE_NAME = "Listen to the Post"
        private const val ARG_MENU_TYPE = "ARG_MENU_TYPE"
        private const val ARG_APP_SECTION = "ARG_APP_SECTION"
        private const val ARG_SHOW_CONFIRMATION_ONLY = "ARG_SHOW_CONFIRMATION_ONLY"
        private const val IS_AUDIO_ARTICLE = "IS_AUDIO_SECTION"
        private const val IS_PODCAST = "IS_PODCAST"
        private const val AUDIO_ID = "AUDIO_ID"
        const val EXTRA_FROM_AUDIO_READ_ARTICLE = "EXTRA_FROM_AUDIO_READ_ARTICLE"

        fun create(
            menuType: EllipsisMenu,
            appSection: String?,
            isAudioItem: Boolean,
            audioId: String,
            isPodcast: Boolean = false,
            showConfirmationOnly: Boolean = false,
        ): EllipsisMenuFragment =
            EllipsisMenuFragment().also {
                (it.arguments ?: Bundle()).let { bundle ->
                    bundle.putParcelable(ARG_MENU_TYPE, menuType)
                    bundle.putString(ARG_APP_SECTION, appSection)
                    bundle.putBoolean(IS_AUDIO_ARTICLE, isAudioItem)
                    bundle.putBoolean(IS_PODCAST, isPodcast)
                    bundle.putBoolean(ARG_SHOW_CONFIRMATION_ONLY, showConfirmationOnly)
                    bundle.putString(AUDIO_ID, audioId)
                    it.arguments = bundle
                }
            }
    }
}
