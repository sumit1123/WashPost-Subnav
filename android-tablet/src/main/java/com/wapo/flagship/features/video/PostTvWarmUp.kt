package com.wapo.flagship.features.video

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.wapo.android.commons.util.Logger
import androidx.annotation.MainThread
import com.wapo.flagship.features.articles2.models.deserialized.video.Host
import com.wapo.flagship.features.articles2.models.deserialized.video.Video
import com.wapo.flagship.features.posttv.ExoPlayerCache
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
import java.lang.ref.WeakReference
import java.util.*

private val TAG = PostTvWarmUp::class.java.simpleName

/**
 * A helper class that initializes PstTv players by a given name and stores them in [PostTvPlayer2Coordinator].
 * The main use-case of this class is to initialize a player in advance before the video container
 * view appears on the screen
 */
class PostTvWarmUp(
    val videoActivityViewModel: VideoActivityViewModel,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val warmUpQueue = LinkedList<WarmUpItem>()

    /**
     * Search for all suitable article items that contain videos and initialize
     * a new player of each
     */
    @MainThread
    fun createPostTvPlayersForArticle(
        articleId: String,
        videoItems: List<Video>?,
        activity: Activity,
    ) {
        videoItems?.filter { it.host != Host.YOUTUBE && it.host != Host.VIMEO }?.forEach { item ->
            item.id?.let { id ->
                warmUpQueue.add(WarmUpItem(articleId, id, item.streamURL, WeakReference(activity)))
            }
        }
        if (warmUpQueue.isNotEmpty()) {
            handler.post(WarmUpRunnable())
        }
    }

    inner class WarmUpRunnable : Runnable {
        override fun run() {
            Logger.d(TAG, "PostTv warm up queue size is: ${warmUpQueue.size}.")
            val workItem = warmUpQueue.poll()
            if (workItem != null) {
                val activity = workItem.activity.get()
                if (activity != null && !activity.isFinishing) {
                    // It reduces initial stream loading when it is cached.
                    workItem.streamUrl?.let { ExoPlayerCache.cacheVideo(it) }
                    PostTvPlayer2Coordinator.getOrCreatePlayer(workItem.name, activity)
                    // Send an event to update the UI.
                    // Cases to cover where videos is already visible and eligible to autoplay.
                    videoActivityViewModel.dispatchVideosLoadedEvent(
                        workItem.articleId,
                        workItem.name,
                    )
                }
            }
            if (warmUpQueue.isNotEmpty()) {
                handler.post(WarmUpRunnable())
            } else {
                Logger.d(TAG, "PostTv warm up queue is empty. Finishing.")
            }
        }
    }
}

class WarmUpItem(
    val articleId: String,
    val name: String,
    val streamUrl: String?,
    val activity: WeakReference<Activity>,
)
