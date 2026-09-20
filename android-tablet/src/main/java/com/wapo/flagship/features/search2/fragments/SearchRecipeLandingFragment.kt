package com.wapo.flagship.features.search2.fragments

import android.os.Bundle
import com.wapo.flagship.features.grid.FusionSectionFragment
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchRecipeLandingFragment : FusionSectionFragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.RecipesRenderEvent)
    }

    override fun onPageReady() {
        super.onPageReady()

        loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.RecipesRenderEvent)
    }
}
