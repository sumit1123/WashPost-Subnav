package com.washingtonpost.android.paywall.reminder.accounthold

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.UnderlineSpan
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.bottomsheet.UserEvent
import com.washingtonpost.android.paywall.databinding.FragmentAccountholdReminderScreenBinding

class AccountHoldFragment : DialogFragment() {

    private val accountHoldScreenViewModel: AccountHoldViewModel by activityViewModels()
    private var paywallSheetSideMargin: Float = 0f
    private var separator: Int = 0
    private var nextItemVisiblePx: Float = 0f
    private val screenType = "fromScreen"
    private var _binding: FragmentAccountholdReminderScreenBinding? = null
    private val binding get() = _binding!!


    private var screenFrom: AccountHoldType? = null

    enum class AccountHoldType {
        SECTION_DISPLAY, ARTICLE_SCREEEN, SETTINGS_DISPLAY, GLOBAL_SCREEN_DISPLAY
    }

    private val onClickListener = View.OnClickListener { v ->
        when (v) {
            binding.accountHoldClose -> handleClick(UserEvent.Close)
            binding.accountContactUs -> handleClick(UserEvent.ContactUs)
            binding.btnUpdatePayment -> handleClick(UserEvent.UpdatePaymentDetails)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountholdReminderScreenBinding.inflate(inflater, container, false)
        trackEvent()
        return binding.root
    }

    private fun trackEvent() {
        PaywallService.getOmniture().trackAccountHoldEvent(context, screenFrom)
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private fun updateUI() {
        binding.header.setTextOrHide(
            formatText(
                getString(R.string.accounthold_header),
                R.style.accounthold_text_h1
            )
        )
        binding.core.setTextOrHide(
            formatText(
                getString(R.string.core),
                R.style.accounthold_core
            )
        )
        accountHoldScreenViewModel.getPrice()?.apply {
            binding.accountHoldPrice.setTextOrHide(formatText(this, R.style.accounhold_sub_details))
        }
        accountHoldScreenViewModel.getTitle()?.apply {
            binding.core.setTextOrHide(formatText(this, R.style.accounthold_core))
        }
        binding.accountContactUs.setTextOrHide(
            formatTextWithUnderLine(
                getString(R.string.contact_us),
                R.style.accounhold_contact_us
            )
        )
        binding.accountholdTextH2.setTextOrHide(
            formatText(
                getString(R.string.accounthold_update),
                R.style.accounthold_text_h2
            )
        )
        binding.accountHoldClose.setOnClickListener(onClickListener)
        binding.accountContactUs.setOnClickListener(onClickListener)
        binding.btnUpdatePayment.setOnClickListener(onClickListener)
    }

    override fun getTheme(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            R.style.Theme_NoWiredStrapInNavigationBar
        } else {
            super.getTheme()
        }
    }

    fun show(@NonNull fragmentManager: FragmentManager, @Nullable tag: String, reason: Int) {
        fragmentManager.beginTransaction()
            .add(this, tag)
            .commitAllowingStateLoss()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.apply {
            nextItemVisiblePx = resources.getDimension(R.dimen.viewpager_next_item_visible)
            paywallSheetSideMargin = resources.getDimension(R.dimen.paywall_sheet_side_margin)
            separator = this.resources.getDimension(R.dimen.viewpager_next_item_visible).toInt()
        }
        accountHoldScreenViewModel.accountHoldStatus?.observe(this, Observer {
            it?.let {
                if (it) {
                    dismiss()
                }
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.let {
            screenFrom = AccountHoldType.values()[it.getInt(screenType)]
        }
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            val prefStorage = AccountHoldReminderStorage.getInstance(it)
            prefStorage.iapRegistrationAskReminderShownTime = SystemClock.elapsedRealtime()
        }
        accountHoldScreenViewModel.update()
    }


    private fun handleClick(event: UserEvent) {
        when (event) {
            UserEvent.Close -> {
                PaywallService.getOmniture().trackAccountHoldDismiss(screenFrom)
                when (screenFrom) {
                    AccountHoldType.ARTICLE_SCREEEN -> activity?.finish()
                    else -> dismiss()
                }

            }
            UserEvent.ContactUs -> PaywallService.getConnector()
                .showContactUs(context)
            UserEvent.UpdatePaymentDetails -> {
                PaywallService.getOmniture().trackAccountHoldPayment(screenFrom)
                PaywallService.getConnector().openPlaystore(activity)
            }
            else -> {
                // no op
            }
        }

    }

    private fun TextView.setTextOrHide(string: CharSequence?) {
        if (string.isNullOrEmpty()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = string
        }
    }

    private fun formatText(
        @NonNull text: String,
        @NonNull appearance: Int,
        isHtml: Boolean = false
    ): CharSequence {
        val builder = SpannableStringBuilder()
        return when {
            TextUtils.isEmpty(text) -> builder
            isHtml -> HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
            else -> {
                builder.append(text)
                builder.setSpan(
                    WpTextAppearanceSpan(context, appearance),
                    0,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder
            }
        }
    }

    private fun formatTextWithUnderLine(
        @NonNull text: String,
        @NonNull appearance: Int,
        isHtml: Boolean = false
    ): CharSequence {
        val builder = SpannableStringBuilder()
        return when {
            TextUtils.isEmpty(text) -> builder
            isHtml -> HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
            else -> {
                builder.append(text)
                builder.setSpan(
                    WpTextAppearanceSpan(context, appearance),
                    0,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            try {
                // Note: found this interesting way to handle orientation. Didn't find a clear one.
                val ft = fragmentManager?.beginTransaction()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ft?.setReorderingAllowed(false)
                }
                ft?.detach(this)?.attach(this)?.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {

        private var accountHoldFragment: AccountHoldFragment? = AccountHoldFragment()

        @JvmStatic
        fun showIfConditionsAreMet(
            @NonNull context: Context,
            @NonNull fragmentManager: FragmentManager,
            @NonNull preferenceStorage: AccountHoldReminderStorage,
            @NonNull bypassElapsedTimeChecks: Boolean = false,
            @NonNull accountHoldType: AccountHoldType
        ): AccountHoldFragment? {
            if (!isConnectedOrConnecting(context)) {
                return null
            }
            if (accountHoldFragment?.isStateSaved == true) {
                return null
            }
            val frequencyRange = context.getString(R.string.accounthold_frequency)
            Logger.i("AccountHOld", "Frequency $frequencyRange")
            return if (AccountHoldViewModel.areConditionsMet(
                    preferenceStorage,
                    bypassElapsedTimeChecks,
                    frequencyRange.toLong()
                )
            ) {

                accountHoldFragment?.apply {
                    arguments = Bundle().apply {
                        putInt(screenType, accountHoldType.ordinal)
                    }
                    this.isCancelable = false
                    show(fragmentManager, "accounthold")
                }
            } else {
                null
            }
        }
    }


}