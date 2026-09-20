/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wapo.flagship.features.aixp.R
import com.wapo.flagship.features.aixp.databinding.FragmentFeedbackStatusBinding
import com.wapo.flagship.features.aixp.ui.FeedbackFragment.FeedbackType
import com.wapo.fragment.BaseBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackStatusFragment(private val feedbackType: FeedbackType = FeedbackType.ARTICLE_SUMMARIES) : BaseBottomSheetDialogFragment() {

    private lateinit var binding: FragmentFeedbackStatusBinding

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (feedbackType == FeedbackType.ARTICLE_SUMMARIES) {
            SummaryFragment().show(parentFragmentManager, "summary_fragment")
        }
    }
}