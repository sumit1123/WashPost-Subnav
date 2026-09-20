// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.aixp.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.aixp.R
import com.wapo.flagship.features.aixp.databinding.FragmentSummaryBinding
import com.wapo.flagship.features.aixp.databinding.SummaryItemRowBinding
import com.wapo.flagship.features.aixp.states.ArticleSummaryState
import com.wapo.flagship.features.aixp.viewmodels.ArticleSummaryCollaborationViewModel
import com.wapo.flagship.features.aixp.viewmodels.FeedbackCollaborationViewModel
import com.wapo.fragment.BaseBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

const val TAG = "SummaryFragment"

@AndroidEntryPoint
class SummaryFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private lateinit var binding: FragmentSummaryBinding

    private val feedbackCollaborationViewModel: FeedbackCollaborationViewModel by activityViewModels()

    private val summaryCollaborationViewModel: ArticleSummaryCollaborationViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.link.setOnClickListener(this@SummaryFragment)
        binding.retryButton.setOnClickListener(this)
        binding.dismissBtn.setOnClickListener(this)
        observeForYouSummary()
        observeRealtimeSummary()
        summaryCollaborationViewModel.apply {
            if (!isForYouSummary() && !isRealtimeSummary()) {
                // Fetch realtime summary when no summary is available
                summaryCollaborationViewModel.startLoadingSummary()
            }
        }
    }

    private fun observeForYouSummary() {
        summaryCollaborationViewModel.forYouSummary.observe(viewLifecycleOwner) {
            it ?: return@observe
            updateTitle(it.title)
            updateDisclaimer(it.disclaimer)
            updateOverview(it.overview)
            updateKeyPointsAndHeading(it.keyPoints, it.keyPointsHeading)
            updateLink()
            binding.dismissBtn.visibility = View.VISIBLE
        }
    }

    private fun observeRealtimeSummary() {
        summaryCollaborationViewModel.articleSummaryState.observe(viewLifecycleOwner) {
            when (it) {
                is ArticleSummaryState.Loading -> {
                    binding.progressIndicator.visibility = View.VISIBLE
                    binding.errorContainer.visibility = View.GONE
                    binding.title.visibility = View.GONE
                    binding.disclaimer.visibility = View.GONE
                    binding.keyPointsHeadline.visibility = View.GONE
                    binding.overview.visibility = View.GONE
                    binding.list.visibility = View.GONE
                    binding.link.visibility = View.GONE
                    binding.dismissBtn.visibility = View.GONE
                }

                is ArticleSummaryState.Success -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.errorContainer.visibility = View.GONE
                    updateTitle(it.summary.title)
                    updateDisclaimer(it.summary.disclaimer)
                    updateOverview(it.summary.overview)
                    updateKeyPointsAndHeading(it.summary.keyPoints, it.summary.keyPointsHeading)
                    updateLink()
                    binding.dismissBtn.visibility = View.VISIBLE
                }

                is ArticleSummaryState.Failure -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.errorContainer.visibility = View.VISIBLE
                    showErrorMessage(getString(R.string.summary_error_message))
                }

                else -> {
                    binding.progressIndicator.visibility = View.GONE
                }
            }
        }
    }

    private fun showErrorMessage(message: String?) {
        if (!message.isNullOrEmpty()) {
            binding.errorMessage.text = message
            binding.errorContainer.visibility = View.VISIBLE
        } else {
            binding.errorContainer.visibility = View.GONE
        }
    }

    private fun updateTitle(title: String?) {
        binding.title.apply {
            text = title ?: getString(R.string.summary_title)
            visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateDisclaimer(disclaimer: String?) {
        binding.disclaimer.apply {
            text = disclaimer
                ?: if (summaryCollaborationViewModel.isForYouSummary()) {
                    getString(R.string.approved_summary_disclaimer)
                } else {
                    getString(R.string.realtime_summary_disclaimer)
                }
            visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateOverview(summary: String?) {
        binding.overview.apply {
            text = summary
            visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateKeyPointsAndHeading(
        keyPoints: List<String?>?,
        heading: String?,
    ) {
        binding.keyPointsHeadline.apply {
            text = heading
            visibility = if (heading.isNullOrEmpty() || keyPoints.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        binding.list.apply {
            visibility = if (keyPoints.isNullOrEmpty()) View.GONE else View.VISIBLE
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            val spaceBetweenItems =
                requireContext().resources.getDimensionPixelSize(R.dimen.summary_dimen_small)
            addItemDecoration(DividerItemDecoration(spaceBetweenItems))
            adapter = ItemsAdapter(keyPoints)
        }
    }

    private fun updateLink() {
        binding.link.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setVisible(summaryCollaborationViewModel.shouldEnableFeedbackLink())
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.link -> {
                dismiss()
                feedbackCollaborationViewModel.dispatchFeedbackLinkClickEvent(FeedbackFragment.FeedbackType.ARTICLE_SUMMARIES)
            }
            R.id.retry_button -> {
                summaryCollaborationViewModel.startLoadingSummary()
            }
            R.id.dismiss_btn -> {
                dismiss()
            }
        }
    }
}

private class ItemsAdapter(
    val items: List<String?>?,
) : RecyclerView.Adapter<ItemViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val view = SummaryItemRowBinding.inflate(LayoutInflater.from((parent.context)))
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = items?.size ?: 0

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
    ) {
        holder.itemRowBinding.bullet.visibility = if (itemCount <= 1) View.GONE else View.VISIBLE
        holder.itemRowBinding.item.text = items?.get(position)
    }
}

private class ItemViewHolder(
    val itemRowBinding: SummaryItemRowBinding,
) : RecyclerView.ViewHolder(itemRowBinding.root)

class DividerItemDecoration(
    private val verticalSpaceHeight: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.bottom = verticalSpaceHeight
    }
}
