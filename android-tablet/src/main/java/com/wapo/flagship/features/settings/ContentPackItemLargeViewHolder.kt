package com.wapo.flagship.features.settings

import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ContentPackLargeBinding

/**
 * ViewHolder for the large, "double-wide" Content Pack view.
 * Only the first content pack is this type. See [ContentPackItemViewHolder] for the rest.
 */
class ContentPackItemLargeViewHolder(
    val binding: ContentPackLargeBinding,
    private val selectClicked: (ContentPackUiItem) -> Unit,
    private val done: () -> Unit,
    private val contentPacksViewModel: ContentPacksViewModel,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(contentPackUiItem: ContentPackUiItem) {
        binding.title.text = contentPackUiItem.heading
        binding.description.text = contentPackUiItem.description
        val requestOption = RequestOptions().centerCrop()
        Glide
            .with(binding.root)
            .load(contentPackUiItem.transparentSmall)
            .apply(requestOption)
            .into(binding.image)
        setUpSelectButton(contentPackUiItem)
    }

    /**
     * Sets up button's style, state, and click events.
     * On click does 3 things:
     * - Sends callback up chain to [ContentPacksViewModel] about which content pack was selected/unselected.
     * - Sends callback up chain to [SettingsContentPacksFragment] to notify that a user action has been made.
     *      Used for onboarding - progress via Continue button to next onboarding step is gated behind making at least 1 selection.
     * - Toggles button's state and UI.
     */
    private fun setUpSelectButton(contentPackUiItem: ContentPackUiItem) {
        removeButtonShadow()
        setInitialButtonState(contentPackUiItem.id)
        binding.selectBtn.setOnClickListener {
            selectClicked(contentPackUiItem)
            done()
            toggleButtonState(it)
        }
    }

    /**
     * Initializes Select button with state matching user's preferences.
     * Marks button as Selected if content pack's id is found in list of user's selected content packs.
     */
    private fun setInitialButtonState(contentPack: String?) {
        val userContentPacks =
            contentPacksViewModel.selectedContentPacks.value?.mapNotNull {
                it.pack
            } ?: listOf()
        if (userContentPacks.contains(contentPack)) {
            binding.selectBtn.isSelected = true
            binding.selectBtnText.text =
                binding.root.context.getString(
                    R.string.content_packs_select_button_selected_text,
                )
        } else {
            binding.selectBtn.isSelected = false
            binding.selectBtnText.text =
                binding.root.context.getString(
                    R.string.content_packs_select_button_text,
                )
        }
    }

    /**
     * Toggles button state between selected and unselected.
     * Fires on click.
     */
    private fun toggleButtonState(selectButton: View) {
        selectButton.isSelected = selectButton.isSelected != true
        if (selectButton.isSelected) {
            binding.selectBtnText.text =
                binding.root.context.getString(
                    R.string.content_packs_select_button_selected_text,
                )
        } else {
            binding.selectBtnText.text =
                binding.root.context.getString(
                    R.string.content_packs_select_button_text,
                )
        }
    }

    private fun removeButtonShadow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.selectBtn.stateListAnimator = null
        }
    }
}
