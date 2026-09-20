package com.washingtonpost.android.follow.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.washingtonpost.android.follow.model.AuthorItem

private const val AUTHOR = "AuthorBottomSheetDialogViewModel.AUTHOR"
private const val LOW_DATA_MODE_STATE = "AuthorBottomSheetDialogViewModel.LOW_DATA_MODE_STATE"

class AuthorBottomSheetDialogViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val author: LiveData<AuthorItem?> = savedState.getLiveData(AUTHOR)
    val isLowDataModeEnable: LiveData<Boolean?> = savedState.getLiveData(LOW_DATA_MODE_STATE)

    @UiThread
    fun setAuthorItem(authorItem: AuthorItem) {
        savedState.set(AUTHOR, authorItem)
    }

    @UiThread
    fun setLowDataModeState(isLowDataModeEnable: Boolean) {
        savedState.set(LOW_DATA_MODE_STATE, isLowDataModeEnable)
    }
}
