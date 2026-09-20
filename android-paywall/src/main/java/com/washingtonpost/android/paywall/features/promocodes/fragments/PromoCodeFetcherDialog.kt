package com.washingtonpost.android.paywall.features.promocodes.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.washingtonpost.android.paywall.databinding.FragmentPromocodeFetcherBinding
import com.washingtonpost.android.paywall.features.promocodes.viemodels.PromoCodeViewModel
import com.washingtonpost.android.paywall.models.PromoCodeRequestState
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.washingtonpost.android.paywall.R

/**
 * Dialog that initiates the promo codes fetching promo codes by invoking functions in [PromoCodeViewModel]
 * This is a transparent full-screen dialog with just a Progress bar in the center.
 */
class PromoCodeFetcherDialog: DialogFragment() {

    private val promoCodeViewModel: PromoCodeViewModel by viewModels()

    private var _binding: FragmentPromocodeFetcherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        _binding = FragmentPromocodeFetcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.also { window ->
            window.attributes?.also { attributes ->
                attributes.dimAmount = 0.5f
                window.attributes = attributes
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        promoCodeViewModel.promoCodeRequestState.observe(viewLifecycleOwner, { promoCodeRequestState ->
            when(promoCodeRequestState){
                PromoCodeRequestState.Failure -> {
                    /*
                        In case of failure just show an alert dialog with "Try again" and "Cancel" buttons.
                     */
                    AlertDialog.Builder(requireContext()).apply {
                        setCancelable(true)
                        setMessage(R.string.try_again_message)
                        /*
                            Try again will let the view model know that user is trying to retrieve the promo code again.
                         */
                        setPositiveButton(R.string.try_again) { dialogInterface, _ ->
                            promoCodeViewModel.tryAgainClicked()
                            dialogInterface.dismiss()
                        }
                        /*
                            Cancel dismisses the alert dialog as well as this Dialog Fragment.
                         */
                        setNegativeButton(R.string.cancel){ dialogInterface, _ ->
                            dialogInterface.dismiss()
                            dismiss()
                        }
                    }.show()
                }
                is PromoCodeRequestState.Success -> {
                    /*
                        This redirects user to the playstore app for subscription purchase with applied promo code.
                     */
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/redeem?code=${promoCodeRequestState.promocode.promoCode}"))
                    startActivity(browserIntent)
                    dismiss()
                }
            }
        })
        promoCodeViewModel.startPromoCodeRetrieval()
    }

    companion object {
        const val TAG = "PromoCodeFetcherDialog"
    }


}