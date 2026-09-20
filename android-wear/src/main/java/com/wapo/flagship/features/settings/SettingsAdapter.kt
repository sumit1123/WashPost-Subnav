/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.settings

import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.databinding.SettingsRowBinding

/**
 *  Unused in new wear app. Reserve just in case.
 *
 */
class SettingsAdapter(
    private val sectionList: List<String>,
    private val onSectionChangedListener: (String) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.SectionsViewHolder>() {

    inner class SectionsViewHolder(private val binding: SettingsRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sectionName: String) {
            with(binding) {
                checkbox.text = sectionName
//                checkbox.isChecked =
//                    TextUtils.equals(sectionName, WearAppContext.extraSection)
                checkbox.setOnClickListener {
                    if (binding.checkbox.isChecked) {
                        notifyDataSetChanged()
                        onSectionChangedListener(sectionName)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionsViewHolder {
        val binding = SettingsRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return SectionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionsViewHolder, position: Int) {
        holder.bind(sectionList[position])
    }

    override fun getItemCount(): Int = sectionList.size
}