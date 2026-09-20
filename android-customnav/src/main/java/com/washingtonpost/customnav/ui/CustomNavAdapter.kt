package com.washingtonpost.customnav.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.customnav.data.CustomNavSection
import com.washingtonpost.customnav.databinding.ItemCustomNavCellBinding

class CustomNavAdapter : RecyclerView.Adapter<CustomNavCellViewHolder>() {

    private val items = mutableListOf<CustomNavSection>()
    var onButtonClicked: ((CustomNavSection) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomNavCellViewHolder {
        val binding = ItemCustomNavCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomNavCellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomNavCellViewHolder, position: Int) {
        holder.onBindViewHolder(items[position], onButtonClicked)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: List<CustomNavSection>) {
        val diffCallback = CustomNavDiffCallback(this.items, items)
        val result = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(items)
        result.dispatchUpdatesTo(this)
    }
}

class CustomNavDiffCallback(val old: List<CustomNavSection>, val new: List<CustomNavSection>): DiffUtil.Callback() {

    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition].id == new[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition] == new[newItemPosition]
    }
}