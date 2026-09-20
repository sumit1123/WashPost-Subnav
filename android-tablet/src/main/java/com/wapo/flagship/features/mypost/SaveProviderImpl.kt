package com.wapo.flagship.features.mypost

import android.content.Context
import android.os.Build
import android.os.Process
import androidx.fragment.app.FragmentActivity
import com.wapo.android.commons.constants.*
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils
import com.wapo.flagship.common.getArticleIndex
import com.wapo.flagship.features.articles2.activities.ArticlesParcel.Companion.builder
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.querypolicies.LMTQueryPolicy
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.save.SaveProvider
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class SaveProviderImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : SaveProvider {
        override fun isLoggedInUser(): Boolean {
            val isLoggedIn = PaywallService.getInstance().isWpUserLoggedIn
            if (isLoggedIn && AuthHelper.getInstance(getAppContext()).accessToken == null) {
                logPreferenceSyncException(Exception("save_sync_error: accessToken is null"))
                return false
            } else if (isLoggedIn && AuthHelper.getInstance(getAppContext()).shouldRefreshAccessToken()) {
                PaywallService.getInstance().refreshAccessToken()
                return false
            }
            return isLoggedIn
        }

        override fun isLoggedInUserAndSubscriber(): Boolean =
            PaywallService.getInstance().isPremiumUser && PaywallService.getInstance().isWpUserLoggedIn

        override fun getPreferencesRequestHeaders(isArchive: Boolean): HashMap<String, String> =
            hashMapOf(
                Pair(
                    AUTHORIZATION,
                    "Bearer " + AuthHelper.getInstance(getAppContext()).accessToken,
                ),
                Pair(CLIENT_ID, PaywallService.getConnector().clientId),
                Pair(CLIENT_IP, PaywallService.getConnector().ipAddress),
                Pair(CLIENT_APP, PaywallService.getConnector().appName),
                Pair(REQUEST_ID, UUID.randomUUID().toString()),
                Pair(DEVICE_ID, PaywallService.getConnector().deviceId),
                Pair(CLIENT_USER_AGENT, PaywallService.getConnector().userAgent),
                Pair(CLIENT_APP_VERSION, PaywallService.getConnector().appVersion),
                Pair(OS_VERSION, Build.VERSION.SDK_INT.toString()),
                Pair(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL),
                Pair(ARCHIVE, isArchive.toString()),
            )

        private val saveConfig get() = ConfigManager.getInstance().config.saveConfig

        override fun getPreferenceBaseURL(): String = saveConfig.preferenceBaseUrl

        override fun getMetadataBaseUrl(): String = saveConfig.metadataServiceBaseUrl

        override fun getAnimatedImageLoader(): AnimatedImageLoader = FlagshipApplication.getInstance().animatedImageLoader

        override fun getSavedArticleManager(): SavedArticleManager = FlagshipApplication.getInstance().savedArticleManager

        override fun updateArticlesIfNeeded(savedArticleList: List<ArticleAndMetadata>) {
            GlobalScope.launch {
                Thread.currentThread().name = "th-savedArticleUpdater"
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                val queries =
                    savedArticleList.map {
                        Query(
                            it.contentURL,
                            LMTQueryPolicy(it.lastUpdated),
                        )
                    }
                FlagshipApplication.getInstance().repo.prefetchArticle(queries)
            }
        }

        override fun getAppContext(): Context = appContext

        override fun logException(throwable: Throwable) {
            CrashWrapper.sendException(throwable)
        }

        override fun logPreferenceSyncException(t: Throwable) {
            EventLog
                .Builder()
                .apply {
                    setMessage("Save Sync Error")
                    setModule(LogModules.SAVE)
                    setErrorMessage(t.message)
                    setUUID(PaywallService.getInstance().uuid)
                }.run {
                    RemoteLog.e(getAppContext(), build())
                }
        }

        override fun logMetadataSyncException(t: Throwable) {
            EventLog
                .Builder()
                .apply {
                    setMessage("MetaData Sync Error")
                    setModule(LogModules.SAVE)
                    setErrorMessage(t.message)
                }.run {
                    RemoteLog.e(getAppContext(), build())
                }
        }

        override fun logPreferenceErrorResponse(
            code: Int,
            errorResponse: String,
        ) {
            EventLog
                .Builder()
                .apply {
                    setMessage("Save Sync Error")
                    setModule(LogModules.SAVE)
                    setErrorMessage(errorResponse)
                    setErrorCode(code)
                    setUUID(PaywallService.getInstance().uuid)
                }.run {
                    RemoteLog.e(getAppContext(), build())
                }
        }

        override fun logMetadataErrorResponse(
            code: Int,
            errorResponse: String,
        ) {
            EventLog
                .Builder()
                .apply {
                    setMessage("MetaData Sync Error")
                    setModule(LogModules.SAVE)
                    setErrorMessage(errorResponse)
                    setErrorCode(code)
                }.run {
                    RemoteLog.e(getAppContext(), build())
                }
        }

        override fun logExtras(message: String) {
            CrashWrapper.logExtras(message)
            EventLog
                .Builder()
                .apply {
                    setMessage(message)
                    setModule(LogModules.SAVE)
                }.run {
                    RemoteLog.d(getAppContext(), build())
                }
        }

        override fun isConnected(): Boolean = ReachabilityUtil.isConnected(getAppContext())

        override fun openWebViewActivity(
            url: String,
            context: Context,
        ) {
            Utils.startWebActivity(url, context)
        }

        override fun openLoginActivity(context: Context) {
            PaywallService.getConnector().showSignInScreen(
                context.findActivityOfType<FragmentActivity>()?.supportFragmentManager,
                AuthIntentBuilder().build(),
                null,
                PaywallConstants.WallType.SAVE_REGWALL,
                false,
                null
            )
        }

        override fun isNightModeOn(): Boolean = FlagshipApplication.getInstance().isNightModeEnabled()

        override fun openArticles(
            context: Context?,
            navigationBehavior: String,
            urls: Array<String>,
            url: String,
            sectionDisplayName: String,
        ) {
            val metaList = mutableListOf<ArticleMeta>()
            urls.forEach { metaList.add(ArticleMeta(it, false)) }
            val sectionPosition = getArticleIndex(metaList, url)
            val intent =
                builder()
                    .setArticleMetas(metaList, sectionPosition)
                    .setSectionDisplayName(sectionDisplayName)
                    .setTabName(context?.getString(R.string.tab_my_post))
                    .setAppSection(sectionDisplayName)
                    .setOmniturePathToView(navigationBehavior)
                    .buildIntent(context)
            context?.startActivity(intent)
        }

        companion object {
            val TAG: String = SaveProviderImpl::class.java.simpleName
        }
    }
