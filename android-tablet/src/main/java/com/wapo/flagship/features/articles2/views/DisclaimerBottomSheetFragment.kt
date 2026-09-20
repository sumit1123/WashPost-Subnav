package com.wapo.flagship.features.articles2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.DisclaimerInfo
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.wapo.flagship.features.articles3.views.DisclaimerInfoBottomSheetView
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A bottom sheet fragment that displays a disclaimer message to the user. This fragment is designed
 * to be used in scenarios where the app needs to inform the user about certain conditions or disclaimers
 * related to the content they are viewing.
 */
@AndroidEntryPoint
class DisclaimerBottomSheetFragment : BottomSheetDialogFragment(), ArticlesInteractionHelper {
    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel by activityViewModels()
    private val disclaimerInfo: DisclaimerInfo
        get() = requireNotNull(
            BundleCompat.getParcelable(
                requireArguments(),
                ARG_DISCLAIMER_INFO,
                DisclaimerInfo::class.java
            )
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val articlesInteractionHelper = this
        return ComposeView(requireContext()).apply {
            setContent {
                AndroidClassicTheme {
                    DisclaimerInfoBottomSheetView(
                        disclaimerInfo = disclaimerInfo,
                        articlesInteractionHelper = articlesInteractionHelper
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val bottomSheet = (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        BottomSheetBehavior.from(bottomSheet).apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onEventFired(event: ArticleInteractionEvent) {
        if (event is ArticleInteractionEvent.LinkClickEvent) {
            articlesPagerCollaborationViewModel.linkClicked(event.url)
        }
    }


    companion object {
        const val TAG = "DisclaimerBottomSheetFragment"
        private const val ARG_DISCLAIMER_INFO = "disclaimer_info"

        /**
         * Creates a new instance of [DisclaimerBottomSheetFragment].
         *
         * @return A new instance of [DisclaimerBottomSheetFragment].
         */
        fun newInstance(
            disclaimerInfo: DisclaimerInfo
        ) = DisclaimerBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_DISCLAIMER_INFO, disclaimerInfo)
            }
        }
    }
}
