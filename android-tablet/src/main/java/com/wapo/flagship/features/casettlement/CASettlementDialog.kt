package com.wapo.flagship.features.casettlement

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.android.commons.util.setClickSpan
import com.wapo.flagship.features.settings.*
import com.wapo.flagship.features.settings.contactus.ContactUsActivity
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentCaSettlementBinding
import com.washingtonpost.android.paywall.features.casettlement.CaSettlementValues
import java.text.SimpleDateFormat
import java.util.*

/**
 * This dialog is shown to let users know that there subscription is set to auto-renew.
 */
class CASettlementDialog(
    private val caSettlementValues: CaSettlementValues,
) : BottomSheetDialogFragment() {
    private var _binding: FragmentCaSettlementBinding? = null
    private val binding get() = _binding!!

    private val fromDateFormat = "yyyy-MM-dd HH:mm:ss"
    private val toDateFormat = "MM/dd/yyyy"
    private val contactUs = "contact us"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, androidx.appcompat.R.style.Theme_AppCompat_DayNight_DialogWhenLarge)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCaSettlementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val formattedStringDate = getFormattedDate(caSettlementValues.unFormattedExpirationDate)
        val message =
            getString(
                R.string.ca_settlement_message,
                caSettlementValues.productType,
                formattedStringDate,
                formattedStringDate,
                caSettlementValues.price,
            )
        val spannableString = SpannableString(message)
        spannableString.apply {
            setClickSpan(message, contactUs, com.washingtonpost.android.paywall.R.color.subscription_blue, requireContext()) {
                /**
                 * Redirect to contact us
                 */
                val intent = Intent(requireContext(), ContactUsActivity::class.java)
                startActivity(intent)
            }
        }
        binding.tvContent.text = spannableString
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.btnOk.setOnClickListener { dismiss() }
    }

    private fun getFormattedDate(unformattedDate: String): String {
        val oldFormat = SimpleDateFormat(fromDateFormat, Locale.US)
        val newFormat = SimpleDateFormat(toDateFormat, Locale.US)
        val formattedDateObj = oldFormat.parse(unformattedDate)
        return if (formattedDateObj != null) {
            newFormat.format(formattedDateObj)
        } else {
            unformattedDate
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        AppPreferences.setCASettlementDialogShown()
        Measurement.trackCaSettlementShown()
        super.onDismiss(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet,
            ) as FrameLayout?
        bottomSheet?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<FrameLayout?>(this)
            val layoutParams = this.layoutParams
            val windowHeight = Resources.getSystem().displayMetrics.heightPixels
            if (layoutParams != null) {
                layoutParams.height = windowHeight
            }
            this.layoutParams = layoutParams
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    companion object {
        const val TAG = "CASettlementDialog"
    }
}
