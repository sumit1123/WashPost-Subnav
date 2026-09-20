package com.wapo.flagship.views

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.widget.TextViewCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.features.mypost.fragments.MyPost2Fragment
import com.wapo.flagship.wapomain.MainConstants.ACTION_MY_POST
import com.washingtonpost.android.R

object SnackbarFactory {
    @JvmStatic
    fun networkProblem(
        container: View,
        context: Context,
        hasBottomNav: Boolean = true,
    ): Snackbar {
        val snackbar =
            Snackbar
                .make(
                    container,
                    R.string.alert_network_problems,
                    Snackbar.LENGTH_INDEFINITE,
                ).setAction(R.string.alert_settings) { Utils.openNetworkSettings(context) }
        snackbar.allowLongText()
        snackbar.setWhiteText()
        snackbar.setLayoutParams(hasBottomNav)
        return snackbar
    }

    fun verticalVideoError(
        isNetworkError: Boolean,
        container: View,
        context: Context,
    ): Snackbar {
        val snackbar = Snackbar.make(container, "", Snackbar.LENGTH_INDEFINITE)
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        snackbar.view.setBackgroundColor(
            ContextCompat.getColor(context, com.washingtonpost.android.sections.R.color.vertical_videos_error_view_background),
        )
        // Hide the default textView so we can add a custom layout that allows us to arrange icon where we want it
        snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).visibility = View.INVISIBLE

        val relativeLayout = RelativeLayout(context)

        // Create imageView for error icon
        val iconImageView = ImageView(context)
        val imageViewLayoutParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        iconImageView.setImageResource(R.drawable.vertical_video_error_icon)
        imageViewLayoutParams.topMargin =
            context.resources.getDimensionPixelSize(
                com.washingtonpost.android.sections.R.dimen.vertical_videos_error_icon_margin_top,
            )
        imageViewLayoutParams.marginStart =
            context.resources.getDimensionPixelSize(
                com.washingtonpost.android.sections.R.dimen.vertical_videos_error_icon_margin_start,
            )
        imageViewLayoutParams.alignWithParent = true
        imageViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        relativeLayout.addView(iconImageView, imageViewLayoutParams)

        // Create textView for error text
        val textView = TextView(context)
        val errorTextId = if (isNetworkError) R.string.vertical_video_error_offline else R.string.vertical_video_error_other
        val builder = SpannableStringBuilder()
        builder.append("Error: ")
        builder.setSpan(StyleSpan(Typeface.BOLD), 0, builder.length, 0)
        builder.append(context.resources.getText(errorTextId))
        textView.text = builder
        val textViewLayoutParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        textViewLayoutParams.topMargin =
            context.resources.getDimensionPixelSize(
                com.washingtonpost.android.sections.R.dimen.vertical_videos_error_text_vertical_margin,
            )
        textViewLayoutParams.bottomMargin =
            context.resources.getDimensionPixelSize(
                com.washingtonpost.android.sections.R.dimen.vertical_videos_error_text_vertical_margin,
            )
        textViewLayoutParams.marginStart =
            context.resources.getDimensionPixelSize(
                com.washingtonpost.android.sections.R.dimen.vertical_videos_error_text_margin_start,
            )
        textViewLayoutParams.marginEnd =
            context.resources.getDimensionPixelSize(
                com.washingtonpost.android.sections.R.dimen.vertical_videos_error_text_margin_end,
            )
        TextViewCompat.setTextAppearance(textView, com.washingtonpost.android.sections.R.style.vertical_videos_error)
        relativeLayout.addView(textView, textViewLayoutParams)

        (snackbar.view as Snackbar.SnackbarLayout).addView(relativeLayout)

        snackbar.setLayoutParams()
        return snackbar
    }

    @JvmStatic
    fun nightModeInfo(
        container: View,
        context: Activity,
    ): Snackbar {
        val nightModeManager = FlagshipApplication.getInstance().nightModeManager
        val isNightModeOn = nightModeManager.immediateNightModeStatus

        @StringRes val actionText = if (isNightModeOn) R.string.night_mode_off else R.string.night_mode_on
        val snackbar =
            Snackbar
                .make(
                    container,
                    context.resources.getString(R.string.enable_night_mode),
                    Snackbar.LENGTH_LONG,
                ).setDuration(7000)
                .setAction(context.resources.getString(actionText)) {
                    AppContext.setShowNightModeSnackbarOnStart(true)
                    nightModeManager.setUserExplicitlySelectedAMode()
                    nightModeManager.setNightModeStatus(!isNightModeOn)
                }

        snackbar.setWhiteText()
        snackbar.setLayoutParams()
        return snackbar
    }

    @JvmStatic
    fun authorFollow(
        container: View,
        followId: String?,
    ): Snackbar {
        val snackbar =
            Snackbar
                .make(
                    container,
                    container.context.resources.getString(R.string.author_follow_my_post),
                    Snackbar.LENGTH_SHORT,
                ).setDuration(4000)
        snackbar
            .setAction(com.washingtonpost.android.save.R.string.my_post) {
                val intent = IntentHelper.getMainActivityIntent(container.context)
                intent.action = ACTION_MY_POST
                intent.putExtra(MyPost2Fragment.FOLLOW_ID, followId)
                startActivity(container.context, intent, null)
            }.setActionTextColor(
                ContextCompat.getColor(container.context, com.washingtonpost.android.follow.R.color.snackbar_action_follow),
            )
        snackbar.setWhiteText()
        snackbar.setLayoutParams()
        return snackbar
    }

    private fun Snackbar.setWhiteText() {
        val tv = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(ContextCompat.getColor(context, R.color.white))
    }

    private fun Snackbar.allowLongText() {
        val tv = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tv.maxLines = 3
    }

    private fun Snackbar.setLayoutParams(hasBottomNav: Boolean = false) {
        this.view.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            if (hasBottomNav) {
//                this.anchorId = R.id.bottom_navigation TODO: Anchor to Compose bottom nav
                this.anchorGravity = Gravity.TOP
                this.gravity = Gravity.TOP
            }
        }
    }
}
