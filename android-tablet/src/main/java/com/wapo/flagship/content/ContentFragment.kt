package com.wapo.flagship.content

import android.content.Context
import androidx.fragment.app.Fragment
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

open class ContentFragment : Fragment() {
    private var contentActivity: ContentActivity? = null
    var compositeSubscription: CompositeSubscription? = null
    var updaterSubject: BehaviorSubject<Long>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contentActivity = activity as? ContentActivity ?: throw IllegalArgumentException(
            "an activity of interface ${ContentActivity::class.java.name} is expected",
        )
    }

    override fun onDetach() {
        super.onDetach()
        contentActivity = null
    }

    override fun onStart() {
        super.onStart()
        compositeSubscription = CompositeSubscription()
        updaterSubject = BehaviorSubject.create()
    }

    override fun onStop() {
        super.onStop()
        val hasCompleted = updaterSubject?.hasCompleted() ?: true
        if (!hasCompleted) {
            updaterSubject?.onCompleted()
        }
        compositeSubscription?.unsubscribe()
        updaterSubject = null
        compositeSubscription = null
    }

    fun getContentManagerObs(): Observable<ContentManager> {
        val activity = contentActivity ?: return Observable.empty<ContentManager>()
        return activity.getContentManagerObs()
    }
}
