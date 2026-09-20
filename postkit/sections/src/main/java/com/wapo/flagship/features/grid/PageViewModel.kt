package com.wapo.flagship.features.grid

import com.wapo.android.commons.util.Logger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wapo.android.commons.exceptions.GenericLoggableError
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.PageModelMapper
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.nightmode.NightModeManager
import com.wapo.flagship.features.sections.PageLayout
import com.wapo.flagship.features.sections.PageManager
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

class PageViewModel(
    nightModeManager: NightModeManager,
    private val loginStatusProvider: () -> Boolean,
    private val pageConfig: () -> PageConfig
) : ViewModel() {
    private val _pageData = MutableLiveData<PageState>()
    val pageData: LiveData<PageState> = _pageData

    private val _nightModeData = MutableLiveData<NightModeStatus>()
    val nightModeData: LiveData<NightModeStatus> = _nightModeData

    private var pageManager: PageManager? = null
    private var pageId: String? = null

    private val _breakingNewsData = MutableLiveData<List<BarEntity>>()
    val breakingNewsData: LiveData<List<BarEntity>> = _breakingNewsData

    var lastReceivedPage: GridEntity? = null
    private var nightModeSubscription: Subscription? = null
    private var pageSubscription: Subscription? = null
    private var refreshSubscription: Subscription? = null

    init {
        nightModeSubscription = nightModeManager
                .getNightModeStatus()
                .subscribe {
                    _nightModeData.value = if (it) NightModeStatus.ON else NightModeStatus.OFF
                }
    }

    fun onPageManagerReady(pageManager: PageManager, pageId: String, isLowDataModeEnable: Boolean) {
        this.pageManager = pageManager
        this.pageId = pageId

        val shouldClearPage = pageManager.shouldClearPage(pageId)
        val shouldUpdatePage = pageManager.shouldUpdatePage(pageId)
        val showProgress = shouldClearPage || _pageData.value !is PageState.Content
        pageManager.onLowDataMode(isLowDataModeEnable)
        _pageData.value = PageState.Loading(showProgress = showProgress, dropPage = shouldClearPage)
        pageSubscription?.unsubscribe()
        pageSubscription = pageManager.listenToPage(pageId, false)
                .first()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { pageLayout ->
                        val shouldRefreshPage = shouldUpdatePage && pageLayout.fusionPage?.checksum != lastReceivedPage?.checksum
                        onPageLoaded(pageLayout, shouldRefreshPage)
                        pageSubscription?.unsubscribe()
                    },
                        {
                            Logger.e(TAG, "Error loading section", it)
                            _pageData.value = PageState.Error(it)
                            pageSubscription?.unsubscribe()
                        },
                        {
                            //when onNext does not get called (e.g. 304 not modified)
                            if (_pageData.value is PageState.Loading) {
                                if (lastReceivedPage != null) {
                                    onPageLoaded(PageLayout(fusionPage = lastReceivedPage))
                                } else {
                                    //cached page is missing
                                    _pageData.value = PageState.Error(RuntimeException("Fallback page null"))
                                }
                            }
                            pageSubscription?.unsubscribe()
                        }
                )
    }

    private fun isLoggedInUser(): Boolean {
        return loginStatusProvider()
    }

    private fun onPageLoaded(pageLayout: PageLayout, refreshPage: Boolean = false) {
        lastReceivedPage = pageLayout.fusionPage

        try {
            val isLoggedIn = isLoggedInUser()
            val grid = lastReceivedPage?.let { PageModelMapper.getGrid(it, isLoggedIn, pageConfig()) }
            if (grid != null) {
                _pageData.value = PageState.Content(grid, refreshPage)
                handleBreakingNews(pageLayout.fusionPage)
            } else {
                _pageData.value = PageState.Error(RuntimeException("Page response is null"))
            }
        } catch (t: Throwable) {
            _pageData.value = PageState.Error(GenericLoggableError(t))
        }

    }

    private fun handleBreakingNews(fusionPage: GridEntity?) {
        fusionPage ?: return
        val breakingNewsItems = fusionPage.regions.flatMap { it.items }.filterIsInstance<BarEntity>()
        _breakingNewsData.value = breakingNewsItems
    }

    fun refreshPage(isLowDataModeEnable: Boolean = false) {
        _pageData.value = PageState.Loading(false)
        val pageId = pageId ?: return
        val pageManager = pageManager ?: return
        pageManager.onLowDataMode(isLowDataModeEnable)
        refreshSubscription?.unsubscribe()
        refreshSubscription = pageManager.updatePage(pageId, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            onPageLoaded(it)
                            refreshSubscription?.unsubscribe()
                        },
                        {
                            Logger.e(TAG, "Error loading section", it)
                            if (AppContextUtils.isConnectingOrConnected()) {
                                // Log Failure to Remote Log
                                val eventLogBuilder = EventLog.Builder()
                                    .setMessage("Section pull to refresh failed")
                                    .setModule(LogModules.SECTIONS)
                                    .set("section_name", pageId)
                                    .set("has_last_received_page", lastReceivedPage != null)
                                    .setErrorMessage(it.message)
                                RemoteLog.e(AppContextUtils.appContext, eventLogBuilder.build())
                            }
                            if (lastReceivedPage != null) {
                                onPageLoaded(PageLayout(fusionPage = lastReceivedPage))
                            } else {
                                _pageData.value = PageState.Error(it)
                            }
                            refreshSubscription?.unsubscribe()
                        },
                        {
                            //when onNext does not get called (e.g. 304 not modified)
                            if (_pageData.value is PageState.Loading) {
                                if (lastReceivedPage != null) {
                                    onPageLoaded(PageLayout(fusionPage = lastReceivedPage))
                                } else {
                                    //cached page is missing
                                    _pageData.value = PageState.Error(RuntimeException("Fallback page null"))
                                }
                            }
                            refreshSubscription?.unsubscribe()
                        }
                )
    }

    override fun onCleared() {
        pageSubscription?.unsubscribe()
        pageSubscription = null
        refreshSubscription?.unsubscribe()
        refreshSubscription = null
        nightModeSubscription?.unsubscribe()
        nightModeSubscription = null
        pageManager = null
        lastReceivedPage = null
        _pageData.value = PageState.None
        super.onCleared()
    }

    fun onPageStop(pageId: String) {
        pageManager?.onPageStop(pageId)
    }

    companion object {
        private val TAG: String = PageViewModel::class.java.simpleName
    }
}

sealed class PageState {
    class Content(val page: Grid, val refreshPage: Boolean) : PageState()
    class Loading(val showProgress: Boolean, val dropPage: Boolean = false) : PageState()
    class Error(val error: Throwable) : PageState()
    data object None : PageState()
}

class PageViewModelFactory(
    private val nightModeManager: NightModeManager,
    private val loginStatusProvider: () -> Boolean,
    private val pageConfig: () -> PageConfig
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PageViewModel(nightModeManager, loginStatusProvider, pageConfig) as T
    }
}

enum class NightModeStatus { ON, OFF }