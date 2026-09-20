package com.wapo.flagship.features.search2.fragments

import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.setStyleSpan
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.viewmodel.SearchViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentSearch2NoMatchesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchNoMatchesFragment : Fragment() {

    private val searchViewModel: SearchViewModel by activityViewModels()

    private var _binding: FragmentSearch2NoMatchesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentSearch2NoMatchesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initMessage()
    }

    private fun initMessage() {
        when (searchViewModel.searchUiState.value) {
            is SearchUiState.NoMatches -> {
                binding.searchErrorSecondary.setVisible(true)
                binding.imgSearch.setVisible(true)
                val primaryText = resources.getString(R.string.search_no_matches_h1)
                val queryText = "\"${searchViewModel.searchQuery?.query}\""
                val fulltext = "$primaryText $queryText"
                val spannable = SpannableString(fulltext)
                spannable.setStyleSpan(
                    fulltext,
                    queryText,
                    R.style.searchResult_error_h1_bold,
                    requireContext(),
                )
                binding.searchErrorPrimary.text = spannable
            }
            else -> {
                binding.searchErrorSecondary.setVisible(false)
                binding.imgSearch.setVisible(false)
                val primaryText = resources.getString(R.string.search_error_h1)
                binding.searchErrorPrimary.text = primaryText
            }
        }
    }

    companion object {
        @JvmField
        val FRAGMENT_TAG = SearchNoMatchesFragment::class.java.name + ".fragmentTag"
    }
}
