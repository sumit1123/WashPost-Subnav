package com.wapo.flagship.features.audio

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import android.widget.FrameLayout
import com.wapo.flagship.features.personalizedpodcasts.events.UserEvent
import com.wapo.view.habittiles.ArticleSource

interface AudioActivity {
    fun addFragment(@IdRes viewID: Int, fragment: Fragment)

    fun removeFragment(fragment: Fragment?)

    fun getPersistentPlayerFrame(): FrameLayout?

    fun removePersistentPlayerFragment()

    fun onAudioStarted()

    fun updatePlayerFragment()

    fun trackAddToPlayListTapEvents(arcId : String)

    fun onAskSamEvent(userEvent: UserEvent)

    fun showSources(sources: List<ArticleSource>)
}