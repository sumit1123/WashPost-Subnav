package com.wapo.flagship.features.articles2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.wapo.flagship.features.articles2.adapters.TableOfContentsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.viewmodels.ArticleTableOfContentsViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wapo.text.GlobalFontAdjustmentSpan
import com.washingtonpost.android.databinding.FragmentTabletOfContentsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArticleTableOfContentsFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentTabletOfContentsBinding

    private val articleTableOfContentsViewModel: ArticleTableOfContentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTabletOfContentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(requireContext()).apply {
                        addItemDecoration(DividerItemDecoration(requireContext(), orientation))
                    }
                adapter =
                    TableOfContentsRecyclerViewAdapter(
                        {
                            articleTableOfContentsViewModel.getAnchorPos()
                        },
                        {
                            articleTableOfContentsViewModel.dispatchScrollToAnchorEvent(it)
                            dismiss()
                        },
                    )
                itemAnimator = null
                setOnTouchListener { v, e ->
                    when (e.action) {
                        MotionEvent.ACTION_MOVE -> {
                            setBottomSheetDraggable(false)
                        }
                        MotionEvent.ACTION_UP -> {
                            setBottomSheetDraggable(true)
                            v.performClick()
                        }
                    }
                    false
                }
            }
            close.setOnClickListener { dismiss() }
            observeTableOfContentsEvent()
            observeScrollToAnchorEvent()
            restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            KEY_TOC_META_ID,
            articleTableOfContentsViewModel.tableOfContentsEvent.value?.metaId,
        )
        super.onSaveInstanceState(outState)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        val metaId = savedInstanceState?.getString(KEY_TOC_META_ID)
        if (!metaId.isNullOrEmpty()) {
            articleTableOfContentsViewModel.restoreShowTableOfContentsEvent(metaId)
        }
    }

    private fun observeTableOfContentsEvent() {
        articleTableOfContentsViewModel.tableOfContentsEvent.observe(viewLifecycleOwner) Observer@{
            it ?: return@Observer
            binding.apply {
                title.text =
                    GlobalFontAdjustmentSpan.applyGlobalFontSpan(it.tableOfContents.liveText)
                (recyclerView.adapter as? TableOfContentsRecyclerViewAdapter)?.submitList(
                    it.tableOfContents.children,
                )
                articleTableOfContentsViewModel.getAnchorPos().let { pos ->
                    if (pos > -1) {
                        (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                            pos,
                            0,
                        )
                    }
                }
            }
        }
    }

    private fun observeScrollToAnchorEvent() {
        articleTableOfContentsViewModel.scrollToAnchorEvent.observe(viewLifecycleOwner) {
            articleTableOfContentsViewModel.getAnchorPos().let { pos ->
                if (pos > -1) {
                    Measurement.trackLiveUpdateTap((pos + 1).toString())
                    binding.recyclerView.adapter?.notifyItemChanged(pos)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        val TAG = ArticleTableOfContentsFragment::class.java.simpleName
        const val KEY_TOC_META_ID = "toc_meta_id"
    }
}
