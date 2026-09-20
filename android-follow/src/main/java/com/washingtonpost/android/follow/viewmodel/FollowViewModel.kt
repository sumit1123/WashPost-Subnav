package com.washingtonpost.android.follow.viewmodel

import com.wapo.android.commons.util.Logger
import androidx.annotation.Keep
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.room.withTransaction
import com.wapo.android.commons.util.LiveEvent
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.database.model.FollowEntity
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.model.ArticleItem
import com.washingtonpost.android.follow.model.AuthorItem
import kotlinx.coroutines.launch

class FollowViewModel @Keep constructor(val followManager: FollowManager, private val savedState: SavedStateHandle) : ViewModel() {
    private val followDb = followManager.followDb
    val followedAuthors: LiveData<PagedList<AuthorEntity>> = followDb.followDao().followingByLmt().toLiveData(pageSize = 10)
    val numFollowing = followDb.followDao().getNumFollowing()
    val authorPosition: LiveData<Int?> = savedState.getLiveData(AUTHOR_POSITION)
    private var _deepLinkedAuthorId: MutableLiveData<String?> = MutableLiveData()
    val deepLinkedAuthorId: LiveData<String?> = _deepLinkedAuthorId

    private val _utilityMenuClickEvent = LiveEvent<ArticleItem?>()
    val utilityMenuClickEvent: LiveData<ArticleItem?> = _utilityMenuClickEvent

    val isLoggedIn: Boolean
        get() = followManager.followProvider.isLoggedInUser()

    fun isFollowing(authorItem: AuthorItem): LiveData<FollowEntity?> {
        return isFollowing(authorItem.id)
    }

    fun isFollowing(authorId: String?) =
        followDb.followDao().isFollowing(authorId)

    fun setFollowing(isFollowing: Boolean, authorItem: AuthorItem, callback: ((reachedMaxFollow:Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                if (authorItem.id != null && authorItem.name != null) {
                    val authorEntity = AuthorEntity(authorItem.id, authorItem.name, authorItem.bio,
                            authorItem.expertise, authorItem.image, authorItem.lmt, System.currentTimeMillis())
                    val followEntity = FollowEntity(authorItem.id, System.currentTimeMillis(), isFollowing, 0)
                    var reachedMax = false
                    followDb.withTransaction {
                        if (isFollowing) {
                            val numFollowing = followDb.followDao().getNumFollowingSync()
                            reachedMax = numFollowing >= MAX_FOLLOW
                            if (!reachedMax) {
                                val followAuthor =
                                    followDb.followDao().isFollowedAuthor(authorItem.id)
                                if (followAuthor != null) {
                                    followDb.followDao().updateFollowing(followEntity)
                                } else {
                                    followDb.authorDao().insertAuthor(authorEntity)
                                    followDb.followDao().setFollowing(followEntity)
                                }
                            }else{
                                //This block is executed when the user follow author after the MAX_FOLLow limit is reached
                                //In this scenario we don't block the user but will remove the author from the local db to cap the app limit to 25
                                followDb.followDao().enforceFollowLimit(1)
                                followDb.authorDao().removeAuthorForUnFollow()
                                //Insert the new author to author entity and set following in Follow entity
                                followDb.authorDao().insertAuthor(authorEntity)
                                followDb.followDao().setFollowing(followEntity)

                            }
                            // Follow sync to remote
                            followManager.followAuthor()
                            reachedMax = false
                        } else {
                            followDb.followDao().updateFollowing(followEntity)
                            //Un-following author sync to remote
                            followManager.unFollowAuthor()
                        }
                    }
                    if (callback != null) {
                        callback(reachedMax)
                    }
                    followManager.updateAuthor(followEntity)
                    FollowTrackingInfo.authors = followDb.followDao().getFollowedAuthors()
                } else {
                    throw IllegalStateException("Author id and name must not be null source=$authorItem")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error setting follow state", e)
                followManager.followProvider.logException(e)
                val data = hashMapOf("isFollowing" to isFollowing)
                followManager.followProvider.logError(-1, e.message, "Follow setFollowing Exception.", data, e)
            }
        }
    }

    fun setPosition(authorId: String) {
        viewModelScope.launch {
            try {
                savedState.set(AUTHOR_POSITION, followDb.followDao().getFollowPosition(authorId))
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set position", e)
                followManager.followProvider.logException(e)
            }
        }
    }

    fun setDeepLinkedAuthorId(authorId: String?) {
        _deepLinkedAuthorId.value = authorId
    }

    fun handleUtilityMenuClickEvent(articleItem: ArticleItem?) {
        _utilityMenuClickEvent.value = articleItem
    }

    /***
     * Increasing the limit for Follow author to 200 {Might want to discuss with team to finalize the limit}
     */
    companion object {
        private val TAG: String = FollowViewModel::class.java.simpleName
        private const val AUTHOR_POSITION = "FollowViewModel.AUTHOR_POSITION"
        private const val MAX_FOLLOW = 25
    }
}