package com.washingtonpost.customnav.ui

import android.app.ActionBar.LayoutParams
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.customnav.R
import com.washingtonpost.customnav.data.CustomNavCellType
import com.washingtonpost.customnav.data.CustomNavSection
import com.washingtonpost.customnav.databinding.ItemCustomNavCellBinding

class CustomNavCellViewHolder(
    val binding: ItemCustomNavCellBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun onBindViewHolder(
        section: CustomNavSection,
        onButtonClicked: ((CustomNavSection) -> Unit)?
    ) {
        binding.displayName.text = section.displayName

        when (section.cellType) {
            CustomNavCellType.LOCKED -> initLockedCell()
            CustomNavCellType.SELECTED -> initSelectedCell(section, onButtonClicked)
            CustomNavCellType.RECOMMENDED -> initRecommendedCell(section, onButtonClicked)
            else -> initSelectedCell(section, onButtonClicked)
        }
    }

    private fun initLockedCell() {
        binding.icon.setImageResource(R.drawable.custom_nav_lock_icon)
        binding.icon.visibility = View.VISIBLE
        binding.displayName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.custom_nav_locked))
        binding.dragIcon.visibility = View.GONE
        binding.cell.layoutParams.width = LayoutParams.MATCH_PARENT
    }

    private fun initSelectedCell(
        section: CustomNavSection,
        onButtonClicked: ((CustomNavSection) -> Unit)?
    ) {
        binding.icon.setImageResource(R.drawable.custom_nav_remove_icon)
        binding.icon.visibility = View.VISIBLE
        binding.displayName.setTextColor(ContextCompat.getColor(binding.root.context, com.wapo.view.R.color.primary_menu_text))
        binding.dragIcon.visibility = View.VISIBLE
        binding.cell.layoutParams.width = LayoutParams.MATCH_PARENT
        binding.icon.setOnClickListener {
            onButtonClicked?.invoke(section)
        }
    }

    private fun initRecommendedCell(
        section: CustomNavSection,
        onButtonClicked: ((CustomNavSection) -> Unit)?
    ) {
        binding.icon.setImageResource(R.drawable.custom_nav_add_icon)
        binding.icon.visibility = View.VISIBLE
        binding.displayName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.custom_nav_recommended))
        binding.dragIcon.visibility = View.GONE
        binding.cell.layoutParams.width = LayoutParams.WRAP_CONTENT
        binding.cell.setOnClickListener {
            onButtonClicked?.invoke(section)
        }
    }
}