package com.wapo.view.nested

import androidx.recyclerview.widget.RecyclerView

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
open class NestedAdapter(val baseAdapter : BaseAdapter) : RecyclerView.Adapter<BaseViewHolder>(){

    private var nestedListView: NestedListView? = null

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val view = baseAdapter.getView(position, holder.itemView, holder.itemView.parent as ViewGroup?)
        val lv = nestedListView;
        val listener = lv?.itemClickListener
        if (listener != null && lv != null){
            view.setOnClickListener {
                listener.onItemClick(lv, it, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val pos = findPosWithViewType(viewType)
        val view = baseAdapter.getView(pos, null, parent)
        return BaseViewHolder(view)
    }

    private fun findPosWithViewType(viewType: Int): Int {
        for (i in 0..baseAdapter.count-1) {
            val baseViewType = baseAdapter.getItemViewType(i)
            if (viewType == baseViewType) {
                return i
            }
        }
        return -1
    }

    override fun getItemCount(): Int {
        return baseAdapter.count
    }

    override fun getItemViewType(position: Int): Int {
        return baseAdapter.getItemViewType(position)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.nestedListView = recyclerView as NestedListView?
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        nestedListView = nestedListView
    }
}

class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)