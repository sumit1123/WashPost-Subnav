package com.wapo.flagship.features.gifting.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.app.AlertDialog
import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wapo.android.commons.util.animateAlpha
import com.wapo.android.commons.util.setClickSpan
import com.wapo.android.commons.util.setStyleSpan
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.gifting.events.UserEvent
import com.wapo.flagship.util.Share
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentGiftArticleBinding
import com.washingtonpost.android.paywall.PaywallService

class GiftSenderViewStateHelper(
    val binding: FragmentGiftArticleBinding,
    val context: Context,
) {
    private var fadeInOutAnimatorSet: AnimatorSet? = null
    private var fadeOutAnim: Animator? = null

    /**
     * Show loading dialog
     */
    fun showLoading() {
        binding.giftScreen.root.setVisible(false)
        binding.noGiftsLeftScreen.root.setVisible(false)
        binding.noSubScreen.root.setVisible(false)
        binding.notSignedInScreen.root.setVisible(false)
        binding.giftLoading.setVisible(true)
    }

    /**
     * Show Gift Article Dialog
     * [giftsRemaining] - Remaining gifts left for Month
     * [alreadyGifted] - If this article has been already gifted
     */
    fun showGifting(
        giftsRemaining: Int,
        alreadyGifted: Boolean,
        clickEvent: (UserEvent) -> Unit,
    ) {
        binding.giftScreen.root.setVisible(true)
        binding.noGiftsLeftScreen.root.setVisible(false)
        binding.noSubScreen.root.setVisible(false)
        binding.notSignedInScreen.root.setVisible(false)
        binding.giftLoading.setVisible(false)
        initGiftScreen(giftsRemaining, alreadyGifted, clickEvent)
    }

    /**
     * Show No Gifts Left Dialog
     */
    fun showNoGiftsLeft(clickEvent: (UserEvent) -> Unit) {
        binding.giftScreen.root.setVisible(false)
        binding.noGiftsLeftScreen.root.setVisible(true)
        binding.noSubScreen.root.setVisible(false)
        binding.notSignedInScreen.root.setVisible(false)
        binding.giftLoading.setVisible(false)
        initNoGiftsLeft(clickEvent)
    }

    /**
     * Show Gifting Failure Dialog
     * - shown if there is a Network Connection issue, Request Timeout, API Failure, etc.
     */
    fun showFailure(
        context: Context,
        clickEvent: (UserEvent) -> Unit,
    ) {
        AlertDialog
            .Builder(context)
            .setTitle(R.string.gift_failure_title)
            .setMessage(R.string.gift_failure_message)
            .setPositiveButton(R.string.dismiss) { dialog, _ ->
                dialog.dismiss()
            }.setNegativeButton(R.string.actions_contact_us) { _, _ ->
                clickEvent(UserEvent.ContactUs)
            }.setOnDismissListener {
                clickEvent(UserEvent.Dismiss)
            }.show()
    }

    /**
     * Show No Subscription Dialog
     */
    fun showNoSub(clickEvent: (UserEvent) -> Unit) {
        binding.giftScreen.root.setVisible(false)
        binding.noGiftsLeftScreen.root.setVisible(false)
        binding.noSubScreen.root.setVisible(true)
        binding.notSignedInScreen.root.setVisible(false)
        binding.giftLoading.setVisible(false)
        initNoSub(clickEvent)
    }

    /**
     * Show Not Signed In Dialog
     */
    fun showNotSignedIn(clickEvent: (UserEvent) -> Unit) {
        binding.giftScreen.root.setVisible(false)
        binding.noGiftsLeftScreen.root.setVisible(false)
        binding.noSubScreen.root.setVisible(false)
        binding.notSignedInScreen.root.setVisible(true)
        binding.giftLoading.setVisible(false)
        initNotSignedIn(clickEvent)
    }

    /**
     * Show Share dialog to Share Gift Article Bitly url
     */
    fun showNativeGiftShare(
        articleUrl: String?,
        shareUrl: String,
        isActionButton: Boolean,
        appSection: String?,
        context: Context,
        trackingInfo: OmnitureX?,
    ) {
        Share
            .Builder()
            .articleUrl(articleUrl)
            .shareUrl(shareUrl)
            .isGiftShare(true)
            .isActionButton(isActionButton)
            .appSection(appSection)
            .trackingInfo(trackingInfo)
            .build()
            .shareItem(context)
    }

    /**
     * Initialize UI for Gift or Gift Again Dialog.
     * [giftsRemaining] - Load Gift Remaining count into MessageText
     * [alreadyGifted] - Update title and message if gifting again
     * [onClick] - Send all clicks as UserEvents
     */
    private fun initGiftScreen(
        giftsRemaining: Int,
        alreadyGifted: Boolean,
        onClick: (UserEvent) -> Unit,
    ) {
        var titleText =
            if (alreadyGifted) {
                context.resources.getString(R.string.gift_again_title_text)
            } else {
                context.resources.getString(
                    R.string.gift_title_text,
                )
            }
        titleText = "$ICON_TITLE_SPACE $titleText"
        val messageText =
            when {
                alreadyGifted && giftsRemaining == 0 ->
                    context.resources.getString(
                        R.string.gift_again_no_gifts_message_text,
                    )
                alreadyGifted ->
                    context.resources.getQuantityString(
                        R.plurals.gift_again_message_text,
                        giftsRemaining,
                        giftsRemaining,
                    )
                else ->
                    context.resources.getQuantityString(
                        R.plurals.gift_message_text,
                        giftsRemaining,
                        giftsRemaining,
                    )
            }
        var buttonText =
            if (alreadyGifted) {
                context.resources.getString(R.string.gift_again_button_text)
            } else {
                context.resources.getString(
                    R.string.gift_button_text,
                )
            }
        buttonText = "$buttonText $ICON_TITLE_SPACE"
        val messageSpan = SpannableString(messageText)
        val boldText =
            when {
                !alreadyGifted && giftsRemaining == 1 -> "$giftsRemaining article"
                alreadyGifted && giftsRemaining == 1 -> "$giftsRemaining remaining article"
                else -> "$giftsRemaining articles"
            }
        messageSpan.setStyleSpan(messageText, boldText, R.style.gifting_message_bold_style, context)
        if (!alreadyGifted) {
            val linkText = context.resources.getString(R.string.learn_more_link_text)
            messageSpan.setClickSpan(messageText, linkText, com.washingtonpost.android.paywall.R.color.sign_in_text, context) {
                onClick(UserEvent.LearnMore)
            }
        }
        val giftIcon =
            VectorDrawableCompat.create(context.resources, R.drawable.gift_icon, context.theme)
        binding.giftScreen.title.setCompoundDrawablesWithIntrinsicBounds(giftIcon, null, null, null)
        binding.giftScreen.title.text = titleText
        binding.giftScreen.message.text = messageSpan
        binding.giftScreen.message.movementMethod = LinkMovementMethod.getInstance()
        binding.giftScreen.button.text = buttonText
        binding.giftScreen.button.setOnClickListener { onClick(UserEvent.Gift) }
        val shareIcon =
            VectorDrawableCompat.create(
                context.resources,
                R.drawable.icon_share_gift,
                context.theme,
            )
        binding.giftScreen.button.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            shareIcon,
            null,
        )
    }

    /**
     * Initialize UI for No Gifts left dialog
     * [onClick] - Send all clicks as UserEvents
     */
    private fun initNoGiftsLeft(onClick: (UserEvent) -> Unit) {
        val messageText = context.resources.getString(R.string.no_gift_message_text)
        val messageSpan = SpannableString(messageText)
        val linkText = context.resources.getString(R.string.learn_more_link_text)
        messageSpan.setClickSpan(messageText, linkText, com.washingtonpost.android.paywall.R.color.sign_in_text, context) {
            onClick(UserEvent.LearnMore)
        }

        binding.noGiftsLeftScreen.message.text = messageSpan
        binding.noGiftsLeftScreen.message.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Initialize UI for No Subscription
     * [onClick] - Send all clicks as UserEvents
     */
    private fun initNoSub(onClick: (UserEvent) -> Unit) {
        var titleText = context.resources.getString(R.string.no_sub_title_text)
        titleText = "$ICON_TITLE_SPACE $titleText"
        val messageText = context.resources.getString(R.string.no_sub_message_text)
        val linkText = context.resources.getString(R.string.learn_more_link_text)
        val messageSpan = SpannableString(messageText)
        val boldText = "10 articles"
        messageSpan.setStyleSpan(messageText, boldText, R.style.gifting_message_bold_style, context)
        messageSpan.setClickSpan(messageText, linkText, com.washingtonpost.android.paywall.R.color.sign_in_text, context) {
            onClick(UserEvent.LearnMore)
        }

        val giftIcon =
            VectorDrawableCompat.create(context.resources, R.drawable.gift_icon, context.theme)
        binding.noSubScreen.title.setCompoundDrawablesWithIntrinsicBounds(
            giftIcon,
            null,
            null,
            null,
        )
        binding.noSubScreen.title.text = titleText
        binding.noSubScreen.message.text = messageSpan
        binding.noSubScreen.message.movementMethod = LinkMovementMethod.getInstance()
        binding.noSubScreen.button.setOnClickListener { onClick(UserEvent.Paywall) }
        binding.noSubScreen.signInText.setSignInText(PaywallService.getInstance().isWpUserLoggedIn) {
            onClick(UserEvent.SignIn)
        }
    }

    /**
     * Initialize UI for No Subscription
     * [onClick] - Send all clicks as UserEvents
     */
    private fun initNotSignedIn(onClick: (UserEvent) -> Unit) {
        binding.notSignedInScreen.button.setOnClickListener { onClick(UserEvent.SignIn) }
        binding.notSignedInScreen.signInText.setSignInText(
            PaywallService.getInstance().isWpUserLoggedIn,
        ) {
            onClick(UserEvent.SignIn)
        }
    }

    /**
     * Animate height of BottomSheet and Fade In/Out of content
     */
    fun animateDailogState(
        dialog: BottomSheetDialog,
        updateState: () -> Unit,
    ) {
        // It is important to cancel AnimatorSet if the UI State changes faster than the length
        // of the animation otherwise there will be unwanted side effects (Flickering).
        fadeInOutAnimatorSet?.cancel()
        fadeInOutAnimatorSet = AnimatorSet()
        fadeOutAnim = binding.container.animateAlpha(1.0f, 0.0f, 200)
        // Once
        fadeOutAnim?.addListener({
            animateTransition(dialog) {
                updateState()
            }
        })
        val fadeInAnim =
            binding.container.animateAlpha(0.0f, 1.0f, 200, 500, AccelerateInterpolator())
        fadeInOutAnimatorSet?.play(fadeInAnim)?.after(fadeOutAnim)
        fadeInOutAnimatorSet?.start()
    }

    /**
     * Animate height of bottom sheet when the contents change due state change
     */
    private fun animateTransition(
        bottomSheetDialog: BottomSheetDialog,
        changeHeight: () -> Unit,
    ) {
        val bottomSheet =
            bottomSheetDialog.findViewById<ViewGroup>(
                com.google.android.material.R.id.design_bottom_sheet,
            )
        val coordinatorLayout =
            bottomSheetDialog.findViewById<CoordinatorLayout>(
                com.washingtonpost.android.R.id.coordinator,
            )
        if (bottomSheet != null && coordinatorLayout != null) {
            val transition = AutoTransition()
            transition.addTarget(bottomSheet)
            transition.interpolator = FastOutSlowInInterpolator()
            transition.duration = 300L
            TransitionManager.beginDelayedTransition(coordinatorLayout, transition)
            changeHeight()
            TransitionManager.endTransitions(bottomSheet)
        } else {
            changeHeight()
        }
    }

    fun cleanup() {
        fadeOutAnim?.removeAllListeners()
        fadeOutAnim = null
        fadeInOutAnimatorSet = null
    }

    companion object {
        private const val ICON_TITLE_SPACE = "  "
    }
}
