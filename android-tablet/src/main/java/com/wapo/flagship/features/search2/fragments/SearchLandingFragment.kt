package com.wapo.flagship.features.search2.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchLandingFragment : SearchBaseFragment() {

    @Inject lateinit var adService: AdService

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initSearchRV {
            handleUIEvent(it)
        }
        observerUiState()
        observeAdsMode()
    }

    fun handleUIEvent(event: AskThePostEvent) {
        //Not Used in this fragment, but needed to be passed down to the Search2Adapter
    }

    private fun initSearchRV(askThePostUIEvent: (AskThePostEvent) -> Unit) {
        binding?.rvSearch?.apply {
            // Clear existing decorations to prevent stacking on re-init
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            layoutManager = LinearLayoutManager(context)
            adapter =
                Search2Adapter(
                    askQuestionsViewModel,
                    askThePostUIEvent,
                    adService.currentAdsMode == AdsModel.Disabled
                ) {
                    searchViewModel.itemClicked(it)
                }
            addItemDecoration(decorator)
        }
    }

    private fun observeAdsMode() {
        viewLifecycleOwner.lifecycleScope.launch {
            adService.adsMode.collect {
                // Refresh the adapter when ad status changes
                initSearchRV {
                    handleUIEvent(it)
                }
            }
        }
    }

    private fun observerUiState() {
        searchViewModel.searchUiState.observe(viewLifecycleOwner) {
            when (it) {
                SearchUiState.Loading -> searchViewStateHelper.showLoading()
                is SearchUiState.Landing -> {
                    searchViewStateHelper.showRecentSearches(it.recents, it.top)
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    companion object {
        @JvmField
        val FRAGMENT_TAG = SearchLandingFragment::class.java.name + ".fragmentTag"
    }
}
