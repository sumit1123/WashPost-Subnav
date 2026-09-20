package com.wapo.fragment

import android.content.res.Configuration
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver.*
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.view.R
import kotlin.math.min

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var behaviorState = BottomSheetBehavior.STATE_EXPANDED

    override fun onResume() {
        updateDialog()
        super.onResume()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateDialog()
        super.onConfigurationChanged(newConfig)
    }

    private fun updateDialog() {
        dialog?.window?.apply {
            val width = min(
                resources.getDimension(R.dimen.bottom_sheet_max_width).toInt(),
                resources.displayMetrics.widthPixels
            )
            setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
            setLayout(width, LayoutParams.WRAP_CONTENT)
        }

        // BottomSheetDialogFragment is not showing full details by default.
        // swipe up to expand the dialog. So set peekHeight to dialog's height to fix that.
        view?.let { view ->
            view.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    view.post {
                        getBottomSheetBehavior()?.apply {
                            state = behaviorState
                            peekHeight = view.measuredHeight
                            isDraggable = isBottomSheetDraggable()
                        }
                    }
                }
            })
        }
    }

    fun getBottomSheetDialog(): BottomSheetDialog? {
        return dialog as? BottomSheetDialog
    }

    fun getBottomSheet(): ViewGroup? {
        return getBottomSheetDialog()?.findViewById(com.google.android.material.R.id.design_bottom_sheet)
    }

    fun getBottomSheetBehavior(): BottomSheetBehavior<ViewGroup>? {
        return getBottomSheet()?.let {
            BottomSheetBehavior.from(it)
        }
    }

    open fun isBottomSheetDraggable(): Boolean {
        return getBottomSheetBehavior()?.isDraggable ?: true
    }

    fun setBottomSheetDraggable(draggable: Boolean) {
        getBottomSheetBehavior()?.isDraggable = draggable
    }

    fun makeBackgroundTransparent() {
        dialog?.setOnShowListener {
            val bottomSheet: FrameLayout? = (it as? BottomSheetDialog)?.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.apply {
                this.setBackgroundColor(ContextCompat.getColor(context, com.balysv.materialripple.R.color.transparent))
            }
        }
    }
}
