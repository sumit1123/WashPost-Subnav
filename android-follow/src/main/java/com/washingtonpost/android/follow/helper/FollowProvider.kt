package com.washingtonpost.android.follow.helper

import android.content.Context
import android.view.View
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.volley.RequestQueue
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader

interface FollowProvider {
    val requestQueue: RequestQueue

    val animatedImageLoader: AnimatedImageLoader

    val authorFollowUrl: String?

    fun logException(t: Throwable)

    fun openArticles(context: Context?, urls: Array<String>, url: String)

    fun onAuthorFollowed(view: View, followId: String?)

    fun onTrackingEvent(trackingEvent: TrackingEvent)

    fun getAuthorImageRequestUrl(url: String?): String?

    fun onAuthorNameClicked(authorItem: AuthorItem)

    fun onMaxFollowReached(context: Context?, author: AuthorItem)

    fun startAuthorPageActivity(context: Context, author: AuthorItem)

    fun isLoggedInUserAndSubscriber(): Boolean

    fun handleSignInOrCreateAccount(activity: Context)

    fun getFollowRequestHeaders() : HashMap<String, String>

    fun getAuthorBaseUrl(): String?

    fun isConnected() : Boolean

    fun isLoggedInUser() : Boolean

    fun logError(errorCode: Int?, errorMessage: String?, message: String?, data: Map<String, Any?>?, t: Throwable?)
}