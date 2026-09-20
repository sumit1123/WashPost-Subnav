package com.wapo.flagship.features.settings

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.wapo.flagship.external.toDp
import com.wapo.flagship.features.onboarding2.activity.Onboarding2Activity
import com.wapo.flagship.features.preferencesapi.GetUserContentPacksApiStatus
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.wapo.flagship.features.preferencesapi.models.ContentPacksValueItem
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentSettingsContentPacksBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsContentPacksFragment : Fragment() {
    private lateinit var binding: FragmentSettingsContentPacksBinding

    private val contentPacksViewModel: ContentPacksViewModel by activityViewModels()

    /**
     * Used by [ContentPacksFragment] for onboarding.
     * Gates user's advance to next onboarding step via Continue button.
     */
    var done: () -> Unit = {}

    /**
     * Used by [ContentPacksFragment] to resize onboarding container.
     */
    var uiStateChanged: (ContentPacksUiState?) -> Unit = {}

    /**
     * In onboarding case, retrieve user's selected content packs set by earlier [PostLoginActivity] call.
     * Else make call to get updated user's selected content packs. Stored prefs are fallback if call fails.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Onboarding2Activity) {
            contentPacksViewModel.isOnboarding = true
            contentPacksViewModel.storeUserContentPacks(
                PrefUtils.getSelectedContentPacks(requireContext()),
            )
        } else {
            contentPacksViewModel.getUserContentPacks()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSettingsContentPacksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeUserContentPacksStatus()
        hideHeaderIfNeeded()
        observeUiState()
        if (contentPacksViewModel.contentPacksUiState.value !is ContentPacksUiState.Success) {
            contentPacksViewModel.getContentPackUiData()
        }
    }

    /**
     * There is no submit button in Settings. Submit when user navigates away.
     * Onboarding has a submit button instead.
     */
    override fun onPause() {
        super.onPause()
        if (!contentPacksViewModel.isOnboarding) {
            submitContentPacks()
        }
    }

    /**
     * Makes remote call to set user's content pack selections.
     */
    fun submitContentPacks() {
        context?.let { contentPacksViewModel.submitUserContentPacks(it) }
    }

    /**
     * RecyclerView Grid of span 12 to support different numbers of columns based on screen width.
     * Narrow width: [ContentPackItemLargeViewHolder] span 12, [ContentPackItemViewHolder] span 6.
     * Medium width: [ContentPackItemLargeViewHolder] span 8, [ContentPackItemViewHolder] span 4.
     * Large width: [ContentPackItemLargeViewHolder] span 6, [ContentPackItemViewHolder] span 3.
     */
    private fun prepareRecyclerView(contentPacks: List<ContentPackUiItem?>) {
        binding.rvSettingsContentPacks.apply {
            layoutManager = GridLayoutManager(context, 12)
            (layoutManager as GridLayoutManager).spanSizeLookup =
                object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val scaleFactor =
                            if (position == 0) {
                                2
                            } else {
                                1
                            } // first Content Pack is double wide
                        val screenWidth =
                            resources.displayMetrics.widthPixels.toDp(
                                resources.displayMetrics.density,
                            )
                        return when {
                            screenWidth <= MEDIUM_WIDTH_BREAKPOINT -> {
                                binding.settingsContentPacks.maxWidth = maxWidthValue(700f)
                                TWO_COLUMNS * scaleFactor
                            }
                            screenWidth <= LARGE_WIDTH_BREAKPOINT -> {
                                binding.settingsContentPacks.maxWidth = maxWidthValue(700f)
                                THREE_COLUMNS * scaleFactor
                            }
                            else -> {
                                binding.settingsContentPacks.maxWidth = maxWidthValue(975f)
                                FOUR_COLUMNS * scaleFactor
                            }
                        }
                    }
                }
            adapter =
                ContentPacksRecyclerViewAdapter(
                    {
                        contentPacksViewModel.selectClicked(it)
                    },
                    {
                        done()
                    },
                    contentPacksViewModel,
                ).apply {
                    this.submitList(contentPacks)
                }
        }
    }

    /**
     * Hide Settings header during onboarding.
     * Attach error msg to parent instead of to header.
     */
    private fun hideHeaderIfNeeded() {
        if (contentPacksViewModel.isOnboarding) {
            binding.header.visibility = View.GONE
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.settingsContentPacks)
            constraintSet.connect(
                binding.contentPacksFailureHeader.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
            )
            constraintSet.applyTo(binding.settingsContentPacks)
        }
    }

    private fun maxWidthValue(maxWidth: Float): Int =
        TypedValue
            .applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                maxWidth,
                resources.displayMetrics,
            ).toInt()

    /**
     * Observe and store user content pack prefs from remote.
     */
    private fun observeUserContentPacksStatus() {
        contentPacksViewModel.userContentPacksStatus.observe(viewLifecycleOwner) {
            when (it) {
                is GetUserContentPacksApiStatus.Failure ->
                    contentPacksViewModel.storeUserContentPacks(
                        PrefUtils.getSelectedContentPacks(requireContext()),
                    )
                GetUserContentPacksApiStatus.NoUserContentPacks, GetUserContentPacksApiStatus.NewUser ->
                    contentPacksViewModel.storeUserContentPacks(
                        listOf(),
                    )
                is GetUserContentPacksApiStatus.Success -> {
                    contentPacksViewModel.storeUserContentPacks(it.contentPacks.filterNotNull())
                    reloadItems(it.contentPacks.filterNotNull())
                }
            }
        }
    }

    private fun reloadItems(contentPacks: List<ContentPacksValueItem?>) {
        (binding.rvSettingsContentPacks.adapter as? ContentPacksRecyclerViewAdapter)?.apply {
            contentPacksViewModel.contentPacksUiState.value?.let {
                if (it is ContentPacksUiState.Success) {
                    it.contentPacks.forEachIndexed { index, contentPackUiItem ->
                        if (contentPacks.find { pack -> pack?.pack != null && pack.pack == contentPackUiItem?.id } != null) {
                            this.notifyItemChanged(index)
                        }
                    }
                }
            }
        }
    }

    /**
     * Observe and handle Dialog UI State changes.
     */
    private fun observeUiState() {
        contentPacksViewModel.contentPacksUiState.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                ContentPacksUiState.Loading -> showLoading()
                ContentPacksUiState.Failure -> showFailure()
                is ContentPacksUiState.Success -> showSuccess(uiState.contentPacks)
            }
            uiStateChanged(uiState)
        }
    }

    /**
     * Loading UI state while we await Content Pack data response.
     */
    private fun showLoading() {
        binding.rvSettingsContentPacks.visibility = View.GONE
        binding.contentPacksFailureHeader.visibility = View.GONE
        binding.contentPacksFailureMsg.visibility = View.GONE
        binding.contentPacksSpinner.visibility = View.VISIBLE
    }

    /**
     * Failure UI state for when Content Pack data response fails.
     */
    private fun showFailure() {
        binding.contentPacksFailureMsg.text =
            if (contentPacksViewModel.isOnboarding) {
                resources.getString(R.string.content_packs_failure_msg_onboarding)
            } else {
                resources.getString(R.string.content_packs_failure_msg)
            }
        binding.rvSettingsContentPacks.visibility = View.GONE
        binding.contentPacksSpinner.visibility = View.GONE
        binding.contentPacksFailureHeader.visibility = View.VISIBLE
        binding.contentPacksFailureMsg.visibility = View.VISIBLE
        done()
    }

    /**
     * Content Pack UI state. Submits content pack data from response to adapter.
     */
    private fun showSuccess(contentPacks: List<ContentPackUiItem?>) {
        prepareRecyclerView(contentPacks)
        binding.contentPacksSpinner.visibility = View.GONE
        binding.contentPacksFailureHeader.visibility = View.GONE
        binding.contentPacksFailureMsg.visibility = View.GONE
        binding.rvSettingsContentPacks.visibility = View.VISIBLE
    }

    companion object {
        /**
         * Span width corresponding to number of columns to display.
         */
        private const val TWO_COLUMNS = 6
        private const val THREE_COLUMNS = 4
        private const val FOUR_COLUMNS = 3

        /**
         * Screen width breakpoints in dp.
         * Number of columns will change at these breakpoints.
         */
        private const val MEDIUM_WIDTH_BREAKPOINT = 550
        private const val LARGE_WIDTH_BREAKPOINT = 1200
    }
}
