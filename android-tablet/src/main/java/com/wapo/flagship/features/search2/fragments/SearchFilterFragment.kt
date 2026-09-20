package com.wapo.flagship.features.search2.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wapo.android.commons.decorator.Decorator
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.px
import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.ui.adapter.FilterAdapter
import com.wapo.flagship.features.search2.ui.decor.FilterDividerDrawer
import com.wapo.flagship.features.search2.ui.decor.Gap
import com.wapo.flagship.features.search2.ui.decor.Rules
import com.wapo.flagship.features.search2.utils.FilterViewStateHelper
import com.wapo.flagship.features.search2.viewmodel.FilterViewModel
import com.wapo.flagship.features.sections.utils.UIUtils
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentSearch2FilterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFilterFragment : BaseBottomSheetDialogFragment() {
    private var _binding: FragmentSearch2FilterBinding? = null
    private val binding get() = _binding!!

    private var _filterViewStateHelper: FilterViewStateHelper? = null
    private val filterViewStateHelper get() = _filterViewStateHelper!!

    private val filterViewModel: FilterViewModel by activityViewModels()

    private val dividerDrawer2Dp by lazy {
        FilterDividerDrawer(
            Gap(
                ContextCompat.getColor(requireContext(), R.color.search_separator),
                1.px,
                paddingStart = 16.px,
                paddingEnd = 16.px,
                rule = Rules.MIDDLE,
            ),
        )
    }

    private val decorator by lazy {
        Decorator
            .Builder()
            .overlay(dividerDrawer2Dp)
            .build()
    }

    override fun onAttach(context: Context) {
        behaviorState = BottomSheetBehavior.STATE_EXPANDED
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentSearch2FilterBinding.inflate(inflater, container, false)
        _filterViewStateHelper = FilterViewStateHelper(binding, requireContext())
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        _filterViewStateHelper = null
        super.onDestroyView()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initSearchRV()
        initFilterScreen()
        observeFilterItems()
        observeFilterEvents()
    }

    private fun initFilterScreen() {
        this.getBottomSheetBehavior()?.maxHeight =
            AppContextUtils.getScreenHeight() - UIUtils.dpToPx(234f, resources)
        binding.reset.setOnClickListener {
            filterViewModel.filterClicked(FilterEvent.Reset)
        }
        binding.close.setOnClickListener {
            filterViewModel.filterClicked(FilterEvent.Close)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        filterViewModel.filterClicked(FilterEvent.Apply)
        super.onDismiss(dialog)
    }

    private fun initSearchRV() {
        binding.rvFilter.apply {
            layoutManager = LinearLayoutManager(context)
            adapter =
                FilterAdapter {
                    filterViewModel.filterClicked(it)
                }
            addItemDecoration(decorator)
        }
    }

    private fun observeFilterItems() {
        filterViewModel.filterMap.observe(viewLifecycleOwner) {
            filterViewStateHelper.loadFilters(it)
        }
    }

    private fun observeFilterEvents() {
        filterViewModel.filterEvent.observe(viewLifecycleOwner) {
            when (it) {
                FilterEvent.Close -> dismiss()
                is FilterEvent.Radio -> filterViewModel.updateRadioItemActiveFilter(it.item)
                is FilterEvent.Check -> filterViewModel.updateCheckItemActiveFilters(it.item)
                FilterEvent.Reset -> filterViewModel.resetFilters()
                is FilterEvent.ExpandCollapse -> filterViewModel.updateExpandCollapse(it.item)
                else -> {}
            }
        }
    }
}
