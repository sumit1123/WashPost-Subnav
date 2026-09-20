package com.wapo.flagship.lifecycle

import com.wapo.android.commons.util.Logger
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.wapo.flagship.FlagshipApplication

abstract class FlagshipLifecycleObserver(
    protected val lifecycle: Lifecycle,
) : LifecycleObserver {
    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    open fun onApplicationStart() {
        Logger.d(TAG, "${javaClass.name}: onApplicationStart")
        FlagshipApplication.isInForeground = true
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    open fun onApplicationPause() {
        Logger.d(TAG, "${javaClass.name}: onApplicationPause")
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun onApplicationStop() {
        Logger.d(TAG, "${javaClass.name}: onApplicationStop")
        FlagshipApplication.isInForeground = false
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onApplicationDestroy() {
        Logger.d(TAG, "${javaClass.name}: onApplicationDestroy")
        FlagshipApplication.isInForeground = false
    }



    companion object {
        @JvmStatic
        protected val TAG: String = FlagshipLifecycleObserver::class.java.simpleName
    }
}
