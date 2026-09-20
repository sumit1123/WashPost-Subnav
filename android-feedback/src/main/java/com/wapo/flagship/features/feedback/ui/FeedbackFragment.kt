package com.wapo.flagship.features.feedback.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import com.wapo.flagship.features.feedback.models.FeedbackProvider
import com.wapo.flagship.features.feedback.state.FeedbackSubmissionState
import com.wapo.flagship.features.feedback.viewmodel.FeedbackViewModel
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.feedback.R
import com.wapo.flagship.features.feedback.databinding.FragmentFeedbackBinding
import com.wapo.fragment.BaseBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

// TODO refactor to compose
@AndroidEntryPoint
class FeedbackFragment(
    private val provider: FeedbackProvider,
    private val presetReaction: Int? = null
) : BaseBottomSheetDialogFragment(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener {

    private lateinit var binding: FragmentFeedbackBinding

    private val feedbackViewModel: FeedbackViewModel by activityViewModels()


    private fun observeFeedbackSubmissionState() {
        feedbackViewModel.resetFeedbackSubmissionState()
        feedbackViewModel.feedbackSubmissionState.observe(viewLifecycleOwner) {
            Logger.d("FeedbackFragment", "observeSummaryFeedbackState, it=$it")
            handleFeedbackState(it)
        }
    }

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
        binding.submitButton.setIsEnabled(false)
        binding.submitButton.setOnClickListener(this)
        binding.radioGroup.setOnCheckedChangeListener(this)
        presetReaction?.let {
            binding.detailsContainer.visibility = View.VISIBLE
            binding.radioGroup.check(binding.radioGroup.getChildAt(presetReaction).id)
        }
        binding.description.text = provider.description()
        observeFeedbackSubmissionState()
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.submit_button -> {
                feedbackViewModel.submitFeedback(
                    provider,
                    getRating(),
                    getFeedback()
                )
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // todo fix for other feedback types
//        if (feedbackType == FeedbackType.ARTICLE_SUMMARIES) {
//            SummaryFragment().show(parentFragmentManager, "summary_fragment")
//        }
    }

    private fun getRating(): Int {
        return if (binding.yes.isChecked) 2
        else if (binding.no.isChecked) 1
        else -1
    }

    private fun getFeedback(): String? {
        return binding.inputFeedback.text?.trim().toString()
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
                binding.submitButton.setIsEnabled(false)
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
                binding.submitButton.setIsEnabled(true)
                binding.progressIndicator.visibility = View.GONE
                dismiss()
                feedbackViewModel.dispatchFeedbackSubmittedEvent()
            }

            is FeedbackSubmissionState.Failure -> {
                this@FeedbackFragment.isCancelable = true
                binding.radioGroup.apply {
                    isEnabled = true
                    children.forEach { it.isEnabled = isEnabled }
                }
                binding.inputFeedback.isEnabled = true
                binding.submitButton.setIsEnabled(true)
                binding.submitButton.text = getText(R.string.feedback_retry)
                binding.progressIndicator.visibility = View.GONE
            }
        }
    }

    fun View.setIsEnabled(isEnabled: Boolean, disabledAlpha: Float = 0.3f) {
        this.isEnabled = isEnabled
        this.alpha = if (isEnabled) 1f else disabledAlpha
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
        binding.submitButton.setIsEnabled(getRating() != -1)
    }
}