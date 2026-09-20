package com.wapo.flagship.features.search2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.decorator.Decorator
import com.wapo.android.commons.util.px
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.flagship.features.search2.ui.decor.*
import com.wapo.flagship.features.search2.utils.SearchViewStateHelper
import com.wapo.flagship.features.search2.viewmodel.SearchViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentSearch2Binding

abstract class SearchBaseFragment : Fragment() {
    private var _binding: FragmentSearch2Binding? = null
    protected val binding get() = _binding

    /**
     * ViewStateHelper manages the various dialogs to be shown for Gift Sender Flow
     */
    private var _searchViewStateHelper: SearchViewStateHelper? = null
    protected val searchViewStateHelper get() = _searchViewStateHelper!!

    protected val decorator by lazy {
        when (searchViewModel.searchMode.value) {
            else -> getRegularDecorator()
        }
    }

    protected val searchViewModel: SearchViewModel by activityViewModels()
    protected val askQuestionsViewModel: AskQuestionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentSearch2Binding.inflate(inflater, container, false)
        _searchViewStateHelper = binding?.let { SearchViewStateHelper(it, requireContext()) }
        return binding?.root
    }

    override fun onDestroyView() {
        _binding = null
        _searchViewStateHelper = null
        super.onDestroyView()
    }

    private fun getRegularDecorator(): RecyclerView.ItemDecoration =
        Decorator
            .Builder()
            .underlay(RoundDecor(6.px.toFloat(), roundPolitic = RoundPolitic.Group()))
            .offset(
                SimpleOffsetDrawer(
                    left = 16.px,
                    right = 16.px,
                ),
            ).overlay(
                LinearDividerDrawer(
                    Gap(
                        ContextCompat.getColor(requireContext(), R.color.search_separator),
                        1.px,
                        paddingStart = 32.px,
                        paddingEnd = 32.px,
                        rule = Rules.MIDDLE,
                    ),
                ),
            ).build()
}
