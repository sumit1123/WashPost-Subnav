// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.mypost.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.flagship.Utils
import com.wapo.flagship.features.mypost.adapters.TopicsAdapter
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowCollaborationViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavEvent
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.follow.ui.CardifiedListTopMarginDecorationWithPadding
import com.washingtonpost.android.save.databinding.MyPostTopicsDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPostTopicsDetailFragment : Fragment() {
    private val myPost2ViewModel: MyPost2ViewModel by activityViewModels()
    private val topicFollowCollaborationViewModel: TopicFollowCollaborationViewModel by activityViewModels()
    private val sectionNavViewModel: SectionNavViewModel by activityViewModels()

    private var _binding: MyPostTopicsDetailBinding? = null
    private val binding get() = _binding!!

    private var adapter: TopicsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MyPostTopicsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        adapter =
            TopicsAdapter(
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
            )

        binding.topicList.adapter = adapter

        binding.topicList.apply {
            addItemDecoration(CardifiedListTopMarginDecorationWithPadding(context))
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            myPost2ViewModel.refresh()
            binding.topicList.adapter?.notifyDataSetChanged()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        myPost2ViewModel.contentPacksData.observe(viewLifecycleOwner) {
            adapter?.submitList(it)
        }

        topicFollowCollaborationViewModel.reloadTopicState.observe(viewLifecycleOwner) {
            onUpdate()
        }

        observeSectionNavViewModelEvents()
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

    private fun onUpdate() {
        val items = myPost2ViewModel.topics
        val state = binding.topicList.layoutManager?.onSaveInstanceState()
        adapter?.submitList(items) {
            binding.topicList.layoutManager?.onRestoreInstanceState(state)
        }
    }
}
