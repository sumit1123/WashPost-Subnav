package com.washingtonpost.android.follow.helper

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelStoreOwner

interface AuthorProvider {
    fun getActivityViewModelOwner(): ViewModelStoreOwner

    fun getSupportFragmentManager(): FragmentManager

    fun getRootView(): CoordinatorLayout
}