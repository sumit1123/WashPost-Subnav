package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.widget.AdapterView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.TAB_LABEL_VISIBILITY_UNLABELED
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.model.Carousel
import com.wapo.view.stack.FlexibleStackView
import com.washingtonpost.android.sections.R

class StackViewHolder(itemView: View) : GridViewHolder(itemView) {

    private val stackView = itemView.findViewById<FlexibleStackView>(R.id.stackView)
    private val tabLayout = itemView.findViewById<TabLayout>(R.id.tabLayout)

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val carousel = gridAdapter.items[position] as Carousel
        val adapter = gridAdapter.environment.provideStackViewAdapter(carousel)!!
        stackView.setHasExcerpt(carousel.items.first().excerpt?.text != null)
        stackView.adapter = adapter
        stackView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, pos, id ->
                gridAdapter.onStackCardClicked?.invoke(carousel.items.map{ it.link }, pos)
            }

        stackView.cardChangeListener = object : FlexibleStackView.CardChangeListener {
            override fun onCardChanged(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
                gridAdapter.environment.onStackViewCardChanged(carousel, position)
            }
        }

        tabLayout.removeAllTabs()
        for (i in 0 until adapter.count) {
            val newTab = tabLayout.newTab()
            newTab.tabLabelVisibility = TAB_LABEL_VISIBILITY_UNLABELED
            tabLayout.addTab(newTab, false)
        }
        tabLayout.selectTab(tabLayout.getTabAt(0))
        tabLayout.clearOnTabSelectedListeners()
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                stackView.setSelection(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }
}