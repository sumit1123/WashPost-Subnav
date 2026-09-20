/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import android.app.Activity
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton class to manage a pool of [PostTvPlayer2Manager] objects.
 * This class creates and holds [PostTvPlayer2Manager] objects. It maintains a concurrent HashMap for it.
 * Activities/Fragments/UI classes can request for a manager by passing a unique player name.
 * LifeCycleObserver class [PostTvPlayer2ActivityLifecycleObserver] calls a couple of life cycle methods
 * there in this class (resumePlayers(), pausePlayers() & releasePlayers()) to maintain [PostTvPlayer2Manager] states.
 * If any class has specific requirements to maintain [PostTvPlayer2Manager] class has its own, it can be done easily without
 * using this class by creating and interacting directly with the [PostTvPlayer2Manager] class.
 */
object PostTvPlayer2Coordinator {

    /**
     * HashMap to maintain a pool of [PostTvPlayer2Manager] objects.
     */
    private val playerPool = ConcurrentHashMap<String, PostTvPlayer2Manager>()

    /**
     * Returns [PostTvPlayer2Manager] object if one is already available in the map.
     * Otherwise it creates a new object and puts it in the map with the given key (playerName).
     * @param playerName key name to put it in the map
     * @param activity for the player's state management based on the activities life cycle methods.
     */
    fun getOrCreatePlayer(playerName: String, activity: Activity,
                          fetchPercentage: Float? = null,
                          onPlayerClicked: (() -> Unit)? = null,
                          isPlayerClickable: Boolean = false,
                          ): PostTvPlayer2Manager {
        // Not allowing more than one activity to hold the same player as releasePlayers() releases
        // the player when one of the activities is destroyed and other activities will fail to get the original player.
        // There is also a lifecycle behavior difference on older versions that [Lifecycle.Event.ON_DESTROY] gets triggered
        // lately by then the user can open the same activity and [Lifecycle.Event.ON_RESUME] is called already.
        if (playerPool[playerName] != null && playerPool[playerName]?.isOwnedBy(activity) == false) {
            playerPool[playerName]?.releasePlayer()
            playerPool.remove(playerName)
        }
        return playerPool[playerName]
            ?: PostTvPlayer2Manager().also { manager ->
                playerPool[playerName] = manager
                manager.initPlayer(activity, fetchPercentage = fetchPercentage, isPlayerClickable = isPlayerClickable, onPlayerClicked = onPlayerClicked)
                manager.setPlayerName(playerName)
            }
    }

    /**
     * Returns [PostTvPlayer2Manager] object if one is already available in the map.
     * Otherwise throws an error.
     * Activities can initialize by calling a [getOrCreatePlayer] method. Then non activity classes
     * (for ex: view holders, fragments etc.,) can interact with the same manager by using this method.
     * All classes who are calling this should make sure that the manager is already created by
     * the corresponding parent activities.
     */
    fun getPlayer(playerName: String): PostTvPlayer2Manager {
        if (playerName.isEmpty()) {
            throw IllegalArgumentException("playerName value is empty")
        }
        return playerPool[playerName] ?: throw IllegalArgumentException("No player exists with the given name=$playerName")
    }

    /**
     * [PostTvPlayer2ActivityLifecycleObserver] uses this method to resume all the players that are owned
     * by the corresponding activity while resuming the activity.
     */
    fun resumePlayers(activity: Activity) {
        playerPool.forEach { entry -> if (entry.value.isOwnedBy(activity)) entry.value.resumeMedia() }
    }

    /**
     * [PostTvPlayer2ActivityLifecycleObserver] uses this method to pause all the players that are owned
     * by the corresponding activity while pausing the activity.
     */
    fun pausePlayers(activity: Activity) {
        playerPool.forEach { entry -> if (entry.value.isOwnedBy(activity)) entry.value.pauseMedia() }
    }

    /**
     * [PostTvPlayer2ActivityLifecycleObserver] uses this method to release all the players that are owned
     * by the corresponding activity while destroying the activity.
     */
    fun releasePlayers(activity: Activity) {
        playerPool.forEach { entry ->
            if (entry.value.isOwnedBy(activity)) {
                entry.value.releasePlayerFrame()
                entry.value.releasePlayer()
                playerPool.remove(entry.key)
            }
        }
    }

    /**
     * Helper Method to know whether a player exists in the pool with the given name.
     * @param playerName
     * @return true if player exists otherwise false
     */
    fun doesPlayerExist(playerName: String): Boolean {
        return playerPool[playerName] != null
    }
}