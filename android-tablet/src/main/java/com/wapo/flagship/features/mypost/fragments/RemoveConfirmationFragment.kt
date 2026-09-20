package com.wapo.flagship.features.mypost.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.FragmentMyPostRemoveConfirmationBinding
import com.washingtonpost.android.save.types.MyPostSection

class RemoveConfirmationFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentMyPostRemoveConfirmationBinding

    private val myPost2ViewModel: MyPost2ViewModel by activityViewModels()

    companion object {
        val tag: String = UtilityMenuFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyPostRemoveConfirmationBinding.inflate(inflater, container, false)
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
        val actionItem = myPost2ViewModel.saveClickEvent.value
        val currentSection = myPost2ViewModel.section.value
        binding.tvLabel.text =
            when (actionItem?.section) {
                MyPostSection.SAVED_STORIES ->
                    getString(
                        R.string.my_post_remove_confirmation_saved_stories,
                    )
                MyPostSection.READING_HISTORY ->
                    getString(
                        R.string.my_post_remove_confirmation_reading_history,
                    )
                else -> ""
            }
        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener {
            myPost2ViewModel.apply {
                handleSaveConfirmClickEvent()
                when (actionItem?.section) {
                    MyPostSection.SAVED_STORIES -> {
                        if (currentSection != null && actionItem.recipePageName.isEmpty()) {
                            // don't track here in Recipe case
                            Measurement.trackMyPostMenuRemoveArticleEvent(
                                currentSection,
                                actionItem.url,
                            )
                        }
                    }
                    MyPostSection.READING_HISTORY -> {
                        if (currentSection != null) {
                            Measurement.trackMyPostMenuRemoveHistoryArticleEvent(
                                currentSection,
                                actionItem.url,
                            )
                        }
                    }
                    else -> {}
                }
                dismiss()
            }
        }
    }
}
