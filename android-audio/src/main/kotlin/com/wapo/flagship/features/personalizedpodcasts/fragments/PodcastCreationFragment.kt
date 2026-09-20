package com.wapo.flagship.features.personalizedpodcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PodcastCreationFragment : BottomSheetDialogFragment() {

    private val personalizedPodcastViewModel: PersonalizedPodcastViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    PodcastBottomSheetContent({ dismiss() }, personalizedPodcastViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        personalizedPodcastViewModel.stopVoiceSample()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog

        dialog?.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
        }

        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isHideable = false

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState != BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }

    }

    companion object {
        const val TAG = "PodcastCreationFragment"

        fun newInstance(): PodcastCreationFragment {
            return PodcastCreationFragment()
        }
    }
}