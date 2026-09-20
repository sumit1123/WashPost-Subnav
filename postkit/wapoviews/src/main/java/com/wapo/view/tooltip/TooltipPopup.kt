package com.wapo.view.tooltip

import android.content.Context
import android.content.res.Resources
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import com.wapo.android.commons.util.Logger
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.wapo.android.commons.util.ViewUtil.findActivity
import com.wapo.view.R
import java.lang.ref.WeakReference

/**
 * This class represents the actual tooltips that are being shown at different places throughout the app.
 */
class TooltipPopup(private val tooltipProperties: TooltipProperties,
                   activity: Context,
                   val tooltipListener: TooltipListener?) : PopupWindow.OnDismissListener {
    companion object {
        const val HIGHLIGHT_SHAPE_OVAL = 1
        const val HIGHLIGHT_SHAPE_RECTANGLE = 2
        const val TAG: String = "TooltipPopup"
    }

    val activityContext = WeakReference(activity)
    val context = activity.applicationContext

    // maxWidth in pixels
    private val maxWidth = context.resources.getDimension(R.dimen.tooltip_max_width).toInt()
    private val margin: Int = context.resources.getDimension(R.dimen.tooltip_margin_podcast).toInt()
    private lateinit var contentPopupWindow: PopupWindow
    private lateinit var arrowPopupWindow: PopupWindow
    private var rootView: ViewGroup? = null
    private lateinit var popupOverlay: TooltipPopupOverlay
    private lateinit var contentLayout: LinearLayout
    private lateinit var arrowLayout: LinearLayout
    private lateinit var contentView: LinearLayout
    private lateinit var arrowImageView: ImageView
    private var horizontalMargins: Int = context.resources.getDimension(R.dimen.tooltip_horizontal_margin).toInt()
    private var autoDismissible: Boolean = false
    private lateinit var anchorCenter: PointF
    private var arrowVerticalOffset: Int = context.resources.getDimension(R.dimen.tooltip_arrow_vertical_offset).toInt()
    private var contentLocation: PointF = PointF()
    private var arrowLocation: PointF = PointF()

    /**
     * Whenever layout changes occur, we need to pretty much redraw the entire tooltip to match the new anchor position
     * e.g. orientation change.
     */
    private var anchorViewLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        //Iff the pop up is showing, adjust it as per the recent layout changes.
        // show() method is basically used to redraw the entire view for the popup
        if (isShowing()) {
            show()
        }
    }

    init {
        initRootView()
        initContentPopupWindow()
        initArrowPopupWindow()
        initContentView()
        initArrowView()
        addGlobalLayoutChangeListener()
        tooltipListener?.onTooltipDisplayed()
    }

    /**
     * Whenever layout changes occur, we need to pretty much redraw the entire tooltip to match the new anchor position
     * e.g. orientation change.
     * Removed in [removeGlobalLayoutListener]
     */
    private fun addGlobalLayoutChangeListener() {
        (tooltipProperties.parentView ?: tooltipProperties.anchorView)?.viewTreeObserver?.let {
            if (it.isAlive) {
                it.addOnGlobalLayoutListener(anchorViewLayoutListener)
            }
        }
    }

    /**
     * Root view is currently only used for popup overlay which is currently not used.
     */
    private fun initRootView() {
        var view = tooltipProperties.anchorView?.rootView as? ViewGroup
        if (view?.childCount == 1 && view.getChildAt(0) is FrameLayout) {
            view = view.getChildAt(0) as ViewGroup
        }
        rootView = view
    }

    /**
     * Initialize arrow popup window (pointing to the subject of the popup window)
     */
    private fun initArrowPopupWindow() {
        arrowPopupWindow = PopupWindow(ContextThemeWrapper(activityContext.get(), R.style.PopupWindow)).apply {
            setBackgroundDrawable(null)
            isOutsideTouchable = true
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    /**
     * Initialize the content popup window. This is different than the arrow attached to the popup window.
     */
    private fun initContentPopupWindow() {
        contentPopupWindow = PopupWindow(ContextThemeWrapper(activityContext.get(), R.style.PopupWindow)).apply {
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, com.balysv.materialripple.R.color.transparent)))
            setOnDismissListener(this@TooltipPopup)
            isOutsideTouchable = true
            width = 0
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    /**
     * This function sets the content view of the [contentPopupWindow]
     */
    private fun initContentView() {
        val activityContext = activityContext.get() ?: return
        val layoutInflater = activityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        contentLayout = LinearLayout(activityContext).apply {
            setBackgroundColor(ContextCompat.getColor(context, com.balysv.materialripple.R.color.transparent))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
        }

        contentView = layoutInflater.inflate(R.layout.tooltip_content, contentLayout) as LinearLayout
        contentView.apply {
            findViewById<TextView>(R.id.textview)?.text = tooltipProperties.text
            findViewById<ImageButton>(R.id.closeBtn)?.setOnClickListener {
                dismissTooltip()
            }
            setOnClickListener { dismissTooltip() }
        }
        contentPopupWindow.contentView = contentLayout
    }

    /**
     * This function initializes the arrow view for pointing to the tooltip subject.
     */
    private fun initArrowView() {
        arrowImageView = ImageView(activityContext.get()).apply {
            setImageResource(R.drawable.tooltip_arrow)
            background = null
            if (tooltipProperties.tooltipData.gravity == Gravity.TOP) {
                rotation = 180f
            }
        }
        arrowLayout = LinearLayout(activityContext.get()).apply {
            background = null
            orientation = LinearLayout.VERTICAL
            if (!tooltipProperties.tooltipData.hideArrow) {
                addView(arrowImageView)
            }
        }
        arrowPopupWindow.contentView = arrowLayout
    }

    private fun initOverlay() {
        popupOverlay = TooltipPopupOverlay(context)
        val location = IntArray(2)
        val anchorView = tooltipProperties.anchorView
        val highlightShape = tooltipProperties.tooltipData.highlightShape
        tooltipProperties.anchorView?.getLocationOnScreen(location)
        val anchorRect = RectF(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0].plus(anchorView?.measuredWidth ?: 0)).toFloat(),
            (location[1].plus(anchorView?.measuredHeight ?: 0).toFloat())
        )
        if (highlightShape == HIGHLIGHT_SHAPE_OVAL) {
            popupOverlay.setCircle(anchorRect, (anchorView?.measuredWidth ?: 0).toFloat())
        } else if (highlightShape == HIGHLIGHT_SHAPE_RECTANGLE) {
            popupOverlay.setRectangle(anchorRect)
        }
        rootView?.addView(popupOverlay)
    }

    private fun getArrowYOffset(): Int {
        return if (tooltipProperties.tooltipData.gravity == Gravity.TOP) arrowVerticalOffset else - arrowVerticalOffset
    }

    private fun getVerticalMargin(): Int {
        return if (tooltipProperties.tooltipData.gravity == Gravity.TOP) tooltipProperties.tooltipData.verticalMargin else - tooltipProperties.tooltipData.verticalMargin
    }

    /**
     * Main function that is used to show the tooltip.
     */
    fun show() {
        val anchorView = tooltipProperties.anchorView
        val parentView = tooltipProperties.parentView
        val duration = tooltipProperties.tooltipData.duration
        anchorView?.post {
            if (anchorView.isShown && !isParentActivityFinishing()) {
                contentLocation = calculatePosition(contentLayout, false)
                val windowWidth = parentView?.measuredWidth ?: context.resources.displayMetrics.widthPixels
                contentPopupWindow.width = if (windowWidth > maxWidth) maxWidth else windowWidth
                contentPopupWindow.showAtLocation(
                    anchorView,
                    Gravity.NO_GRAVITY,
                    contentLocation.x.toInt(),
                    contentLocation.y.toInt() - getVerticalMargin()
                )
                contentLayout.post {
                    if (contentLayout.isShown) {
                        contentLocation = calculatePosition(contentLayout, false)
                        contentPopupWindow.apply {
                            isClippingEnabled = true
                            if (width >= maxWidth) {
                                width = maxWidth
                            }
                            update(
                                contentLocation.x.toInt(),
                                contentLocation.y.toInt() - getVerticalMargin(),
                                width,
                                height
                            )
                        }
                    }
                }

                arrowLocation = calculatePosition(arrowLayout, true)
                arrowPopupWindow.showAtLocation(
                    anchorView,
                    Gravity.NO_GRAVITY,
                    arrowLocation.x.toInt(),
                    arrowLocation.y.toInt() + getArrowYOffset() - getVerticalMargin()
                )
                arrowLayout.post {
                    if (arrowLayout.isShown) {
                        arrowLocation = calculatePosition(arrowLayout, true)
                        arrowPopupWindow.apply {
                            isClippingEnabled = true
                            update(
                                arrowLocation.x.toInt(),
                                arrowLocation.y.toInt() + getArrowYOffset() - getVerticalMargin(),
                                width,
                                height
                            )

                        }
                    }
                }
                //initOverlay()
                if (autoDismissible) {
                    Handler().postDelayed({
                        if (contentPopupWindow.isShowing) contentPopupWindow.dismiss()
                        if (arrowPopupWindow.isShowing) arrowPopupWindow.dismiss()
                    }, duration)
                }

            } else
                Logger.e(TAG, "Tooltip cannot be shown, root view is invalid or has been closed.")
        }
    }

    /**
     * Calculates the position to anchor the tooltip depending on the positions of the views provided in [tooltipProperties]
     * and required gravity of the views also provided in [tooltipProperties]
     */
    private fun calculatePosition(contentLayout: LinearLayout, isArrow: Boolean): PointF {
        val point = PointF()
        val parentView = tooltipProperties.parentView
        val anchorView = tooltipProperties.anchorView
        val gravity = tooltipProperties.tooltipData.gravity
        // Measuring contentLayout's width and height to find the correct x and y values.
        val atMostWidth = (parentView?.width ?: Resources.getSystem().displayMetrics.widthPixels).coerceAtMost(maxWidth)
        if (isArrow || atMostWidth == 0) {
            contentLayout.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        } else {
            contentLayout.measure(
                View.MeasureSpec.makeMeasureSpec(atMostWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        val parentViewLocation = IntArray(2)
        parentView?.getLocationInWindow(parentViewLocation)
        val location = IntArray(2)
        anchorView?.getLocationInWindow(location)
        val anchorRect = RectF(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0].plus(anchorView?.width ?: 0).toFloat()),
            (location[1].plus(anchorView?.height ?: 0).toFloat())
        )
        anchorCenter = PointF(anchorRect.centerX(), anchorRect.centerY())
        contentLayout.let {
            when {
                isArrow -> point.x = anchorCenter.x - it.measuredWidth / 2f
                parentView != null -> point.x = parentViewLocation[0].toFloat()
                else -> point.x = anchorCenter.x - maxWidth / 2
            }

            point.y = when (gravity) {
                Gravity.TOP -> {
                    anchorRect.top - it.measuredHeight.toFloat() - margin.toFloat()
                }
                else -> {
                    anchorRect.bottom + margin
                }
            }
        }
        return point
    }

    /**
     * Dismisses the tooltip content and the arrow pop up window.
     */
    fun dismissTooltip() {
        contentPopupWindow.dismiss()
    }

    /**
     * Call back for [PopupWindow.OnDismissListener]
     * This function makes sure that the global layout listener is removed so that any changes to the layout after dismissal do not trigger
     * any events that are not required anymore.
     * This also increments the priority so that the next tooltip with the incremented priority (+1) is shown.
     */
    override fun onDismiss() {
        /**
         * This can be directly added to [dismissTooltip] function.
         * Reason for doing it this way -
         * On devices less than or equals to Android 5, the arrow remains on the screen when tooltip is dismissed by outside touch
         */
        if (arrowPopupWindow.isShowing) {
            arrowPopupWindow.dismiss()
        }
        removeGlobalLayoutListener()
        TooltipPopupManager.get(context).updatePriority(tooltipProperties)
        if (::popupOverlay.isInitialized) {
            if (popupOverlay.isShown) {
                rootView?.removeView(popupOverlay)
            }
        }
        tooltipListener?.onTooltipFinished()
    }

    /**
     * This removed global layout listener that was added in [addGlobalLayoutChangeListener]
     */
    private fun removeGlobalLayoutListener() {
        val parentView = tooltipProperties.parentView
        val anchorView = tooltipProperties.anchorView
        (parentView ?: anchorView)?.viewTreeObserver?.let {
            if (it.isAlive) {
                it.removeOnGlobalLayoutListener(anchorViewLayoutListener)
            }
        }
    }

    /**
     * Returns true if the tooltip is currently being shown, false otherwise.
     */
    fun isShowing(): Boolean = ::contentPopupWindow.isInitialized && contentPopupWindow.isShowing && ::arrowPopupWindow.isInitialized && arrowPopupWindow.isShowing

    /**
     * Returns true when parent activity is already finished otherwise returns activities isFinishing status.
     */
    fun isParentActivityFinishing(): Boolean {
        if (::arrowLayout.isInitialized) {
            return arrowLayout.findActivity()?.isFinishing ?: true
        }
        return true
    }
}
