package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.adapters.ContextBoxPagerAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.ContextBox
import com.washingtonpost.android.databinding.ContextBoxBinding

/**
 * Viewholder associated to ContextBox [Item] model that comes through in Article2 as a items list.
 */
class ContextBoxViewHolder(
    private val binding: ContextBoxBinding,
    private val interactionHelper: ArticlesInteractionHelper,
    private val contextBoxStyleHelper: ContextBoxStyleHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ContextBox>(binding.root) {
    private val layoutManager: LinearLayoutManager? get() =
        (
            binding.contextBoxPager.getChildAt(
                0,
            ) as? RecyclerView
        )?.layoutManager as? LinearLayoutManager

    private val onPageChangeCallback =
        object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.pageTabs.apply {
                    selectTab(getTabAt(position))
                }
            }
        }

    override fun unbind() {
        binding.contextBoxPager.apply {
            val layoutParamsMod =
                layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            this.layoutParams = layoutParamsMod
            invalidate()
        }
    }

    override fun bind(
        item: ContextBox,
        position: Int,
    ) {
        binding.apply {
            contextBoxPager.unregisterOnPageChangeCallback(onPageChangeCallback)
            contextBoxPager.adapter = null
            headline.text = item.headline ?: ""
            item.contentPages?.let {
                val adapter =
                    ContextBoxPagerAdapter(
                        it,
                        interactionHelper,
                        contextBoxStyleHelper,
                        item.isLowDataModeEnable,
                        item.lowDataModeLive,
                    )
                setupTabs(it.size)
                contextBoxPager.offscreenPageLimit = 10
                contextBoxPager.adapter = adapter
                setToLargestPage(binding.contextBoxPager.adapter?.itemCount ?: 0)
                binding.contextBoxPager.registerOnPageChangeCallback(onPageChangeCallback)
            }
        }
    }

    /**
     * Logic to set pager to the height of the largest page.
     */
    fun setToLargestPage(count: Int) =
        layoutManager?.apply {
            var largestHeight = 0

            // Check all pages and find the largest
            for (i in 0 until count) {
                val view = findViewByPosition(i)
                view?.let {
                    getMeasuredViewHeightFor(it).let { height ->
                        if (height > largestHeight) {
                            largestHeight = height
                        }
                    }
                }
            }

            if (largestHeight == 0) return@apply

            // Set the pager to height to that of the largest page
            binding.contextBoxPager.apply {
                val layoutParamsMod =
                    layoutParams.apply {
                        height = largestHeight
                    }
                this.layoutParams = layoutParamsMod
                invalidate()
            }
        }

    /**
     * Get the current measured height for a given view
     */
    private fun getMeasuredViewHeightFor(view: View): Int {
        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(wMeasureSpec, hMeasureSpec)
        return view.measuredHeight
    }

    /**
     * Setup dots for pager.
     */
    private fun setupTabs(pageCount: Int) {
        binding.pageTabs.setVisible(pageCount > 1)
        binding.pageTabs.apply {
            removeAllTabs()
            for (i in 0 until pageCount) {
                val newTab = newTab()
                newTab.tabLabelVisibility = TabLayout.TAB_LABEL_VISIBILITY_UNLABELED
                newTab.view.isClickable = false
                addTab(newTab, false)
            }
            selectTab(getTabAt(0))
        }
    }
}
