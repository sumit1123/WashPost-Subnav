package com.wapo.view.menu

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import com.wapo.view.R

class HierarchyMenuView : RecyclerView {
    private val _adapter: MenuAdapter

    private var headerLayoutId = R.layout.hierarchy_menu_header
    private var itemLayoutId = R.layout.hierarchy_menu_item
    private var footerLayoutId = R.layout.footer_item
    private var recentItemLayoutId = R.layout.hierarchy_menu_header
    private var dividerLayoutId = R.layout.menu_divider

    var recentTitle: CharSequence = context.getString(R.string.hierarchy_recent)
        set(value) {
            field = value
            _adapter.recentTitle = value
        }

    var mainTitle: CharSequence = context.getString(R.string.hierarchy_sections)
        set(value) {
            field = value
            _adapter.sectionsTitle = value
        }

    var sectionClickListener: SectionClickListener? = null

    @JvmOverloads
    constructor(context: Context, attributeSet: AttributeSet? = null) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleId: Int) : super(context, attributeSet, defStyleId) {
        if (attributeSet != null) {
            val a = context.obtainStyledAttributes(attributeSet, R.styleable.HierarchyMenuView)
            try {
                headerLayoutId = a.getResourceId(R.styleable.HierarchyMenuView_header_layout_id, headerLayoutId)
                itemLayoutId = a.getResourceId(R.styleable.HierarchyMenuView_item_layout_id, itemLayoutId)
                recentItemLayoutId = a.getResourceId(R.styleable.HierarchyMenuView_recent_item_layout_id, recentItemLayoutId)
                dividerLayoutId = a.getResourceId(R.styleable.HierarchyMenuView_divider_layout_id, dividerLayoutId)
            } finally {
                a?.recycle()
            }
        }

        _adapter = MenuAdapter(
                    context,
                    headerLayoutId,
                    itemLayoutId,
                    footerLayoutId,
                    recentItemLayoutId,
                    dividerLayoutId,
                    recentTitle,
                    mainTitle
                )
                .apply {
                    setSectionClickListener(object : SectionClickListener{
                        override fun onSectionClick(item: MenuItem, fromRecent: Boolean) {
                            sectionClickListener?.onSectionClick(item, fromRecent)
                        }
                    })
                }

        super.setAdapter(_adapter)
        super.setLayoutManager(LinearLayoutManager(context))
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        throw UnsupportedOperationException("adapter is set internally")
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        throw UnsupportedOperationException("layout manager is set internally")
    }

    fun setItems(items: List<MenuItem>, recentItems: List<MenuItem>) {
        _adapter.setItems(items, recentItems)
    }
}