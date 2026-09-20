package com.washingtonpost.android.follow.helper

import android.app.Application
import com.wapo.android.commons.util.Logger
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import com.washingtonpost.android.follow.database.FollowDatabase
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.database.model.FollowEntity
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.follow.network.AuthorData
import com.washingtonpost.android.follow.network.AuthorRequest
import com.washingtonpost.android.follow.network.FollowAuthor
import com.washingtonpost.android.follow.network.FollowAuthorRepo
import com.washingtonpost.android.volley.DefaultRetryPolicy
import com.washingtonpost.android.volley.Response
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FollowManager private constructor(app: Application) {
    val followProvider = (app as FollowApplication).followProvider
    val followDb = FollowDatabase.getInstance(app)
    val networkExecutor: ExecutorService = Executors.newFixedThreadPool(5)

    private val retryPolicy = DefaultRetryPolicy(5000, -1, 0f)
    private val syncMutex = Mutex()
    private var isFollowing = true   //Default to set this to true

    private val followNetwork: FollowAuthorRepo.FollowAuthorService =
        FollowAuthorRepo.getInstance()
            .geAuthorServiceNetwork(followProvider.getAuthorBaseUrl().toString())

    /***
     * This method will take care of sync follow author  with the follow services
     * Before Sync we check to see if the network is connected and isLoggedInUser
     */
    suspend fun followAuthor() {
        GlobalScope.launch {
            if (followProvider.isConnected() && followProvider.isLoggedInUser()) {
                isFollowing = true // IsFollowing flag is true when the Author is followed
                syncAuthor()
            }
        }
    }

    /***
     * This method will take care of sync un-follow author  with the unfollow services
     * Before Sync  check to see if the network is connected and isLoggedInUser
     */

    fun unFollowAuthor() {
        GlobalScope.launch {
            if (followProvider.isLoggedInUser() && followProvider.isConnected()) {
                isFollowing = false
                syncAuthor()
            }
        }
    }

    /***
     * This method will take care of syncing follow author to author services
     * Getting FollowedAuthorsBySync by Sync status and making request to update the follow authors bases in the Sync status
     * As part of the request we are sending request headers mainly @AccessToken & @ClientId that we get from the user profile
     * On Successful response updating the follow entity with the remote records to followDB
     */

     suspend fun syncAuthor() {
        try {
            followDb.withTransaction {

                //Gives back list of authors with sync false
                val followAuthors = followDb.followDao().getFollowUnFollowAuthors(isFollowing)

                val followList = mutableListOf<AuthorData>()
                followAuthors.forEach {
                    val entity = AuthorData(it.authorId, it.isFollowing, it.lmt)
                    followList.add(entity)
                }
                val response = if (isFollowing) {
                    followNetwork.followAuthor(followProvider.getFollowRequestHeaders(), FollowAuthor(followList)).execute()
                }else{
                    followNetwork.unFollowAuthor(followProvider.getFollowRequestHeaders(), FollowAuthor(followList)).execute()
                }
                if (response.code() == 200) {
                    //Only take top 25 recently updated authors from the response. We want to limit the authors we are storing top 25.
                    val authorsList = response.body()?.authorList?.sortedByDescending { it.lastUpdated }?.take(MAX_FOLLOWERS)
                    val followList = mutableListOf<FollowEntity>()
                    authorsList?.forEach {
                        if(it.id != null) {
                           val isAuthorMetaDataAvailable =  followDb.authorDao().checkAuthorExist(it.id)
                                followList.add(FollowEntity(it.id, it.lastUpdated as Long, it.following, SYNCED, isAuthorMetaDataAvailable)
                                )
                        }
                    }
                    if (isFollowing) {
                        followDb.followDao().syncFollowAuthor(*followList.toTypedArray())
                    }else{
                        followDb.followDao().updateFollowing(*followList.toTypedArray())
                        followDb.followDao().removeFollowing(*followAuthors.toTypedArray())
                    }
                    syncAuthorsFromRemote()
                    Logger.d("follow_author_response", "" + response.body())
                } else {
                    val data = hashMapOf("isFollowing" to isFollowing)
                    followProvider.logError(response.code(), response.message(), "Follow Sync Error.", data, null)
                }
            }
            followDb.authorDao().removeAuthorForUnFollow()
        }catch (e: Exception) {
            followProvider.logException(e)
            followProvider.logError(-1, e.message, "Follow Sync Exception.", null, e)
        }
    }

    @WorkerThread
    fun syncAuthorsFromRemote() {
        if (followProvider.isLoggedInUser() && followProvider.isConnected()) {
            GlobalScope.launch {
                try {
                    syncMutex.withLock {
                        followDb.withTransaction {
                            //This block will execute when the App resumes , Sync any pending records to remote
                            val followAuthors = followDb.followDao().getFollowUnFollowAuthors(isFollowing)
                            if (!followAuthors.isNullOrEmpty()) {
                                syncAuthor()
                            } else {
                                val response = followNetwork.syncAuthorMetaFromRemote(followProvider.getFollowRequestHeaders()).execute()
                                if (response.code() == 200) {
                                    //Only take top 25 recently updated authors from the response. We want to limit the authors we are storing top 25.
                                    val authorsList = response.body()?.authorList?.sortedByDescending { it.dateAdded }?.take(MAX_FOLLOWERS)
                                    if(!authorsList.isNullOrEmpty()){
                                        followDb.authorDao().insertAuthor(*authorsList.toTypedArray())
                                        for (authorData in authorsList) {
                                            val isFollowing = followDb.followDao()
                                                .isFollowedAuthor(authorData.authorId)
                                            if (isFollowing != null) {
                                                isFollowing.isAuthorMetaDataAvailable = AUTHOR_META_DATA_EXIST
                                                followDb.followDao().updateFollowing(isFollowing)
                                            }else{
                                                val follow = FollowEntity(authorData.authorId, authorData.dateAdded, true, SYNCED, AUTHOR_META_DATA_EXIST)
                                                followDb.followDao().setFollowing(follow)
                                            }
                                        }
                                    }
                                } else {
                                    followProvider.logError(response.code(), response.message(), "Follow Authors List Error.", null, null)
                                }
                            }
                            if (isActive) {
                                followDb.followDao().enforceFollowLimit(MAX_FOLLOWERS)
                                followDb.authorDao().enforceAuthorLimit(MAX_AUTHORS)
                            }

                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error syncing authors from remote")
                    followProvider.logException(e)
                    followProvider.logError(-1, e.message, "Follow Authors List Exception.", null, e)
                }
            }
        }
    }

    @WorkerThread
    suspend fun updateAuthor(authorEntity: FollowEntity) {
        try {
            fetchAuthor(authorEntity.authorId).apply {
                if (id != null && name != null) {
                    followDb.authorDao().insertAuthor(AuthorEntity(id,
                            name, bio, expertise, image, lmt, System.currentTimeMillis()))
                    authorEntity.isAuthorMetaDataAvailable = AUTHOR_META_DATA_EXIST
                    followDb.followDao().updateFollowing(authorEntity)
                } else {
                    throw IllegalStateException("Author id and name must not be null source=$this")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating author $authorEntity", e)
            followProvider.logException(e)
            followProvider.logError(-1, e.message, "Follow Fetch Author Exception.", null, e)
        }
    }

    @WorkerThread
    suspend fun fetchAuthor(authorId: String, pageSize: Int = PAGE_SIZE, from: Int = 0): AuthorItem =
            suspendCancellableCoroutine { cont ->
                val requestUrl = followProvider.authorFollowUrl?.format(Locale.getDefault(), authorId,
                        pageSize, from)
                val authorRequest = AuthorRequest(requestUrl,
                        Response.Listener {
                            if (cont.isActive) {
                                cont.resume(it)
                            }
                        },
                        Response.ErrorListener {
                            if (cont.isActive) {
                                cont.resumeWithException(it)
                                val data = hashMapOf("authorId" to authorId)
                                followProvider.logError(-1, it.message, "Follow Fetch Author Error.", data, it)
                            }
                        })
                authorRequest.retryPolicy = retryPolicy
                followProvider.requestQueue.add(authorRequest)
            }

    /*** This is called when user logout
     *   We are clearing both follow and author tables
     */
    fun onLogout() {
        GlobalScope.launch {
            followDb.followDao().clearMetaData()
            followDb.authorDao().clearMetaData()
        }

    }

    companion object {
        @Volatile
        private var INSTANCE: FollowManager? = null
        private val TAG: String = FollowManager::class.java.simpleName
        const val MAX_FOLLOWERS = 25
        const val MAX_AUTHORS = 25
        const val PAGE_SIZE = 10
        const val SYNCED = 1
        const val AUTHOR_META_DATA_EXIST =1
        const val FOLLOW_AUTHOR = 1
        const val UNFOLLOW_AUTHOR = 0
        const val NOT_SYNCED = 0

        @JvmStatic
        fun getInstance(application: Application): FollowManager =
                INSTANCE
                        ?: synchronized(this) {
                            INSTANCE
                                    ?: FollowManager(application).also {
                                        INSTANCE = it
                                    }
                        }
    }
}