package com.wapo.view

/**
 * A hacky interface that allows child views communicate with its parent and block parent's scroll
 * in the case when [ViewParent.requestDisallowInterceptTouchEvent] doesn't work properly
 */
interface ParentScroll {
    fun setShouldAllowScroll(shouldAllowScroll: Boolean)
}