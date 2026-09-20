package com.wapo.flagship.features.feedback.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wapo.flagship.features.feedback.R
import com.wapo.flagship.features.feedback.databinding.FragmentFeedbackStatusBinding
import com.wapo.fragment.BaseBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackStatusFragment : BaseBottomSheetDialogFragment() {

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

    }
}