package com.wapo.flagship.features.mypost.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wapo.flagship.features.mypost.adapters.SectionsPreviewRecyclerViewAdapter
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowCollaborationViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.save.databinding.FragmentMyPostSectionsPreviewBinding
import com.washingtonpost.android.save.decorators.SectionsItemDecoration
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.types.MyPostSection
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SectionsPreviewFragment : Fragment() {
    private lateinit var binding: FragmentMyPostSectionsPreviewBinding

    private val myPost2ViewModel: MyPost2ViewModel by activityViewModels()

    private val forYouActivityViewModel: ForYouActivityViewModel by activityViewModels()

    private val topicFollowCollaborationViewModel: TopicFollowCollaborationViewModel by activityViewModels()

    private val sectionNavViewModel: SectionNavViewModel by activityViewModels()

    private val globalBannerViewModel: GlobalBannerViewModel by activityViewModels()

    private val allSections =
        listOf(
            MyPostSection.TOPICS,
            MyPostSection.SAVED_STORIES,
            MyPostSection.FOLLOWING,
            MyPostSection.READING_HISTORY,
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyPostSectionsPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        myPost2ViewModel.readingArticleItemsList.observe(viewLifecycleOwner) {
            myPost2ViewModel.updateSections()
        }
        myPost2ViewModel.followingAuthors.observe(viewLifecycleOwner) {
            if (it != null) {
                myPost2ViewModel.updateSections()
            }
        }
        myPost2ViewModel.readingHistoryArticleItemsList.observe(viewLifecycleOwner) {
            myPost2ViewModel.updateSections()
        }
        myPost2ViewModel.purchasedArticles.observe(viewLifecycleOwner) {
            myPost2ViewModel.updateSections()
        }
        initRecyclerView()
        binding.swipeRefreshLayout.setOnRefreshListener {
            myPost2ViewModel.refresh()
            binding.rvMyPostSections.adapter?.notifyDataSetChanged()
            binding.swipeRefreshLayout.isRefreshing = false
        }
        updateSignInButton()
        binding.signIn.text = resources.getText(R.string.sign_in_or_create_account_underlined)
        binding.signIn.setOnClickListener {
            myPost2ViewModel.handleSignInClickEvent()
        }
        myPost2ViewModel.allSections.observe(viewLifecycleOwner) {
            myPost2ViewModel.onPreviewSectionUpdate()
        }

        topicFollowCollaborationViewModel.reloadTopicState.observe(viewLifecycleOwner) { changedTopicId ->
            myPost2ViewModel.onPreviewSectionUpdate()
        }
    }

    /**
     * Tells [BannerPreviewItemViewHolder] about screen rotation so it can re-render CTA.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.rvMyPostSections.adapter.let {
            if (it is SectionsPreviewRecyclerViewAdapter && it.currentList.first() is PreviewItem.BannerPreviewItem) {
                it.notifyItemChanged(0)
            }
        }
    }

    private fun initRecyclerView() {
        binding.rvMyPostSections.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter =
                SectionsPreviewRecyclerViewAdapter(
                    {
                        myPost2ViewModel.handleArticleItemClickEvent(it)
                    },
                    {
                        myPost2ViewModel.handleSaveClickEvent(it)
                    },
                    {
                        myPost2ViewModel.handleOptionsClickEvent(it)
                    },
                    {
                        myPost2ViewModel.handleViewMoreClickEvent(it)
                    },
                    {
                        myPost2ViewModel.handleMoreFromAuthorClickEvent(it)
                    },
                    {
                        myPost2ViewModel.handleSignInClickEvent()
                    },
                    {
                        myPost2ViewModel.handleSettingsClickEvent()
                    },
                    {
                        myPost2ViewModel.handleOpenSectionClickEvent(it)
                    },
                    {
                        myPost2ViewModel.handleViewArchiveClickEvent()
                    },
                    {
                        myPost2ViewModel.handleUpdateConsentSettingsClickEvent()
                    },
                    {
                        Measurement.setNavigationBehavior(
                            Measurement.getDefaultMap(),
                            Measurement.NAVIGATION_BEHAVIOR_MY_POST_TOPIC_FOLLOW,
                        )
                        sectionNavViewModel.openSectionByUrl(it.destination)
                    },
                    {
                        myPost2ViewModel.handleFollowButtonClickEvent(it.topicId)
                    },
                    {
                        myPost2ViewModel.handleBirthdayFrontPageClickEvent()
                    },
                    {
                        globalBannerViewModel.setBannerEvent(it)
                    }
                ).apply {
                    observeSectionSelection()
                    observeSignInAttemptEvent()
                    observeTargetingConsentChangeEvent()
                    observePreviewSectionUpdate(this)
                    observeAllLists()
                    onAuthorDataRequested = {
                        myPost2ViewModel.onAuthorDataRequested(it)
                    }
                }
            val dimen =
                if (myPost2ViewModel.shouldDisplayBanner(MyPostSection.ALL.name)) com.washingtonpost.android.save.R.dimen.my_post_card_divider_height_half else com.washingtonpost.android.save.R.dimen.my_post_card_divider_height
            addItemDecoration(SectionsItemDecoration(topMargin = dimen))
            setHasFixedSize(true)
            itemAnimator = null
            setItemsAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSignInButton()
        binding.swipeRefreshLayout.isEnabled = myPost2ViewModel.canSwipeToRefresh()
    }

    private fun updateSignInButton() {
        binding.signIn.visibility =
            if (PaywallService.getInstance().isWpUserLoggedIn) View.GONE else View.VISIBLE
    }

    private fun setItemsAnimation() {
        binding.rvMyPostSections.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(
                requireContext(),
                com.washingtonpost.android.save.R.anim.slide_up_400_layout_animation,
            )
    }

    private fun observeSectionSelection() {
        myPost2ViewModel.section.observe(
            viewLifecycleOwner,
            Observer {
                if (it == MyPostSection.ALL) {
                    myPost2ViewModel.onPreviewSectionUpdate()
                }
            },
        )
    }

    private fun observeSignInAttemptEvent() {
        myPost2ViewModel.signInAttemptEvent.observe(viewLifecycleOwner) {
            myPost2ViewModel.onPreviewSectionUpdate()
        }
    }

    private fun observeTargetingConsentChangeEvent() {
        myPost2ViewModel.targetingEnabled.observe(viewLifecycleOwner) {
            myPost2ViewModel.onPreviewSectionUpdate()
        }
    }

    private fun observePreviewSectionUpdate(adapter: SectionsPreviewRecyclerViewAdapter) {
        myPost2ViewModel.previewSections.observe(viewLifecycleOwner) { items ->
            submitList(adapter, items)
        }
    }

    private fun submitList(
        adapter: SectionsPreviewRecyclerViewAdapter,
        list: List<PreviewItem>,
    ) {
        val state = binding.rvMyPostSections.layoutManager?.onSaveInstanceState()
        adapter.submitList(list) {
            binding.rvMyPostSections.layoutManager?.onRestoreInstanceState(state)
        }
    }

    private fun observeAllLists() {
        viewLifecycleOwner.lifecycleScope.launch {
            forYouActivityViewModel.feedData.collect {
                myPost2ViewModel.onPreviewSectionUpdate()
            }
        }

        myPost2ViewModel.readingArticleItemsList.observe(viewLifecycleOwner) {
            myPost2ViewModel.onPreviewSectionUpdate()
        }

        myPost2ViewModel.readingHistoryArticleItemsList.observe(viewLifecycleOwner) {
            myPost2ViewModel.onPreviewSectionUpdate()
        }

        myPost2ViewModel.getFollowSnapshot().observe(viewLifecycleOwner) {
            myPost2ViewModel.onPreviewSectionUpdate()
        }
    }
}
