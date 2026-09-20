package com.wapo.view.nested

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Rect
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View

open class NestedListView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : com.wapo.view.selection.SelectableRecyclerView(context, attrs, defStyleAttr) {
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?) : this(context, null)

    var itemClickListener: OnItemClickListener? = null

    init {
        layoutManager = com.wapo.view.selection.SelectableLayoutManager(context)
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        adapter as? NestedAdapter ?: return
        adapter.baseAdapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                adapter.notifyDataSetChanged()
            }

            override fun onInvalidated() {
                adapter.notifyDataSetChanged()
            }
        })
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return false
    }

    fun setSelection(pos: Int) = scrollToPosition(pos)

    fun getFirstVisiblePosition(): Int {
        val lm = layoutManager as LinearLayoutManager
        return lm.findFirstVisibleItemPosition()
    }

}

interface OnItemClickListener {
    fun onItemClick(parent: NestedListView, view: View, position: Int)
}
