package com.wapo.view.menu

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wapo.view.R
import java.util.*

internal class MenuAdapter(
        context: Context,
        @LayoutRes private val headerLayoutId: Int,
        @LayoutRes private val itemLayoutId: Int,
        @LayoutRes private val footerLayoutId: Int,
        @LayoutRes private val recentItemLayoutId: Int,
        @LayoutRes private val dividerLayoutId: Int,
        recentTitle: CharSequence,
        sectionsTitle: CharSequence
) : RecyclerView.Adapter<MenuAdapter.BaseMenuHolder>() {

    companion object {
        private const val TYPE_FEATURED = 0
        private const val TYPE_AZ = 1
        private const val TYPE_FOOTER = 2
        private const val TYPE_RECENT_LIST = 3
        private const val TYPE_RECENT = 4
        private const val TYPE_DIVIDER = 5

        private val TypesMap = mapOf(
            FeaturedItem::class.java to TYPE_FEATURED,
            AzItem::class.java to TYPE_AZ,
            FooterItem::class.java to TYPE_FOOTER,
            RecentListItem::class.java to TYPE_RECENT_LIST,
            RecentItem::class.java to TYPE_RECENT,
            DividerItem::class.java to TYPE_DIVIDER
        )
    }

    var recentTitle: CharSequence = recentTitle
        set(value) {
            field = value
            if (recentItemsCount > 0) {
                notifyDataSetChanged()
            }
        }

    var sectionsTitle: CharSequence = sectionsTitle
        set(value) {
            field = value
            if (recentItemsCount > 0) {
                notifyDataSetChanged()
            }
        }

    private val items = ArrayList<MenuItem>()
    private var recentItemsCount = 0

    private var sectionClickListener: SectionClickListener? = null
    private val inflater: LayoutInflater = LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, itemViewType: Int): BaseMenuHolder {
        return when (itemViewType) {
            TYPE_FEATURED -> MenuFeaturedHolder(inflater.inflate(headerLayoutId, viewGroup, false))
            TYPE_AZ -> MenuAzHolder(inflater.inflate(itemLayoutId, viewGroup, false))
            TYPE_FOOTER -> BaseMenuHolder(inflater.inflate(footerLayoutId, viewGroup, false))
            TYPE_RECENT_LIST -> RecentItemHolder(inflater.inflate(recentItemLayoutId, viewGroup, false))
            TYPE_RECENT -> MenuRecentHolder(inflater.inflate(recentItemLayoutId, viewGroup, false))
            TYPE_DIVIDER -> DividerHolder(inflater.inflate(dividerLayoutId, viewGroup, false))
            else -> throw IllegalStateException("Unsupported item")
        }
    }

    override fun onBindViewHolder(viewHolder: BaseMenuHolder, position: Int) {
        val menuItem = items[position]
        viewHolder.bind(menuItem)
    }

    override fun getItemViewType(position: Int): Int {
        return TypesMap[items[position].javaClass]
                ?: throw IllegalStateException("Unsupported item type ${items[position].javaClass}")
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int) = items[position].hashCode().toLong()

    fun setItems(items: List<MenuItem>, recentItems: List<MenuItem>) {
        this.items.clear()
        this.items.addAll(items)
        this.items.add(FooterItem())
        recentItemsCount = 0
        setRecentItems(recentItems)

        notifyDataSetChanged()
    }

    private fun setRecentItems(recentItems: List<MenuItem>) {
        var isModified = false
        if (recentItemsCount > 0) {
            items.removeAt(0)
            isModified = true
        }

        if (recentItems.isNotEmpty()) {
            val recentItemsList = mutableListOf<MenuItem>()
            for (item in recentItems) {
                recentItemsList.add(0, RecentItem(item.id, item.name, item.type))
            }
            this.items.add(0, RecentListItem(recentItemsList))

            isModified = true
        }

        recentItemsCount = recentItems.size

        if (isModified) {
            notifyDataSetChanged()
        }
    }

    fun setSectionClickListener(sectionClickListener: SectionClickListener) {
        this.sectionClickListener = sectionClickListener
    }

    open inner class BaseMenuHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        open fun bind(menuItem: MenuItem) {}
    }

    inner class MenuFeaturedHolder(itemView: View): BaseMenuHolder(itemView) {
        private val nameTextView = itemView.findViewById<TextView>(R.id.menu_item_name)

        override fun bind(menuItem: MenuItem) {
            nameTextView?.text = menuItem.name
            itemView.setOnClickListener {
                sectionClickListener?.onSectionClick(menuItem, false)
            }
        }
    }

    inner class MenuAzHolder(itemView: View): BaseMenuHolder(itemView) {
        private val nameTextView = itemView.findViewById<TextView>(R.id.menu_item_name)

        override fun bind(menuItem: MenuItem) {
            nameTextView?.text = menuItem.name
            itemView.setOnClickListener {
                sectionClickListener?.onSectionClick(menuItem, false)
            }
        }
    }

    inner class RecentItemHolder(itemView: View) : BaseMenuHolder(itemView) {
        private val recentChipGroup = itemView.findViewById<ChipGroup>(R.id.recent_cg)

        override fun bind(menuItem: MenuItem) {
            recentChipGroup?.let { chipGroup ->
                chipGroup.removeAllViews()
                (menuItem as? RecentListItem)?.let {
                    it.items.forEach { menuItem ->
                        val recentItem = LayoutInflater.from(itemView.context).inflate(R.layout.recent_item, chipGroup, false)
                        recentItem.findViewById<Chip>(R.id.recent_item_chip).apply {
                            text = menuItem.name
                            chipGroup.addView(this, chipGroup.childCount, this.layoutParams)
                            setOnClickListener {
                                sectionClickListener?.onSectionClick(menuItem, true)
                            }
                        }
                    }
                }
            }
        }
    }

    inner class MenuRecentHolder(itemView: View): BaseMenuHolder(itemView) {
        private val nameTextView = itemView.findViewById<TextView>(R.id.menu_item_name)

        override fun bind(menuItem: MenuItem) {
            nameTextView?.text = menuItem.name
            itemView.setOnClickListener {
                sectionClickListener?.onSectionClick(menuItem, false)
            }
        }
    }

    inner class DividerHolder(itemView: View): BaseMenuHolder(itemView) {
        private val menuDivider = itemView.findViewById<View>(R.id.menu_divider)

        override fun bind(menuItem: MenuItem) {
            (menuItem as? DividerItem)?.let { dividerItem ->
                menuDivider.visibility = if (dividerItem.hideRuler) View.INVISIBLE else View.VISIBLE
                (menuDivider.layoutParams as? FrameLayout.LayoutParams)?.let {
                    it.setMargins(dividerItem.leftMargin, dividerItem.topMargin, dividerItem.rightMargin, dividerItem.bottomMargin)
                    menuDivider.layoutParams = it
                }
            }
        }
    }
}
