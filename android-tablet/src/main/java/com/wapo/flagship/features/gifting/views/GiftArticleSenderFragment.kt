package com.wapo.flagship.features.gifting.views

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wapo.flagship.Utils
import com.wapo.flagship.features.gifting.events.GiftCollabEvent
import com.wapo.flagship.features.gifting.events.UserEvent
import com.wapo.flagship.features.gifting.states.GiftSendUiState
import com.wapo.flagship.features.gifting.tracking.GiftTrackingDetails
import com.wapo.flagship.features.gifting.utils.GiftSenderViewStateHelper
import com.wapo.flagship.features.gifting.viewmodels.GiftArticleSenderViewModel
import com.wapo.flagship.features.gifting.viewmodels.GiftCollaborationViewModel
import com.wapo.flagship.features.settings.contactus.ContactUsActivity
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentGiftArticleBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GiftArticleSenderFragment : BaseBottomSheetDialogFragment() {
    private var _binding: FragmentGiftArticleBinding? = null
    private val binding get() = _binding!!

    private val giftArticleSenderViewModel: GiftArticleSenderViewModel by viewModels()
    private val giftCollaborationViewModel: GiftCollaborationViewModel by activityViewModels()

    /**
     * ViewStateHelper manages the various dialogs to be shown for Gift Sender Flow
     */
    private var _giftSenderViewStateHelper: GiftSenderViewStateHelper? = null
    private val viewStateHelper get() = _giftSenderViewStateHelper!!

    override fun onAttach(context: Context) {
        behaviorState = BottomSheetBehavior.STATE_EXPANDED
        super.onAttach(context)
    }

    /**
     * Load Gift Fragment View
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentGiftArticleBinding.inflate(inflater, container, false)
        _giftSenderViewStateHelper = GiftSenderViewStateHelper(binding, requireContext())
        return binding.root
    }

    /**
     * Start Fragment with Loading Dialog while API network requests are made
     * to retrieve User Gifting Status
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeClickEvent()
        observeUiState()
        giftCollaborationViewModel.dispatchCollabEvent(GiftCollabEvent.DismissDelayedPaywall)
    }

    override fun getTheme(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            com.washingtonpost.android.paywall.R.style.Theme_NoWiredStrapInNavigationBar
        } else {
            super.getTheme()
        }

    /**
     * Call onDismiss lambda above when dialog is dismissed
     */
    override fun onDismiss(dialog: DialogInterface) {
        giftCollaborationViewModel.dispatchCollabEvent(GiftCollabEvent.ShowDelayPaywall)
        super.onDismiss(dialog)
    }

    /**
     * Observe and handle Dialog UI State changes
     */
    private fun observeUiState() {
        giftArticleSenderViewModel.giftUiState.observe(viewLifecycleOwner) { uiState ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            viewStateHelper.animateDailogState(bottomSheetDialog) {
                when (uiState) {
                    GiftSendUiState.Startup -> {
                        giftArticleSenderViewModel.startGiftingFlow()
                    }
                    GiftSendUiState.SignedInSub -> {
                        val url = giftCollaborationViewModel.giftArticleUrl
                        url?.let {
                            giftArticleSenderViewModel.signedInSubscriberTappedGiftIcon(it)
                        }
                        // TODO : Remote log if url is null
                    }
                    GiftSendUiState.Failure -> {
                        viewStateHelper.showFailure(requireContext()) {
                            giftArticleSenderViewModel.dispatchUserEvent(it)
                        }
                        giftCollaborationViewModel.dispatchGiftTrackingEvent(
                            GiftTrackingDetails.GIFT_FAILURE,
                        )
                    }
                    GiftSendUiState.Loading -> viewStateHelper.showLoading()
                    GiftSendUiState.NoGifts -> {
                        viewStateHelper.showNoGiftsLeft {
                            giftArticleSenderViewModel.dispatchUserEvent(it)
                        }
                        giftCollaborationViewModel.dispatchGiftTrackingEvent(
                            GiftTrackingDetails.GIFT_CLICK_NONE_LEFT,
                        )
                    }
                    GiftSendUiState.NoSub ->
                        viewStateHelper.showNoSub {
                            giftArticleSenderViewModel.dispatchUserEvent(it)
                        }
                    GiftSendUiState.NotSignedIn ->
                        viewStateHelper.showNotSignedIn {
                            giftArticleSenderViewModel.dispatchUserEvent(it)
                        }
                    is GiftSendUiState.GiftAgain -> {
                        viewStateHelper.showGifting(
                            uiState.giftCount,
                            true,
                        ) {
                            giftArticleSenderViewModel.dispatchUserEvent(it)
                        }
                    }
                    is GiftSendUiState.Gift -> {
                        viewStateHelper.showGifting(uiState.giftCount, false) {
                            giftArticleSenderViewModel.dispatchUserEvent(it)
                        }
                    }
                    is GiftSendUiState.GiftTokenUrl -> {
                        // Load System Share dialog with bitly url
                        val url = giftCollaborationViewModel.giftArticleUrl
                        val isActionButton = giftCollaborationViewModel.isActionButton
                        val appSection = giftCollaborationViewModel.appSection

                        viewStateHelper.showNativeGiftShare(
                            url,
                            uiState.url,
                            isActionButton,
                            appSection,
                            requireContext(),
                            giftCollaborationViewModel.trackingInfo,
                        )
                        dismiss()
                    }
                }
            }
        }
    }

    /**
     * Observe and handle user click events
     */
    private fun observeClickEvent() {
        giftArticleSenderViewModel.userEvent.observe(viewLifecycleOwner) { userEvent ->
            when (userEvent) {
                UserEvent.Gift -> {
                    val url = giftCollaborationViewModel.giftArticleUrl
                    url?.let {
                        giftArticleSenderViewModel.shareButtonClickedOnGiftBottomSheet(it)
                        giftCollaborationViewModel.dispatchGiftTrackingEvent(
                            GiftTrackingDetails.SHARE_BUTTON_CLICK,
                        )
                    }
                }
                UserEvent.LearnMore -> {
                    val learnMoreUrl = resources.getString(R.string.learn_more_url)
                    Utils.startWebChromeCustomTab(learnMoreUrl, context)
                }
                UserEvent.Paywall -> {
                    giftCollaborationViewModel.dispatchCollabEvent(GiftCollabEvent.Paywall)
                    dismiss()
                }
                UserEvent.SignIn -> {
                    giftCollaborationViewModel.dispatchCollabEvent(GiftCollabEvent.SignIn)
                    dismiss()
                }
                UserEvent.ContactUs -> {
                    val intent = Intent(requireContext(), ContactUsActivity::class.java)
                    requireContext().startActivity(intent)
                    dismiss()
                }
                UserEvent.Dismiss -> {
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        viewStateHelper.cleanup()
        _giftSenderViewStateHelper = null
        super.onDestroyView()
    }
}
