package com.washingtonpost.android.paywall.reminder.acquisition

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.wapo.android.commons.extensions.toSpannableBuilder
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderCtaDestination
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderModel
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.bottomsheet.UserEvent
import com.washingtonpost.android.paywall.databinding.FragmentAcquisitionReminderScreenBinding
import com.washingtonpost.android.paywall.reminder.ReminderActivityInterface
import com.washingtonpost.android.paywall.reminder.state.DialogType
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AcquisitionReminderFragment : DialogFragment() {
    private val reminderScreenViewModel: AcquisitionReminderViewModel by viewModels()
    private var _binding: FragmentAcquisitionReminderScreenBinding? = null
    private val binding get() = _binding!!

    private val onClickListener = View.OnClickListener { v ->
        when (v) {
            binding.subscribe -> handleClick(UserEvent.Subscribe(reminderScreenViewModel.sku))
            binding.signIn -> handleClick(UserEvent.SignIn)
            binding.maybeLater -> handleClick(UserEvent.Close)
            binding.privacyPolicyFinePrint -> handleClick(UserEvent.PrivacyPolicy)
            binding.termsOfServiceFinePrint -> handleClick(UserEvent.TermsOfService)
        }
    }

    private fun handleClick(event: UserEvent) {
        activity?.apply {
            when (event) {
                UserEvent.SignIn -> {
                    PaywallService.getConnector().showSignInScreen(activity?.supportFragmentManager, AuthIntentBuilder().build(), null, PaywallConstants.WallType.REMINDER_PAYWALL, true, null)
                    dismiss()
                }
                is UserEvent.Subscribe -> {
                    when (reminderScreenViewModel.getCTASubscribeActionType()) {
                        AcquisitionReminderCtaDestination.PAYWALL -> {
                            (this as? ReminderActivityInterface)?.showPaywallFromReminder(
                                PaywallConstants.WallType.REMINDER_PAYWALL
                            )
                            dismiss()
                        }
                        AcquisitionReminderCtaDestination.PURCHASE -> {
                            (this as? ReminderActivityInterface)?.startPurchaseFlow(
                                event.productId,
                                PaywallUtil.getOfferId(event.productId, null),
                                PaywallConstants.WallType.REMINDER_PAYWALL
                            )
                        }
                    }

                }
                UserEvent.Close -> dismiss()
                UserEvent.TermsOfService -> PaywallService.getConnector()
                    .showPolicy(PaywallConstants.TERMS_OF_SERVICE, context)
                UserEvent.PrivacyPolicy -> PaywallService.getConnector()
                    .showPolicy(PaywallConstants.PRIVACY_POLICY, context)
                else -> {
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAcquisitionReminderScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PaywallService.getConnector().trackOnboardingSeen(PaywallService.getOmniture().getSignInEntranceType(DialogType.ACQUISITION))
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            val prefStorage = AcquisitionReminderStorage.getInstance(it)
            prefStorage.iapRegistrationAskReminderShownTime = SystemClock.elapsedRealtime()
        }
        if (PaywallService.getInstance() == null) {
            dismissAllowingStateLoss()
        } else {
            reminderScreenViewModel.update()
        }
    }

    private fun updateUI() {
        // Check for updated subscription status
        reminderScreenViewModel.update()

        // Connect Live Data to UI
        reminderScreenViewModel.getHeader().observe(this, Observer {
            binding.header.setTextOrHide(
                formatText(
                    it,
                    R.style.acquisition_screen_header_style_variant
                )
            )
        })
        reminderScreenViewModel.getMessage().observe(this, Observer {
            binding.message.setTextOrHide(
                formatText(
                    it,
                    R.style.acquisition_screen_description_style_variant
                )
            )
        })
        reminderScreenViewModel.getCTATextLiveData().observe(this, Observer {
            binding.subscribe.setTextOrHide(formatText(it, R.style.reminder_screen_button1, true))
        })
        reminderScreenViewModel.signOnVisibleLiveData.observe(this, Observer {
            binding.signInText.visibility = if (it) View.VISIBLE else View.GONE
        })

        // Handle button clicks
        binding.subscribe.setOnClickListener(onClickListener)
        binding.signIn.setOnClickListener(onClickListener)
        binding.maybeLater.setOnClickListener(onClickListener)
        binding.privacyPolicyFinePrint.setOnClickListener(onClickListener)
        binding.termsOfServiceFinePrint.setOnClickListener(onClickListener)
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
            isHtml -> text.toSpannableBuilder() ?: builder
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val TAG = "acquisition_reminder"

        @JvmStatic
        fun showIfConditionsAreMet(
            @NonNull context: Context,
            @NonNull containerViewId: Int,
            @NonNull fragmentManager: FragmentManager,
            @NonNull acquisitionReminderStorage: AcquisitionReminderStorage,
            @NonNull acquisitionReminderModel: AcquisitionReminderModel,
            @NonNull bypassElapsedTimeChecks: Boolean = false
        ): AcquisitionReminderFragment? {

            if (!acquisitionReminderModel.enabled || !isConnectedOrConnecting(context) || PaywallService.getInstance() == null) {
                return null
            }
            return if (AcquisitionReminderViewModel.areConditionsMet(
                    acquisitionReminderModel,
                    acquisitionReminderStorage,
                    bypassElapsedTimeChecks
                )
            ) {
                // Remove existing fragment
                val ft = fragmentManager.beginTransaction()
                val prev = fragmentManager.findFragmentByTag(TAG)
                if (prev != null) {
                    ft.remove(prev)
                }
                ft.addToBackStack(null)

                // Add new fragment
                AcquisitionReminderFragment().apply {
                    fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                        .add(containerViewId, this)
                        .addToBackStack(TAG)
                        .commit()
                }
            } else {
                null
            }
        }
    }
}
