package com.wapo.flagship.lifecycle

import android.content.Context
import android.os.AsyncTask
import android.os.Process
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.features.articles.recirculation.RecirculationHook
import com.wapo.flagship.features.mypost.UserArticleStatusMigration
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.util.ChartbeatManager.setUserAnonymous
import com.wapo.flagship.util.ChartbeatManager.setUserLoggedIn
import com.wapo.flagship.util.ChartbeatManager.setUserPaid
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rx.Subscription

class PaywallLifecycleObserver(
    lifecycle: Lifecycle,
    private val forYouFeedRepository: ForYouFeedRepository,
    private val newslettersRepository: NewslettersRepository,
    private val contentManager: ContentManager,
    private val coroutineScopeProvider: CoroutineScopeProvider,
) : FlagshipLifecycleObserver(lifecycle),
    AuthHelper.TokenRefreshListener {
    private var cmApiCallsSubscription: Subscription? = null

    override fun onApplicationStart() {
        super.onApplicationStart()
        val appContext = FlagshipApplication.getInstance().applicationContext
        if (!FlagshipApplication.getInstance().isBillingActivity) {
            if (!PaywallService.initialized()) {
                FlagshipApplication.getInstance().updateConfigsAndInitPaywall()
            } else if (PaywallService.getInstance().isWpUserLoggedIn &&
                appContext != null &&
                AuthHelper
                    .getInstance(
                        appContext,
                    ).shouldRefreshAccessToken()
            ) {
                AuthHelper.getInstance(appContext).refreshAccessToken(this)
            } else {
                onPaywallReady()
            }
        }
        updateUserTrackingForChartbeat()
        attemptPreferencesSync()
    }

    override fun onApplicationStop() {
        super.onApplicationStop()
        FlagshipApplication.getInstance().setShouldSuppressPostPaywallInitVerifyCalls(false)
        if (PaywallService.initialized()) {
            syncSaveData()
            PaywallService.getInstance().tetroManager.sync(
                abTestVariants =
                    Measurement.getABTestingVariants(
                        FlagshipApplication.getInstance().applicationContext,
                    ),
            )
        }
        attemptPreferencesSync()
        cmApiCallsSubscription?.unsubscribe()
        cmApiCallsSubscription = null
    }

    private fun logSavedArticles(context: Context) {
        val interval = (ConfigManager.getInstance().config.savedArticleLogFrequencyDays * 24 * 60 * 60 * 1000).toLong()
        if (interval != 0L) {
            AsyncTask.execute {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                Thread.currentThread().name = "th-savedArticleListCounter"
                val lastLogTime = PrefUtils.getLastSavedArticleCountLogTime(context)
                val now = System.currentTimeMillis()
                if (now - lastLogTime >= interval) {
                    val totalArticles: Long =
                        FlagshipApplication.getInstance().savedArticleManager.getTotalArticles(
                        )
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("Saved Articles Count")
                            setModule(LogModules.SAVE)
                            set("count", totalArticles)
                            setUUID(PaywallService.getInstance().uuid)
                            set("metric", RemoteLog.METRICS)
                        }.run {
                            RemoteLog.d(context, build())
                        }
                    PrefUtils.setLastSavedArticleCountLogTime(context, now)
                }
            }
        }
    }

    private fun updateUserTrackingForChartbeat() {
        when {
            PaywallService.getInstance().isPremiumUser -> setUserPaid()
            PaywallService.getInstance().isWpUserLoggedIn -> setUserLoggedIn()
            else -> setUserAnonymous()
        }
    }

    private fun syncSaveData() {
        if (PaywallService.getInstance() != null) {
            PaywallService.getInstance().makeSaveIdentityPreferencesCallIfConditionsAreMet()
            PaywallService.getInstance().makeSaveIdentityPreferencesCallIfOneTrustConditionsAreMet()
        }

        // sync dependent on paywall initialization
        val cacheManager = FlagshipApplication.getInstance().cacheManager
        if (cacheManager.totalUserArticlesByStatusType == 0L) {
            FlagshipApplication.getInstance().savedArticleManager.synchronize {
                logSavedArticles(FlagshipApplication.getInstance())
            }
        } else {
            UserArticleStatusMigration
                .getInstance(
                    cacheManager,
                    FlagshipApplication.getInstance().savedArticleManager,
                ).migrateIfNeeded()
        }
    }

    override fun onTokenRefresh() {
        onPaywallReady()
        attemptPreferencesSync()
    }

    private fun onPaywallReady() {
        FlagshipApplication.getInstance().makePostInitializeVerifyCalls()
        val currentActivity = FlagshipApplication.getInstance().currentActivity
        val isAppLaunchFromPush = currentActivity?.intent?.let { IntentHelper().isPushOriginated(it) } ?: false
        PaywallService.getInstance().tetroManager.sync(
            1,
            Measurement.getABTestingVariants(FlagshipApplication.getInstance().applicationContext),
        )
        PaywallService.getBillingHelper().onResume(FlagshipApplication.getInstance())

        if(PaywallPrefHelper.getInstance(FlagshipApplication.getInstance().applicationContext).getLoginId(FlagshipApplication.getInstance().applicationContext).equals("")){
            PaywallPrefHelper.getInstance(FlagshipApplication.getInstance().applicationContext).setLoginId(FlagshipApplication.getInstance().applicationContext, PaywallService.getInstance().loginId)
        }
        // Call feature specific (non paywall calls) apis once Top Stories is loaded.
        cmApiCallsSubscription?.unsubscribe()
        cmApiCallsSubscription =
            contentManager.canMakeApiCalls().subscribe { allowed ->
                if (!allowed) return@subscribe
                // Avoid sync when its push
                if (!isAppLaunchFromPush) {
                    // Sync Authors from remote to update the followed authors from the backend {Will update both author and follow entity
                    FollowManager.getInstance(FlagshipApplication.getInstance()).syncAuthorsFromRemote()
                    lifecycle.coroutineScope.launch {
                        if (PaywallService.getInstance().isWpUserLoggedIn) {
                            newslettersRepository.syncNewsletters()
                            FlagshipApplication.getInstance().checkPrefRepo.checkPref()
                        }
                    }
                    attemptPreferencesSync()

                    coroutineScopeProvider.sync.launch(Dispatchers.IO) {
                        forYouFeedRepository.getForYouRecommendations(
                            surface = ForYouFeedRepositoryImpl.SURFACE_AWAKEN,
                        )
                    }
                }
                RecirculationHook.prefetchItems(
                    FlagshipApplication.getInstance().articleRecircCarouselCache,
                )
            }
    }

    // Attempts full preference sync (push dirty then fetch stale) if preconditions met.
    private fun attemptPreferencesSync() {
        val app = FlagshipApplication.getInstance()
        val ctx = app.applicationContext ?: return
        PreferencesSyncCoordinator.synchronize(ctx)
    }
}
