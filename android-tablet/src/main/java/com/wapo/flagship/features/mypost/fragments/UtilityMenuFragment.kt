package com.wapo.flagship.features.mypost.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.ContentType
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wapo.view.RippleHelper
import com.washingtonpost.android.R
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.android.save.databinding.FragmentMyPostUtilityMenuBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.types.MyPostSection

class UtilityMenuFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private lateinit var binding: FragmentMyPostUtilityMenuBinding

    private val myPost2ViewModel: MyPost2ViewModel by activityViewModels()
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)

    companion object {
        val tag: String = UtilityMenuFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyPostUtilityMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            // cases where activity is being recreated and view model also lost its state.
            dismiss()
            return
        }
        binding.buttonAddToSavedStories.setOnClickListener(this)
        binding.buttonRemoveFromSavedStories.setOnClickListener(this)
//        binding.buttonRemoveFromHistory.setOnClickListener(this)
        binding.buttonShare.setOnClickListener(this)

        // remove bottom divider for the last item
        binding.buttonShare.background = null

        RippleHelper.addRippleEffectToView(binding.buttonAddToSavedStories)
        RippleHelper.addRippleEffectToView(binding.buttonRemoveFromSavedStories)
//        RippleHelper.addRippleEffectToView(binding.buttonRemoveFromHistory)
        RippleHelper.addRippleEffectToView(binding.buttonShare)

        myPost2ViewModel.liveArticleByUrl.observe(viewLifecycleOwner) {
            updateBookmarkItems(it != null, myPost2ViewModel.optionsClickEvent.value?.section)
        }

//        if (isListOfType(MyPostSection.READING_HISTORY)) {
//            binding.buttonRemoveFromHistory.visibility = View.VISIBLE
//        } else {
//            binding.buttonRemoveFromHistory.visibility = View.GONE
//        }
        Measurement.trackMyPostMenuOpenEvent(
            myPost2ViewModel.section.value,
            myPost2ViewModel.optionsClickEvent.value?.url,
        )
    }

    override fun onClick(view: View?) {
        val actionItem = myPost2ViewModel.optionsClickEvent.value ?: return
        val section = myPost2ViewModel.section.value
        when (view?.id) {
            R.id.button_add_to_saved_stories -> {
                if (myPost2ViewModel.isLoggedInUserOrSubscriber()) {
                    performAddToSavedStories(actionItem)
                    Measurement.trackMyPostMenuAddArticleEvent(section, actionItem.url)
                } else {
                    // if user is neither logged in nor subscribed, show Save Regwall
                    (activity as? BaseActivity)?.getPaywallSheetHelper()?.showWall(
                        wallName = PaywallConstants.WALL_NAME_SAVE_REGWALL,
                        wallType = PaywallConstants.WallType.SAVE_REGWALL,
                        wallReason = PaywallConstants.WallType.SAVE_REGWALL.ordinal,
                    )
                }
            }
            R.id.button_remove_from_saved_stories -> {
                val action =
                    ArticleActionItem(
                        MyPostSection.SAVED_STORIES,
                        actionItem.url,
                        false,
                        null,
                    )
                myPost2ViewModel.handleSaveClickEvent(action)
            }
            // todo temporarily removing "remove from history" button until backend creates delete endpoint
//            R.id.button_remove_from_history -> {
//                val action =
//                    ArticleActionItem(
//                        MyPostSection.READING_HISTORY,
//                        actionItem.url,
//                        false,
//                        null,
//                    )
//                myPost2ViewModel.handleSaveClickEvent(action)
//            }
            R.id.button_share -> {
                performShare(actionItem)
                Measurement.trackMyPostMenuShareEvent(section, actionItem.url)
            }
        }
        dismiss()
    }

    private fun updateBookmarkItems(isBookmarked: Boolean, section: MyPostSection?) {
        if (section == MyPostSection.PURCHASE){
            binding.buttonAddToSavedStories.visibility = View.GONE
            binding.buttonRemoveFromSavedStories.visibility = View.GONE
        }else if (isBookmarked) {
            binding.buttonAddToSavedStories.visibility = View.GONE
            binding.buttonRemoveFromSavedStories.visibility = View.VISIBLE
        } else {
            binding.buttonRemoveFromSavedStories.visibility = View.GONE
            binding.buttonAddToSavedStories.visibility = View.VISIBLE
        }
    }

    private fun isListOfType(section: MyPostSection): Boolean {
        val actionItem = myPost2ViewModel.optionsClickEvent.value
        return actionItem?.section == section
    }

    private fun getArticleItem(actionItem: ArticleActionItem): MyPostArticleItem? =
        when (actionItem.section) {
            MyPostSection.FOLLOWING -> {
                // See if item is available in the preview item otherwise menu is triggered from other places.
                var item =
                    myPost2ViewModel
                        .getPreviewListBySection(actionItem.section)
                        ?.firstOrNull { it.contentUrl == actionItem.url }
                followViewModel.utilityMenuClickEvent.value?.let {
                    if (item == null) {
                        // Construct from ArticleItem
                        item =
                            MyPostArticleItem(
                                contentUrl = it.url,
                                headline = it.headline,
                                headlinePrefix = null,
                                blurb = it.blurb,
                                kicker = it.storyType,
                                transparency = null,
                                imageUrl = it.image,
                                byline = it.byline,
                                dateTime = it.lmt,
                                displayDate = it.displayDate,
                                contentType = ContentType.ARTICLE
                            )
                    }
                }
                listOf(item)
            }
            else -> myPost2ViewModel.getPreviewListBySection(actionItem.section)
        }?.firstOrNull { it?.contentUrl == actionItem.url }

    private fun performAddToSavedStories(actionItem: ArticleActionItem) {
        getArticleItem(actionItem)
            ?.run {
                val savedArticleModel =
                    SavedArticleModel(
                        contentUrl,
                        System.currentTimeMillis()
                    )
                val metadataModel =
                    MetadataModel(
                        contentUrl,
                        System.currentTimeMillis()
                    ).also {
                        it.imageURL = imageUrl
                        it.headline = headline
                        it.blurb = blurb
                        it.byline = byline
                        it.publishedTime = dateTime
                    }
                myPost2ViewModel.saveArticle(savedArticleModel, metadataModel)
            }
    }

    private fun performShare(actionItem: ArticleActionItem) {
        getArticleItem(actionItem)
            ?.run {
                Share
                    .Builder()
                    .headline(headline)
                    .byline(byline)
                    .shareUrl(contentUrl)
                    .build()
                    .shareItem(requireActivity())
            }
    }
}
