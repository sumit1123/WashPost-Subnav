package com.wapo.flagship.features.search2.fragments

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchElectionLandingFragment : SearchBaseFragment() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initSearchRV { event ->
            handleUIEvent(event)
        }
        observerUiState()
    }

    fun handleUIEvent(event: AskThePostEvent) {
        //Not Used in this fragment, but needed to be passed down to the Search2Adapter
    }

    private fun initSearchRV(askThePostUIEvent: (AskThePostEvent) -> Unit) {
        binding?.rvSearch?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter =
                Search2Adapter(
                    askThePostUIEvent = askThePostUIEvent,
                    shouldSuppressAds = FlagshipApplication.getInstance().shouldSuppressAds()
                ) {
                    searchViewModel.itemClicked(it)
                }
            addItemDecoration(decorator)
        }
    }

    private fun observerUiState() {
        searchViewModel.searchUiState.observe(viewLifecycleOwner) {
            when (it) {
                SearchUiState.Loading -> searchViewStateHelper.showLoading()
                is SearchUiState.ElectionLanding -> {
                    searchViewStateHelper.showElectionStateList(it.stateslist)
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    companion object {
        @JvmField
        val FRAGMENT_TAG = SearchElectionLandingFragment::class.java.name + ".fragmentTag"
    }
}
