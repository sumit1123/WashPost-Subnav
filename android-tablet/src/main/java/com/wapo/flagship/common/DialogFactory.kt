package com.wapo.flagship.common

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.AppContext
import com.wapo.flagship.Utils
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import android.util.Log
import com.wapo.android.commons.util.ViewUtil.findActivity

object DialogFactory {
    private val TAG = DialogFactory::class.java.simpleName
    private const val AMAZON_STORE_URL_TEMPLATE = "amzn://apps/android?p=%s"
    private const val AMAZON_STORE_URL_WEB_TEMPLATE = "https://www.amazon.com/gp/mas/dl/android?p=%s"
    private const val PLAYSTORE_URL_TEMPLATE = "market://details?id=%s"
    private const val PLAYSTORE_URL_WEB_TEMPLATE = "https://play.google.com/store/apps/details?id=%s"

    @JvmStatic
    fun getReviewDialog(
        context: Context,
        onDismissListener: DialogInterface.OnDismissListener?,
    ): Dialog {
        var shouldNeverAskReviewAgain = false
        val builder = AlertDialog.Builder(context)
        builder.setMultiChoiceItems(
            arrayOf(context.getString(R.string.rate_app_dont_show)),
            booleanArrayOf(false),
        ) { _, _, isChecked ->
            shouldNeverAskReviewAgain = isChecked
        }
        builder.setTitle(R.string.rate_app_msg)
        builder.setPositiveButton(R.string.rate_app_yes) { _, _ ->
            if (shouldNeverAskReviewAgain) {
                AppContext.setShowRateMessage(false)
            }
            handleReviewYes(context, onDismissListener)
        }
        builder.setNegativeButton(R.string.rate_app_no) { _, _ ->
            if (shouldNeverAskReviewAgain) {
                AppContext.setShowRateMessage(false)
            }
            handleReviewNo(context, onDismissListener)
        }
        builder.setCancelable(true)
        return builder.create()
    }

    @JvmStatic
    fun getUpdateDialog(
        context: Context,
        canDismiss: Boolean,
        onDismissListener: DialogInterface.OnDismissListener?,
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.update_app_dialog_message_text)
        builder.setPositiveButton(R.string.update_app_dialog_download, null)
        if (canDismiss) {
            builder.setNegativeButton(R.string.night_mode_dialog_no, null)
        }
        builder.setOnDismissListener(onDismissListener)
        builder.setCancelable(canDismiss)
        val dialog = builder.create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                context.findActivity()?.apply {
                    if (!isFinishing) {
                        getIntent(this).apply {
                            try {
                                startActivity(this)
                            } catch (e: Exception) {
                                CrashWrapper.sendException(
                                    IllegalStateException("Error starting activity intent=$intent"),
                                )
                            }
                        }
                    }
                }
                if (canDismiss) {
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    @JvmStatic
    fun getUpdateRestriction(
        context: Context,
        canDismiss: Boolean,
        onDismissListener: DialogInterface.OnDismissListener?,
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.age_restriction_app_dialog_title)
        builder.setMessage(R.string.age_restriction_app_dialog_message)
        builder.setPositiveButton(R.string.age_restriction_app_dialog_update, null)

        builder.setOnDismissListener(onDismissListener)
        builder.setCancelable(canDismiss)
        val dialog = builder.create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                button.findActivity()?.apply {
                    if (!isFinishing) {
                        getIntent(this).apply {
                            try {
                                startActivity(this)
                            } catch (e: Exception) {
                                CrashWrapper.sendException(
                                    IllegalStateException("Error starting activity intent=$intent"),
                                )
                            }
                        }
                    }
                }
                if (canDismiss) {
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    @JvmStatic
    fun getErrorAgeRestriction(
        context: Context,
        canDismiss: Boolean,
        title: Int,
        message: Int,
        onDismissListener: DialogInterface.OnDismissListener?,
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok", null)

        builder.setOnDismissListener(onDismissListener)
        builder.setCancelable(canDismiss)
        val dialog = builder.create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                if (canDismiss) {
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    @JvmStatic
    fun getOsUpdateDialog(
        activity: Activity,
        message: String,
    ): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        builder.setOnDismissListener {
            activity.finish()
        }
        builder.setCancelable(false)
        return builder.create()
    }

    @JvmStatic
    fun getActiveSubscriberDialog(context: Context): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.already_subscribed_title)
        builder.setMessage(R.string.already_subscribed_message)
        builder.setPositiveButton(R.string.already_subscribed_cta) { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        return builder.create()
    }

    private fun handleReviewYes(
        context: Context,
        onDismissListener: DialogInterface.OnDismissListener?,
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.rate_app_msg_yes)
        builder.setPositiveButton(R.string.rate_app_yes) { _, _ ->
            val appStoreUrl = getAppStoreUrl(context)
            if (appStoreUrl != Uri.EMPTY) {
                context.startActivity(Intent(Intent.ACTION_VIEW, appStoreUrl))
            }
        }
        builder.setNegativeButton(R.string.rate_app_ask_me_later, null)
        builder.setOnDismissListener(onDismissListener)
        builder.setCancelable(true)
        val dialog = builder.create()
        dialog.show()
    }

    private fun handleReviewNo(
        context: Context,
        onDismissListener: DialogInterface.OnDismissListener?,
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.rate_app_msg_no)
        builder.setPositiveButton(R.string.rate_app_ok) { _, _ ->
            Utils.startWebActivity(context.getString(com.wapo.zendesk.R.string.zd_contact_us_url), context, false)
        }
        builder.setNegativeButton(R.string.rate_app_ask_me_later, null)
        builder.setOnDismissListener(onDismissListener)
        builder.setCancelable(true)
        val dialog = builder.create()
        dialog.show()
    }

    private fun getAppStoreUrl(context: Context): Uri? {
        try {
            return if (Utils.isAmazonBuild()) {
                Uri.parse(
                    String.format(
                        AMAZON_STORE_URL_TEMPLATE,
                        URLEncoder.encode(context.packageName, "UTF-8"),
                    ),
                )
            } else {
                Uri.parse(
                    String.format(
                        PLAYSTORE_URL_TEMPLATE,
                        URLEncoder.encode(context.packageName, "UTF-8"),
                    ),
                )
            }
        } catch (e: UnsupportedEncodingException) {
            Logger.e(TAG, Log.getStackTraceString(e))
        }
        return Uri.EMPTY
    }

    private fun getIntent(activity: Activity): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        if (Utils.isProductFlavorAmazon()) {
            setData(activity, intent, AMAZON_STORE_URL_TEMPLATE, AMAZON_STORE_URL_WEB_TEMPLATE)
        } else {
            setData(activity, intent, PLAYSTORE_URL_TEMPLATE, PLAYSTORE_URL_WEB_TEMPLATE)
        }
        return intent
    }

    private fun setData(
        activity: Activity,
        intent: Intent,
        storeUrl: String,
        webUrl: String,
    ) {
        val packageName = URLEncoder.encode(activity.packageName, "UTF-8")
        intent.data = Uri.parse(String.format(storeUrl, packageName))
        if (intent.resolveActivity(activity.packageManager) == null) {
            val ex = IllegalStateException("No activity to resolve intent data=${intent.data}")
            Logger.e(TAG, "Intent error", ex)
            CrashWrapper.sendException(ex)
            intent.data = Uri.parse(String.format(webUrl, packageName))
        }
    }
}
