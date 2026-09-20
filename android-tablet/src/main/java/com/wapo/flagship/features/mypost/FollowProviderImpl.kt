package com.wapo.flagship.features.mypost

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.wapo.android.commons.constants.AUTHORIZATION
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.CLIENT_ID
import com.wapo.android.commons.constants.CLIENT_IP
import com.wapo.android.commons.constants.CLIENT_USER_AGENT
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.data.RecentSection
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.BottomTabFragment
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.views.SnackbarFactory
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.activity.AuthorPageActivity
import com.washingtonpost.android.follow.helper.FollowProvider
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.volley.RequestQueue
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import java.util.UUID

class FollowProviderImpl : FollowProvider {
    private val followConfig get() = ConfigManager.getInstance().config.followConfig

    override val requestQueue: RequestQueue = FlagshipApplication.getInstance().requestQueue

    override val animatedImageLoader: AnimatedImageLoader = FlagshipApplication.getInstance().animatedImageLoader

    override val authorFollowUrl: String? = followConfig.authorFollowUrl

    override fun logException(t: Throwable) {
        CrashWrapper.sendException(t)
    }

    override fun openArticles(
        context: Context?,
        urls: Array<String>,
        url: String,
    ) {
        context ?: return

        var tabName = BottomTab.Home.title
        var sectionDisplayName = "FOLLOWING"
        if (context is MyPostAuthorPageActivity) {
            tabName = "Following"
            sectionDisplayName = Measurement.bioPageAuthorName()
        }

        val intent =
            ArticlesParcel
                .builder()
                .setArticleUrls(urls.toList(), urls.indexOf(url))
                .setTabName(tabName)
                .setSectionDisplayName(sectionDisplayName)
                .followOriginated(true)
                .buildIntent(context)
        context.startActivity(intent)
    }

    override fun onAuthorFollowed(
        view: View,
        followId: String?,
    ) {
        SnackbarFactory.authorFollow(view, followId).show()
    }

    override fun onMaxFollowReached(
        context: Context?,
        author: AuthorItem,
    ) {
        val alertDialog = AlertDialog.Builder(context, R.style.dialog_style).create()
        val message = context?.resources?.getString(R.string.max_follow_reached_message)
        alertDialog.setMessage(message?.format(author.name))
        // TODO: Uncomment this when unfollow button is added to My Post
        /*alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context?.resources?.getString(R.string.max_follow_reached_positive_button_text)) {
            dialog, _ ->
            dialog.dismiss()
            goToMyPost(context)
        }*/
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            context?.resources?.getString(R.string.max_follow_reached_negative_button_text),
        ) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    override fun startAuthorPageActivity(
        activity: Context,
        author: AuthorItem,
    ) {
        val intent =
            Intent(activity, MyPostAuthorPageActivity::class.java).apply {
                putExtra(AuthorPageActivity.PARAM_AUTHOR, author)
            }
        activity.startActivity(intent)
    }

    override fun isLoggedInUserAndSubscriber(): Boolean =
        PaywallService.initialized() &&
            PaywallService.getInstance().isPremiumUser &&
            PaywallService.getInstance().isWpUserLoggedIn

    override fun handleSignInOrCreateAccount(activity: Context) {
        val link = activity.getString(com.washingtonpost.android.R.string.uri_subs_signin)
        DeepLinksProcessor.processAsync(
            link,
            activity,
            scope = (activity as? ComponentActivity)?.lifecycleScope
        )
    }

    override fun getFollowRequestHeaders(): HashMap<String, String> =
        hashMapOf(
            Pair(
                AUTHORIZATION,
                "Bearer " +
                    AuthHelper
                        .getInstance(
                            FlagshipApplication.getInstance().applicationContext,
                        ).accessToken,
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
        )

    override fun getAuthorBaseUrl(): String? = followConfig.authorFollowBaseSyncUrl

    override fun isConnected(): Boolean = ReachabilityUtil.isConnected(FlagshipApplication.getInstance().applicationContext)

    override fun isLoggedInUser(): Boolean {
        return PaywallService.getInstance()?.isWpUserLoggedIn ?: false
    }

    override fun logError(
        errorCode: Int?,
        errorMessage: String?,
        message: String?,
        data: Map<String, Any?>?,
        t: Throwable?,
    ) {
        EventLog
            .Builder()
            .apply {
                setErrorCode(errorCode)
                setErrorMessage(errorMessage)
                setMessage(message)
                setModule(LogModules.FOLLOW)
                data?.forEach { entry ->
                    set(entry.key, entry.value)
                }
            }.run {
                RemoteLog.e(FlagshipApplication.getInstance(), build())
            }
    }

    override fun onTrackingEvent(trackingEvent: TrackingEvent) {
        when (trackingEvent) {
            TrackingEvent.ON_FOLLOWED -> {
                // User follows an author
                Measurement.trackAuthorFollowOrUnfollow(true)
            }
            TrackingEvent.ON_UNFOLLOWED -> {
                // User unfollows an author
                Measurement.trackAuthorFollowOrUnfollow(false)
            }
            TrackingEvent.ON_AUTHOR_CARD_OPEN -> {
                // User taps on byline to view author card
                Measurement.trackAuthorCardOpen()
            }
            TrackingEvent.ON_AUTHOR_PAGE_OPEN_FROM_CARD -> {
                // User navigates to author page from author card
                Measurement.trackAuthorPageOpenFromAuthorCard()
            }
            TrackingEvent.ON_READ_ARTICLE_FROM_FOLLOWING_FEED -> {
                // User reads an article from Following feed
                Measurement.trackReadArticleFromFollowingFeed()
            }
            TrackingEvent.ON_AUTHOR_SELECTED -> {
                // User navigates to author page from author items (top bar)/swipe between authors on following feed
                Measurement.trackNavigateToAuthorPageFromAuthorItems()
            }
            else -> {
                // no op
            }
        }
    }

    override fun onAuthorNameClicked(authorItem: AuthorItem) {
        addAuthorPageToRecentSections(authorItem)
    }

    private fun addAuthorPageToRecentSections(authorItem: AuthorItem) {
        val cm = FlagshipApplication.getInstance().cacheManager
        val recentSections = cm.recentSections
        val recentSectionsToUpdate = mutableListOf<RecentSection>()
        var updatingExistingItem = false

        recentSections?.forEach {
            if (it.menuItemId == FollowTrackingInfo.followTracking.authorId) {
                val oldRecentSection = it
                oldRecentSection.setUpdateStatusDelete()
                recentSectionsToUpdate.add(oldRecentSection)
                updatingExistingItem = true
            }
        }
        if (!updatingExistingItem) {
            if (cm.recentSectionsSize >= BottomTabFragment.LIMIT_RECENT_LIST) {
                val oldRecentSection = cm.recentSections[0]
                oldRecentSection.setUpdateStatusDelete()
                recentSectionsToUpdate.add(oldRecentSection)
            }
        }
        val recentSection =
            RecentSection(
                authorItem.id,
                authorItem.name,
                authorItem.name,
                1,
                MenuSection.SECTION_TYPE_AUTHOR,
            )
        recentSection.setUpdateStatusInsert()
        recentSectionsToUpdate.add(recentSection)
        cm.updateRecentSections(recentSectionsToUpdate)
    }

    override fun getAuthorImageRequestUrl(url: String?): String? = url
}
