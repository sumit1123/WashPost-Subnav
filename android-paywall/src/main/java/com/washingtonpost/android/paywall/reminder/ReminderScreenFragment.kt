/*
 * Copyright (c) 2019. The Washington Post
 */
package com.washingtonpost.android.paywall.reminder

import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.config.domain.models.config.paywall.ReminderScreenConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants

class ReminderScreenFragment : BottomSheetDialogFragment() {

    enum class ReminderType {
        IAP_REGISTRATION_ASK, IAP_REGISTRATION_ASK_REMINDER
    }

    private val reminderTypeArgument = "reminder_type"
    private val activityOrientationArgument = "activity_orientation"
    private val isPhoneArgument = "is_phone"

    private lateinit var reminderType: ReminderType
    private var isPhone: Boolean = false
    private val reminderScreenViewModel: ReminderScreenViewModel by viewModels()

    private val onClickListener = View.OnClickListener { v ->
        when (v?.id) {
            R.id.reminder_screen_button1 -> reminderScreenViewModel.events.postValue(
                ReminderScreenViewModel.Event.SIGN_IN
            )
            else -> reminderScreenViewModel.events.postValue(ReminderScreenViewModel.Event.CLOSE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reminder_screen, container, false).apply {
            val reminderScreenTextH21: TextView = findViewById(R.id.reminder_screen_text_h2_1)
            val reminderScreenTextH1: TextView = findViewById(R.id.reminder_screen_text_h1)
            val reminderScreenTextH22: TextView = findViewById(R.id.reminder_screen_text_h2_2)
            val reminderScreenTextH31: TextView = findViewById(R.id.reminder_screen_text_h3_1)
            val reminderScreenTextH32: TextView = findViewById(R.id.reminder_screen_text_h3_2)
            val reminderScreenButton1: Button = findViewById(R.id.reminder_screen_button1)
            val reminderScreenButton2: TextView = findViewById(R.id.reminder_screen_button2)
            val reminderScreenClose: ImageButton = findViewById(R.id.reminder_screen_close)
            when (reminderType) {
                ReminderType.IAP_REGISTRATION_ASK -> {
                    reminderScreenTextH21.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_text_h2_1,
                            R.style.reminder_screen_text_h21
                        )
                    )
                    reminderScreenTextH1.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_text_h1,
                            R.style.reminder_screen_text_h1
                        )
                    )
                    reminderScreenTextH22.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_text_h2_2,
                            R.style.reminder_screen_text_h22
                        )
                    )
                    reminderScreenTextH31.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_text_h3_1,
                            R.style.reminder_screen_text_h3
                        )
                    )
                    reminderScreenTextH32.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_text_h3_2,
                            R.style.reminder_screen_text_h3
                        )
                    )
                    reminderScreenButton1.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_button1,
                            R.style.reminder_screen_button1
                        )
                    )
                    reminderScreenButton2.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_button2,
                            R.style.reminder_screen_button2
                        )
                    )
                }
                ReminderType.IAP_REGISTRATION_ASK_REMINDER -> {
                    reminderScreenTextH21.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_text_h2_1,
                            R.style.reminder_screen_text_h21
                        )
                    )
                    reminderScreenTextH1.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_text_h1,
                            R.style.reminder_screen_text_h1
                        )
                    )
                    reminderScreenTextH22.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_text_h2_2,
                            R.style.reminder_screen_text_h22
                        )
                    )
                    reminderScreenTextH31.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_text_h3_1,
                            R.style.reminder_screen_text_h3
                        )
                    )
                    reminderScreenTextH32.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_text_h3_2,
                            R.style.reminder_screen_text_h3
                        )
                    )
                    reminderScreenButton1.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_button1,
                            R.style.reminder_screen_button1
                        )
                    )
                    reminderScreenButton2.setTextOrHide(
                        formatText(
                            R.string.iap_registration_ask_reminder_button2,
                            R.style.reminder_screen_button2
                        )
                    )
                }
            }
            reminderScreenButton1.setOnClickListener(onClickListener)
            reminderScreenButton2.setOnClickListener(onClickListener)
            reminderScreenClose.setOnClickListener(onClickListener)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // bind
        arguments?.let {
            reminderType = ReminderType.values()[it.getInt(reminderTypeArgument)]
            isPhone = it.getBoolean(isPhoneArgument)
        }
            ?: throw Exception("Use showIfConditionsAreMet() method to create object to this fragment")
        activity?.apply {
            reminderScreenViewModel.events.observe(this, Observer {
                when (it) {
                    ReminderScreenViewModel.Event.SIGN_IN -> {
                        PaywallService.getConnector().showSignInScreen(activity?.supportFragmentManager, AuthIntentBuilder().build(), null, PaywallConstants.WallType.REMINDER_PAYWALL, false, null)
                        dismiss()
                    }
                    ReminderScreenViewModel.Event.CLOSE -> dismiss()
                    else -> {
                        // no op
                    }
                }
            })
            if (isPhone) {
                arguments?.putInt(activityOrientationArgument, requestedOrientation)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
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
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
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

    override fun onDestroy() {
        // unbind
        activity?.apply {
            reminderScreenViewModel.events.removeObservers(this)
            reminderScreenViewModel.events.value = null
            if (isPhone) {
                requestedOrientation = arguments?.getInt(activityOrientationArgument)
                    ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (PaywallService.getInstance() != null && PaywallService.getInstance().isWpUserLoggedIn) {
            dismiss()
        }
        context?.let {
            val prefStorage = ReminderScreenSharedPreferenceStorage.getInstance(it)
            when (reminderType) {
                ReminderType.IAP_REGISTRATION_ASK -> prefStorage.iapRegistrationAskShownTime =
                    SystemClock.elapsedRealtime()
                ReminderType.IAP_REGISTRATION_ASK_REMINDER -> prefStorage.iapRegistrationAskReminderShownTime =
                    SystemClock.elapsedRealtime()
            }
        }
    }

    override fun dismiss() {
        fragmentManager?.popBackStack()
        super.dismiss()
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

    private fun formatText(@NonNull stringId: Int, @NonNull appearance: Int): CharSequence {
        return formatText(getString(stringId), appearance)
    }

    private fun formatText(@NonNull text: String, @NonNull appearance: Int): CharSequence {
        val builder = SpannableStringBuilder()
        if (TextUtils.isEmpty(text)) {
            return builder
        }
        builder.append(text)
        builder.setSpan(
            WpTextAppearanceSpan(context, appearance),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return builder
    }

    companion object {

        private const val TAG = "reminder"

        @JvmStatic
        fun showIfConditionsAreMet(
            @NonNull context: Context,
            @NonNull reminderType: ReminderType,
            @NonNull containerViewId: Int,
            @NonNull fragmentManager: FragmentManager,
            @NonNull isPhone: Boolean,
            @NonNull preferenceStorage: ReminderScreenStorage,
            @NonNull reminderScreenConfig: ReminderScreenConfig,
            @NonNull bypassElapsedTimeChecks: Boolean = false
        ): ReminderScreenFragment? {
            if (!reminderScreenConfig.enabled || !isConnectedOrConnecting(context)) {
                return null
            }
            return if (ReminderScreenViewModel.areConditionsMet(
                    reminderType,
                    preferenceStorage,
                    reminderScreenConfig,
                    bypassElapsedTimeChecks
                )
            ) {
                // Remove existing fragment
                val ft = fragmentManager.beginTransaction();
                val prev = fragmentManager.findFragmentByTag(TAG);
                if (prev != null) {
                    ft.remove(prev);
                }

                // Add new fragment
                ReminderScreenFragment().apply {
                    arguments = Bundle().apply {
                        putInt(reminderTypeArgument, reminderType.ordinal)
                        putBoolean(isPhoneArgument, isPhone)
                    }
                    fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                        .add(containerViewId, this)
                        .addToBackStack(null)
                        .commit()
                }
            } else {
                null
            }
        }
    }
}