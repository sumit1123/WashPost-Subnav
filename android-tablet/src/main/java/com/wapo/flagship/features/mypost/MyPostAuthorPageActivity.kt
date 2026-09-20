package com.wapo.flagship.features.mypost

import android.os.Bundle
import androidx.activity.viewModels
import com.wapo.flagship.features.mypost.fragments.RemoveConfirmationFragment
import com.wapo.flagship.features.mypost.fragments.UtilityMenuFragment
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.washingtonpost.android.follow.activity.AuthorPageActivity
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyPostAuthorPageActivity : AuthorPageActivity() {

    private val myPost2ViewModel: MyPost2ViewModel by viewModels()
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeSaveClickEvent()
        observeFollowOptionsClickEvent()
        observeOptionsClickEvent()
    }

    private fun observeSaveClickEvent() {
        myPost2ViewModel.saveClickEvent.observe(this) {
            RemoveConfirmationFragment().show(
                supportFragmentManager,
                RemoveConfirmationFragment.tag
            )
        }
        myPost2ViewModel.liveUnsavedArticle.observe(this) {
            if (myPost2ViewModel.unsaveArticle.value != null) {
                it?.let {
                    val actionItem = myPost2ViewModel.saveClickEvent.value
                    actionItem?.section?.let { section ->
                        myPost2ViewModel.removeArticleFromList(it)
                        myPost2ViewModel.clearSaveConfirmClickEvent()
                    }
                }
            }
        }
    }

    private fun observeOptionsClickEvent() {
        myPost2ViewModel.optionsClickEvent.observe(this) {
            UtilityMenuFragment().show(supportFragmentManager, UtilityMenuFragment.tag)
        }
    }

    private fun observeFollowOptionsClickEvent() {
        followViewModel.utilityMenuClickEvent.observe(this) {
            myPost2ViewModel.handleFollowOptionsClickEvent(it)
        }
    }
}
