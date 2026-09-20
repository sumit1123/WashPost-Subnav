package com.washingtonpost.android.follow.helper

import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import com.washingtonpost.android.follow.fragment.AuthorBottomSheetDialogFragment
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.follow.viewmodel.AuthorBottomSheetDialogViewModel
import java.lang.ref.WeakReference

class AuthorHelper(private val authorProvider: AuthorProvider) {
    private val authorViewModel: AuthorBottomSheetDialogViewModel
    private var authorBottomSheetFragment: WeakReference<AuthorBottomSheetDialogFragment>? = null

    init {
        val viewModelProvider = ViewModelProvider(authorProvider.getActivityViewModelOwner())
        authorViewModel = viewModelProvider.get(AuthorBottomSheetDialogViewModel::class.java)
        authorBottomSheetFragment = WeakReference(AuthorBottomSheetDialogFragment.newInstance())
    }

    @UiThread
    fun displayAuthorBottomSheet(authorItem: AuthorItem, isLowDataModeEnable: Boolean) {
        authorViewModel.setAuthorItem(authorItem)
        authorViewModel.setLowDataModeState(isLowDataModeEnable)

        if (authorBottomSheetFragment?.get() == null) {
            authorBottomSheetFragment = WeakReference(AuthorBottomSheetDialogFragment.newInstance())
        }
        if (authorBottomSheetFragment?.get()?.isAdded == false) {
            authorBottomSheetFragment?.get()?.show(authorProvider.getSupportFragmentManager(), "")
        }
    }

    @UiThread
    fun dismissAuthorBottomSheet() {
        if (authorBottomSheetFragment?.get()?.isAdded == true) {
            authorBottomSheetFragment?.get()?.dismissAllowingStateLoss()
        }
    }
}