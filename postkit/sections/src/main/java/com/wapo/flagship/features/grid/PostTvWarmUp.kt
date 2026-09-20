package com.wapo.flagship.features.grid

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.wapo.android.commons.util.Logger
import androidx.annotation.MainThread
import com.wapo.flagship.features.grid.model.CarouselVideo
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import java.lang.ref.WeakReference
import java.util.*


private val TAG = PostTvWarmUp::class.java.simpleName

/**
 * A helper class that initializes PstTv players by a given name and stores them in [PostTvPlayer2Coordinator].
 * The main use-case of this class is to initialize a player in advance before the video container
 * view appears on the screen
 */
class PostTvWarmUp {

    private val handler = Handler(Looper.getMainLooper())
    private val warmUpQueue = LinkedList<WarmUpItem>()

    /**
     * Search for all suitable grid items that contain autoplay videos and initialize
     * a new player of each
     */
    @MainThread
    fun createPostTvPlayersForSection(page: Grid, activity: Activity) {
        val carouselVideoItems = page
            .regions
            .flatMap { it.items }
            .flatMap { it.items }
            .flatMap { it.items }
            .filterIsInstance<CarouselVideo>()
        carouselVideoItems.forEach { item ->
            item.id?.let { id ->
                warmUpQueue.add(WarmUpItem(id, WeakReference(activity)))
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
                    PostTvPlayer2Coordinator.getOrCreatePlayer(workItem.name, activity)
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

class WarmUpItem(val name: String, val activity: WeakReference<Activity>)
