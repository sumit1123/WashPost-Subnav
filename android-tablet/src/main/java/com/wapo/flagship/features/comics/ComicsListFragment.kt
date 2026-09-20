package com.wapo.flagship.features.comics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.content.ContentActivity
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.nightmode.NightModeController
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.wapo.flagship.features.sections.utils.AnimationHelper
import com.wapo.flagship.util.UIUtil
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.comics.model.AdItem
import com.washingtonpost.android.comics.model.ComicItem
import com.washingtonpost.android.comics.model.ComicStrip
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.AdSectionConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ComicsListFragment :
    BaseSectionFragment() {
    enum class State {
        LOADING,
        LOADED,
        ERROR,
    }

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics
    private lateinit var list: RecyclerView
    private lateinit var comicsListAdapter: ComicsListAdapter
    private var subscription: Subscription? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val pullToRefreshClickListener =
        OnRefreshListener {
            setRefreshing(true)
            updateContentViewState(State.LOADING)
            updateList()
        }
    private var asyncAnim: ImageView? = null
    private var asyncUILoadingAnimation: Animation? = null
    private var statusContainer: View? = null
    private var retryButton: View? = null
    private var retryClickListener =
        View.OnClickListener {
            updateContentViewState(State.LOADING)
            updateList()
        }
    private var nightModeEnabled: Boolean = false

    @Inject lateinit var adService: AdService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.ComicsRenderEvent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.fragment_comics_list, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        nightModeEnabled = (context?.applicationContext as? NightModeController)?.isNightModeEnabled() ?: false

        comicsListAdapter = ComicsListAdapter(emptyList(), nightModeEnabled)

        list =
            view.findViewById<RecyclerView>(R.id.comics_list_rv).apply {
                comicsListAdapter.setRecyclerViewCacheExtension(this)
                setHasFixedSize(true)
                adapter = comicsListAdapter
            }

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout) as SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(pullToRefreshClickListener)

        asyncAnim = view.findViewById(R.id.async_anim_image_view) as ImageView
        statusContainer =
            view.findViewById(
                com.washingtonpost.android.sections.R.id.status_container,
            )
        retryButton = view.findViewById(R.id.retry) as Button
        retryButton?.setOnClickListener(retryClickListener)

        updateContentViewState(State.LOADING)
        updateList()

        observeAdsMode()
    }

    private fun observeAdsMode() {
        viewLifecycleOwner.lifecycleScope.launch {
            adService.adsMode.collect {
                updateList()
            }
        }
    }

    override fun onStop() {
        subscription?.unsubscribe()
        super.onStop()
    }

    override fun getAdKey(): String? = null

    override fun getBundleName(): String? = arguments?.getString(ARG_BUNDLE_NAME)

    override fun getTracking(): Tracking? =
        sectionDisplayName?.run {
            Tracking(
                pageName = "$bundleName",
                platform = "",
                site = "",
                pageType = "",
                section = "",
                channel = Measurement.CHANNEL_COMICS,
                subsection = Measurement.CONTENT_TYPE_COMICS,
                hierarchy = "",
                contentType = Measurement.CONTENT_TYPE_COMICS,
                storyType = "",
                headline = this,
                author = "",
                source = "",
                contentID = "",
                pageNum = "",
                opRanking = "",
                columnName = "",
                blogName = "",
                published = "",
                newsOrCommercial = "",
                commercialNode = "",
                contentCategory = "",
                sectionFront = "",
                trackScrolling = "",
                contentTopics = "",
                pageTitle = "",
                pagePath = "",
            )
        }

    override fun getSectionDisplayName(): String? = arguments?.getString(ARG_DISPLAY_NAME)

    override fun scrollToTop() {
        // do nothing
    }

    override fun smoothScrollToTop() {
        // do nothing
    }

    private fun updateList() {
        subscription?.unsubscribe()
        subscription =
            (activity as ContentActivity)
                .getContentManagerObs()
                .flatMap { cm ->
                    cm.comicsList
                }.filter { l -> l != null }
                .map { list ->
                    Collections.sort(list, ComicsStripComparator())
                    list
                }.timeout(UI_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        it?.let {
                            subscription?.unsubscribe()
                            updateContentViewState(
                                if (it.isNotEmpty()) State.LOADED else State.ERROR,
                            )
                            comicsListAdapter.setData(addAdsToComicsItemList(it,
                                ConfigManager.getInstance().config.adsConfig.comics))
                            comicsListAdapter.notifyDataSetChanged()
                        }
                    },
                    {
                        subscription?.unsubscribe()
                        updateContentViewState(State.ERROR)
                    },
                    {
                        subscription?.unsubscribe()
                        updateContentViewState(State.LOADED)
                    },
                )
    }

    private fun addAdsToComicsItemList(
        list: List<ComicStrip>,
        adSectionConfig: AdSectionConfig?
    ): List<ComicItem> {
        val comicItemList = mutableListOf<ComicItem>()
        val firstItemDelay = adSectionConfig?.firstItemDelay ?: 0
        val adIntervals = adSectionConfig?.adItemInterval ?: 0

        if (adIntervals == 0 || adSectionConfig?.enabled == false || adService.currentAdsMode == AdsModel.Disabled) {
            return list
        }

        var adPositionCounter = 1
        var currentIndex = 0
        val remainingSize = list.size - firstItemDelay
        val endIndex = if (remainingSize % adIntervals == 0) {
            list.size
        } else {
            (remainingSize - (remainingSize % adIntervals)) + firstItemDelay
        }


        while (currentIndex < endIndex) {
            if (currentIndex == 0 && firstItemDelay > 0) {
                for (i in 0 until firstItemDelay) {
                    comicItemList.add(list[i])
                }
                currentIndex = firstItemDelay
            } else {
                for (i in currentIndex until currentIndex+adIntervals) {
                    comicItemList.add(list[i])
                }
                currentIndex += adIntervals
            }

            val adPositionPath = if (UIUtil.isPhone(context)) {
                "incontent-mob_" + adPositionCounter++
            } else {
                "incontent_" + adPositionCounter++
            }

            comicItemList.add(AdItem(commercialNode = adSectionConfig?.commercialNode ?: "section", adPosition = adPositionPath))
        }
        while (currentIndex < list.size) {
            comicItemList.add(list[currentIndex])
            currentIndex++
        }
        return comicItemList
    }

    private fun setRefreshing(refreshing: Boolean) {
        swipeRefreshLayout.isRefreshing = refreshing
    }

    private fun startAsyncLoadingAnim() {
        asyncAnim?.let {
            it.visibility = View.VISIBLE
            if (asyncUILoadingAnimation == null) {
                asyncUILoadingAnimation =
                    AnimationUtils.loadAnimation(
                        context,
                        com.washingtonpost.android.articles.R.anim.horizontal_anim,
                    )
            }
            it.startAnimation(asyncUILoadingAnimation)
        }
    }

    private fun stopAsyncLoadingAnim() {
        asyncAnim?.let {
            it.clearAnimation()
            AnimationHelper.fadeOut(asyncAnim, null)
        }
    }

    private fun updateContentViewState(state: State) {
        when (state) {
            State.LOADING -> {
                list.visibility = View.GONE
                statusContainer?.visibility = View.GONE
                startAsyncLoadingAnim()
            }
            State.LOADED -> {
                setRefreshing(false)
                list.visibility = View.VISIBLE
                statusContainer?.visibility = View.GONE
                stopAsyncLoadingAnim()
                loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.ComicsRenderEvent)
            }
            State.ERROR -> {
                setRefreshing(false)
                list.visibility = View.GONE
                statusContainer?.visibility = View.VISIBLE
                stopAsyncLoadingAnim()
                loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.ComicsRenderEvent)
            }
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.comics_list_rv)?.apply {
            comicsListAdapter.cleanup(this)
        }
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BUNDLE_NAME: String = "ARG_BUNDLE_NAME"
        private const val ARG_DISPLAY_NAME: String = "ARG_DISPLAY_NAME"
        private const val UI_TIMEOUT_IN_MILLIS: Long = 7500

        @JvmStatic
        fun create(
            bundleName: String?,
            displayName: String?,
        ): ComicsListFragment =
            ComicsListFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_BUNDLE_NAME, bundleName)
                        putString(ARG_DISPLAY_NAME, displayName)
                    }
            }
    }
}
