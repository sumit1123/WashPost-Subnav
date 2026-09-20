package com.wapo.flagship.features.newsletter.repo

import android.content.Context
import android.os.Build
import com.wapo.android.commons.util.Logger
import androidx.room.withTransaction
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.CONTENT_TYPE
import com.wapo.android.commons.constants.COOKIE
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey
import com.wapo.flagship.features.newsletter.repo.local.NewslettersDatabase
import com.wapo.flagship.features.newsletter.repo.local.model.NewslettersEntity
import com.wapo.flagship.features.newsletter.repo.remote.NewslettersService
import com.wapo.flagship.features.newsletter.repo.remote.model.NewslettersRequestBody
import com.wapo.flagship.features.newsletter.repo.remote.model.Profile
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.newdata.model.WpUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NewslettersRepository
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val newsletterService: NewslettersService,
        private val newsletterDatabase: NewslettersDatabase,
        private val dispatcherProvider: DispatcherProvider,
    ) {
        var cachedTopicList = LiveEvent<List<NewslettersKey>>()
        private var willSyncNewsletters = false

        fun isNewsletterEnrolled(key: NewslettersKey): Boolean {
            cachedTopicList.value?.let {
                return it.contains(key)
            } ?: return false
        }

        fun isNewsletterEnrolled(id: String?): Boolean {
            id ?: return false
            return isNewsletterEnrolled(NewslettersKey.Id(id))
        }

        suspend fun enrollNewsletter(
            list: List<NewslettersKey>,
            method: String,
            location: String,
            initiative: String,
        ) = withContext(
            dispatcherProvider.io,
        ) {
            try {
                list.map {
                    NewslettersEntity(
                        key = it,
                        lmt = System.currentTimeMillis()
                    )
                }.also {
                    newsletterDatabase.newslettersDao().followTopics(it)
                }
                syncNewsletters(method, location, initiative)
            } catch (e: Exception) {
                val builder =
                    EventLog.Builder().apply {
                        setMessage("newsletters enroll failed")
                        setErrorMessage(e.message)
                        set("cause", e.cause)
                        setModule(LogModules.TOPIC_FOLLOW)
                    }
                RemoteLog.e(context, builder.build())
            }
        }

        suspend fun unenrollNewsletter(
            list: List<String>,
            method: String,
            location: String,
            initiative: String,
        ) = withContext(
            dispatcherProvider.io,
        ) {
            try {
                list.map {
                    NewslettersEntity(
                        key = NewslettersKey.Id(it),
                        lmt = System.currentTimeMillis(),
                        isEnrolled = false
                    )
                }.also {
                    newsletterDatabase.newslettersDao().updateFollowing(it)
                }
                syncNewsletters(method, location, initiative)
            } catch (e: Exception) {
                val builder =
                    EventLog.Builder().apply {
                        setMessage("newsletters unenroll failed")
                        setErrorMessage(e.message)
                        set("cause", e.cause)
                        setModule(LogModules.TOPIC_FOLLOW)
                    }
                RemoteLog.e(context, builder.build())
            }
        }

        suspend fun syncNewsletters(
            method: String? = null,
            location: String? = null,
            initiative: String? = null,
        ) = withContext(
            dispatcherProvider.io,
        ) {
            try {
                if (!PaywallService.getInstance().isWpUserLoggedIn) {
                   Logger.w(TAG, "syncNewsletters skipped: user is not authenticated")
                   return@withContext
                }
                newsletterDatabase.withTransaction {
                    var syncFailed = false

                    // Get all topics that require syncing
                    val newFollowedTopics = newsletterDatabase.newslettersDao().getAllSyncTopics(true)
                    val newUnfollowedTopics =
                        newsletterDatabase.newslettersDao().getAllSyncTopics(false)

                    val profile =
                        Profile(
                            PaywallService.getInstance().loginId,
                            method,
                            location,
                            initiative,
                        )
                    // Sync new followed topics with remote
                    if (newFollowedTopics.isNotEmpty()) {
                        val enrollResult =
                            newsletterService.enrollUser(
                                getHeaderMap(),
                                newFollowedTopics.toBody(profile),
                            )
                        when (enrollResult) {
                            is APIResult.Success -> {
                                newFollowedTopics.forEach { it.isSynced = 1 }
                                newsletterDatabase.newslettersDao().updateFollowing(newFollowedTopics)
                            }
                            is APIResult.Failure -> {
                                syncFailed = true
                                val builder =
                                    EventLog.Builder().apply {
                                        setMessage("newsletters enroll failed")
                                        setErrorCode(enrollResult.statusCode)
                                        set("response", enrollResult.rawResponse)
                                        setModule(LogModules.TOPIC_FOLLOW)
                                    }
                                RemoteLog.e(context, builder.build())
                            }
                            is APIResult.NetworkError -> {
                                syncFailed = true
                            }
                        }
                    }

                    // Sync new unfollowed topics with remote
                    if (newUnfollowedTopics.isNotEmpty()) {
                        val unenrollResult =
                            newsletterService.unenrollUser(
                                getHeaderMap(),
                                newUnfollowedTopics.toBody(profile),
                            )
                        when (unenrollResult) {
                            is APIResult.Success -> {
                                newUnfollowedTopics.forEach { it.isSynced = 1 }
                                newsletterDatabase.newslettersDao().deleteTopics(newUnfollowedTopics)
                            }
                            is APIResult.Failure -> {
                                syncFailed = true
                                val builder =
                                    EventLog.Builder().apply {
                                        setMessage("newsletters unenroll failed")
                                        setErrorCode(unenrollResult.statusCode)
                                        set("response", unenrollResult.rawResponse)
                                        setModule(LogModules.TOPIC_FOLLOW)
                                    }
                                RemoteLog.e(context, builder.build())
                            }
                            is APIResult.NetworkError -> {
                                syncFailed = true
                            }
                        }
                    }

                    // If sync has not failed, fetch remote list and store it as latest followed topics list
                    if (!syncFailed) {
                        when (val topicsResult = newsletterService.getEnrollments(getHeaderMap())) {
                            is APIResult.Success -> {
                                topicsResult.data?.let { list ->
                                    cachedTopicList.postValue(list.map { NewslettersKey.Id(it) })

                                    // Update db to remote list
                                    newsletterDatabase.newslettersDao().deleteAll()
                                    list
                                        .map {
                                            NewslettersEntity(
                                                key = NewslettersKey.Id(it),
                                                lmt = System.currentTimeMillis(),
                                                isSynced = 1,
                                            )
                                        }.also { entities ->
                                            newsletterDatabase.newslettersDao().followTopics(entities)
                                        }
                                }
                            }
                            else -> {
                                syncFailed = true
                            }
                        }
                    }

                    // If sync failed, use list in db
                    if (syncFailed) {
                        newsletterDatabase
                            .newslettersDao()
                            .getAllTopics(true)
                            .map { it.newslettersKey }
                            .let {
                                cachedTopicList.postValue(it)
                            }
                    }
                }
            } catch (e: Exception) {
                val builder =
                    EventLog.Builder().apply {
                        setMessage("newsletters sync failed")
                        setErrorMessage(e.message)
                        set("cause", e.cause)
                        setModule(LogModules.TOPIC_FOLLOW)
                    }
                RemoteLog.e(context, builder.build())
            }
        }

        private fun getHeaderMap(): HashMap<String, String> {
            val headerMap = HashMap<String, String>()
            val loggedInUser = PaywallService.getInstance().loggedInUser
            headerMap[COOKIE] = getCookieString(loggedInUser)
            headerMap[CLIENT_APP] = PaywallService.getConnector().appName
            headerMap[CLIENT_APP_VERSION] = PaywallService.getConnector().appVersion
            headerMap[DEVICE_NAME] = Build.MANUFACTURER + "-" + Build.MODEL
            headerMap[OS_VERSION] = Build.VERSION.SDK_INT.toString()
            headerMap[DEVICE_ID] = PaywallService.getConnector().deviceId
            headerMap[CONTENT_TYPE] = "application/json"
            return headerMap
        }

        private fun List<NewslettersEntity>.toBody(profile: Profile?): NewslettersRequestBody {
            val body = NewslettersRequestBody(this.map { it.newslettersKey }, profile)
            Logger.d(TAG, body.toString())
            return body
        }

        fun setWillSyncNewsletters() {
            willSyncNewsletters = true
        }

        suspend fun syncNewslettersIfNeeded() {
            if (willSyncNewsletters) {
                syncNewsletters()
                willSyncNewsletters = false
            }
        }

        companion object {
            val TAG = NewslettersRepository::class.simpleName

            fun getCookieString(loggedInUser: WpUser) =
                "wapo_login_id=${loggedInUser.uuid}; wapo_secure_login_id=${loggedInUser.secureLoginID}"
        }
    }
