/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.aixp.R
import com.wapo.flagship.features.aixp.databinding.FragmentFeedbackBinding
import com.wapo.flagship.features.aixp.states.FeedbackSubmissionState
import com.wapo.flagship.features.aixp.viewmodels.ArticleSummaryCollaborationViewModel
import com.wapo.flagship.features.aixp.viewmodels.FeedbackCollaborationViewModel
import com.wapo.fragment.BaseBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FeedbackFragment(
    private val feedbackType: FeedbackType = FeedbackType.ARTICLE_SUMMARIES,
    private val endpoint: String? = null, private val responseId: String? = null,
    private val presetReaction: Int? = null
) : BaseBottomSheetDialogFragment(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener {

    private lateinit var binding: FragmentFeedbackBinding

    private val feedbackCollaborationViewModel: FeedbackCollaborationViewModel by activityViewModels()

    private val summaryCollaborationViewModel: ArticleSummaryCollaborationViewModel? by activityViewModels()

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Update radioGroup buttons with default state.
        if (savedInstanceState == null) onCheckedChanged(0)
        binding.submitButton.setOnClickListener(this)
        binding.radioGroup.setOnCheckedChangeListener(this)
        presetReaction?.let {
            binding.detailsContainer.visibility = View.VISIBLE
            binding.radioGroup.check(binding.radioGroup.getChildAt(presetReaction).id)
        }
        if (feedbackType != FeedbackType.ARTICLE_SUMMARIES) {
            binding.description.text = getString(R.string.feedback_conversation)
        } else {
            binding.description.text = getString(com.wapo.view.R.string.ai_overview_feedback_title)

        }
        observeFeedbackSubmissionState()
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.submit_button -> {
                if (feedbackType == FeedbackType.ARTICLE_SUMMARIES) {
                    feedbackCollaborationViewModel.submitArticleSummaryFeedback(
                        summaryCollaborationViewModel?.getArticleId(),
                        summaryCollaborationViewModel?.getCurrentSummary(),
                        getRating(),
                        getFeedback(),
                        summaryCollaborationViewModel?.isRealtimeSummary()
                    )
                } else if (feedbackType == FeedbackType.POST_ANSWERS || feedbackType == FeedbackType.ASK_THE_POST_ARTICLE || feedbackType == FeedbackType.TALK_TO_THE_POST) {
                    feedbackCollaborationViewModel.submitPostAnswersFeedback(
                        endpoint,
                        responseId,
                        getRating(),
                        getFeedback()
                    )
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (feedbackType == FeedbackType.ARTICLE_SUMMARIES) {
            SummaryFragment().show(parentFragmentManager, "summary_fragment")
        }
    }

    private fun getRating(): Int {
        return if (binding.yes.isChecked) 1
        else if (binding.no.isChecked) 2
        else -1
    }

    private fun getFeedback(): String? {
        return binding.inputFeedback.text?.trim().toString()
    }

    private fun observeFeedbackSubmissionState() {
        feedbackCollaborationViewModel.resetFeedbackSubmissionState()
        feedbackCollaborationViewModel.feedbackSubmissionState.observe(viewLifecycleOwner) {
            Logger.d("FeedbackFragment", "observeSummaryFeedbackState, it=$it")
            handleFeedbackState(it)
        }
    }

    private fun handleFeedbackState(state: FeedbackSubmissionState?) {
        state ?: return
        when (state) {
            is FeedbackSubmissionState.Loading -> {
                this@FeedbackFragment.isCancelable = false
                binding.radioGroup.apply {
                    isEnabled = false
                    children.forEach { it.isEnabled = isEnabled }
                }
                binding.inputFeedback.isEnabled = false
                binding.submitButton.isEnabled = false
                binding.progressIndicator.apply {
                    visibility = View.VISIBLE
                    bringToFront()
                }
            }

            is FeedbackSubmissionState.Success -> {
                this@FeedbackFragment.isCancelable = true
                binding.radioGroup.clearCheck()
                binding.inputFeedback.text = null
                binding.radioGroup.apply {
                    isEnabled = true
                    children.forEach { it.isEnabled = isEnabled }
                }
                binding.inputFeedback.isEnabled = true
                binding.submitButton.isEnabled = true
                binding.progressIndicator.visibility = View.GONE
                dismiss()
                feedbackCollaborationViewModel.dispatchFeedbackSubmittedEvent()
            }

            is FeedbackSubmissionState.Failure -> {
                this@FeedbackFragment.isCancelable = true
                binding.radioGroup.apply {
                    isEnabled = true
                    children.forEach { it.isEnabled = isEnabled }
                }
                binding.inputFeedback.isEnabled = true
                binding.submitButton.isEnabled = true
                binding.submitButton.text = getText(R.string.feedback_retry)
                binding.progressIndicator.visibility = View.GONE
            }
        }
    }

    enum class FeedbackType {
        ARTICLE_SUMMARIES,
        POST_ANSWERS,
        ASK_THE_POST_ARTICLE,
        TALK_TO_THE_POST
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        onCheckedChanged(checkedId)
    }

    fun onCheckedChanged(checkedId: Int) {
        when (checkedId) {
            R.id.yes -> {
                binding.detailsDescription.text = getString(R.string.feedback_details_yes_description)
            }
            R.id.no -> {
                binding.detailsDescription.text = getString(R.string.feedback_details_no_description)
            }
        }
        binding.detailsContainer.visibility = View.VISIBLE
    }
}
