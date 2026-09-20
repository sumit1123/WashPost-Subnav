package com.wapo.flagship.features.mypost.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.flagship.features.mypost.adapters.SectionDetailRecyclerViewAdapter
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.save.databinding.FragmentMyPostSectionDetailBinding
import com.washingtonpost.android.save.decorators.SectionsItemDecoration
import com.washingtonpost.android.save.models.DetailItem
import com.washingtonpost.android.save.models.DetailItem.Article
import com.washingtonpost.android.save.models.DetailItem.Empty
import com.washingtonpost.android.save.models.DetailItem.FooterArchive
import com.washingtonpost.android.save.models.DetailItem.Header
import com.washingtonpost.android.save.types.MyPostSection
import com.washingtonpost.android.save.viewholders.SectionDetailArticleViewHolder
import com.washingtonpost.android.save.views.DetailGridLayoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SectionDetailFragment : Fragment() {
    private lateinit var binding: FragmentMyPostSectionDetailBinding

    private val myPost2ViewModel: MyPost2ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyPostSectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        binding.swipeRefreshLayout.setOnRefreshListener {
            myPost2ViewModel.refresh()
            binding.rvMyPostSectionsDetail.adapter?.notifyDataSetChanged()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initRecyclerView() {
        binding.rvMyPostSectionsDetail.apply {
            if (!DeviceUtils.isTablet(context)) {
                layoutManager = LinearLayoutManager(requireContext())
            } else {
                layoutManager = DetailGridLayoutManager(context)
            }
            adapter =
                SectionDetailRecyclerViewAdapter(
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
                        myPost2ViewModel.handleSignInClickEvent()
                    },
                    {
                        myPost2ViewModel.handleSettingsClickEvent()
                    },
                    {
                        myPost2ViewModel.handleViewArchiveClickEvent()
                    },
                    {
                        myPost2ViewModel.handleUpdateConsentSettingsClickEvent()
                    }
                ).apply {
                    myPost2ViewModel.section.value?.let {
                        setUpDecorators(it)
                        observeSignInAttemptEvent(it, this)
                        observeTargetingConsentChangeEvent(it, this)
                        onDetailSectionUpdate(it, this)
                    }
                }
            itemAnimator = null
            setItemsAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefreshLayout.isEnabled = myPost2ViewModel.canSwipeToRefresh()
    }

    private fun setUpDecorators(section: MyPostSection) {
        if (DeviceUtils.isTablet(context)) {
            binding.rvMyPostSectionsDetail.apply {
                (layoutManager as DetailGridLayoutManager).update()
                addItemDecoration(
                    SectionsItemDecoration(
                        bottomMargin = com.washingtonpost.android.save.R.dimen.my_post_tablet_detail_gutter_width,
                        leftEdgeMargin = R.dimen.my_post_card_content_left_margin,
                        rightEdgeMargin = R.dimen.my_post_card_content_left_margin,
                        gutterWidth = com.washingtonpost.android.save.R.dimen.my_post_tablet_detail_gutter_width,
                        startPosition = if (myPost2ViewModel.shouldDisplayBanner(section.name)) 2 else 1,
                    ),
                )
            }
        }
    }

    private fun setItemsAnimation() {
        binding.rvMyPostSectionsDetail.layoutAnimation =
            if (DeviceUtils.isTablet(context)) {
                AnimationUtils.loadLayoutAnimation(
                    requireContext(),
                    com.washingtonpost.android.save.R.anim.slide_up_400_grid_layout_animation,
                )
            } else {
                val anim = AnimationUtils.loadAnimation(requireContext(), com.washingtonpost.android.save.R.anim.slide_up_400)
                object : LayoutAnimationController(anim) {
                    override fun getDelayForView(view: View?): Long {
                        view?.let {
                            val pos = binding.rvMyPostSectionsDetail.getChildAdapterPosition(it)
                            if (pos == 1) {
                                val vh = binding.rvMyPostSectionsDetail.getChildViewHolder(view)
                                if (vh is SectionDetailArticleViewHolder) {
                                    // no delay for the first and second items to animate header and the first item as a single card in phones.
                                    return 0
                                }
                            }
                        }
                        return super.getDelayForView(view)
                    }
                }.apply {
                    // value should match with the android:delay value in slide_up_400_layout_animations.xml file.
                    delay = 0.25f
                }
            }
    }

    private fun observeSignInAttemptEvent(
        section: MyPostSection,
        adapter: SectionDetailRecyclerViewAdapter,
    ) {
        myPost2ViewModel.signInAttemptEvent.observe(viewLifecycleOwner) {
            onDetailSectionUpdate(section, adapter)
        }
    }

    /**
     * Observes changes in Targeting consent state to control For You empty state.
     */
    private fun observeTargetingConsentChangeEvent(
        section: MyPostSection,
        adapter: SectionDetailRecyclerViewAdapter,
    ) {
        myPost2ViewModel.targetingEnabled.observe(viewLifecycleOwner) {
            onDetailSectionUpdate(section, adapter)
        }
    }

    private fun onDetailSectionUpdate(
        section: MyPostSection,
        adapter: SectionDetailRecyclerViewAdapter,
    ) {
        val layoutManager = binding.rvMyPostSectionsDetail.layoutManager as? DetailGridLayoutManager
        when (section) {
            MyPostSection.SAVED_STORIES -> {
                layoutManager?.let {
                    it.layoutHasFooter = true
                }
                myPost2ViewModel.readingArticleItemsList.removeObservers(viewLifecycleOwner)
                myPost2ViewModel.readingArticleItemsList.observe(viewLifecycleOwner) { itemsList ->
                    val items = mutableListOf<DetailItem>()
                    if (myPost2ViewModel.hasAnyEmptyState(section)) {
                        myPost2ViewModel.getEmptyState(section)?.let {
                            items.add(Empty(section, it))
                        }
                    } else {
                        items.add(Header(section))
                        itemsList?.forEach {
                            items.add(Article(section, it))
                        }
                        items.add(FooterArchive(section))
                    }
                    submitList(adapter, items)
                }
            }
            MyPostSection.READING_HISTORY -> {
                myPost2ViewModel.readingHistoryArticleItemsList.removeObservers(viewLifecycleOwner)
                myPost2ViewModel.readingHistoryArticleItemsList.observe(viewLifecycleOwner) { itemsList ->
                    val items = mutableListOf<DetailItem>()
                    if (myPost2ViewModel.hasAnyEmptyState(section)) {
                        myPost2ViewModel.getEmptyState(section)?.let {
                            items.add(Empty(section, it))
                        }
                    } else {
                        items.add(Header(section))
                        itemsList?.forEach {
                            items.add(Article(section, it))
                        }
                    }
                    submitList(adapter, items)
                }
            }
            MyPostSection.PURCHASE -> {
                myPost2ViewModel.purchasedArticles.removeObservers(viewLifecycleOwner)
                myPost2ViewModel.purchasedArticles.observe(viewLifecycleOwner) { itemsList ->
                    val items = mutableListOf<DetailItem>()
                    if (myPost2ViewModel.hasAnyEmptyState(section)) {
                        myPost2ViewModel.getEmptyState(section)?.let {
                            items.add(Empty(section, it))
                        }
                    } else {
                        items.add(Header(section))
                        itemsList?.forEach {
                            items.add(Article(section, it))
                        }
                    }
                    submitList(adapter, items)
                }
            }
            else -> {
                // no-op
            }
        }
    }


    private fun submitList(
        adapter: SectionDetailRecyclerViewAdapter,
        list: List<DetailItem>,
    ) {
        val state = binding.rvMyPostSectionsDetail.layoutManager?.onSaveInstanceState()
        adapter.submitList(list) {
            binding.rvMyPostSectionsDetail.layoutManager?.onRestoreInstanceState(state)
        }
    }
}
