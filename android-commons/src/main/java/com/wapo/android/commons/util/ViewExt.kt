package com.wapo.android.commons.util

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Extension to set View to Visible if visible = true. Note if false, this
 * is set to GONE which is used more often than INVISIBLE.
 */
fun View.setVisible(visible: Boolean) {
    this.visibility = if(visible) View.VISIBLE else View.GONE
}

/**
 * Extension to set View to Gone if gone = true
 */
fun View.setGone(gone: Boolean) {
    this.visibility = if(gone) View.GONE else View.VISIBLE
}

/**
 * Extension to set View to Invisible if invisible = true
 */
fun View.setInvisible(invisible:Boolean) {
    this.visibility = if(invisible) View.INVISIBLE else View.VISIBLE
}

fun Drawable.tint(color: Int): Drawable {
    val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
    return this.mutate().apply { setColorFilter(colorFilter) }
}

fun View.applySystemBarPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        // Apply the system bar heights as padding to the root view
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

        // Return CONSUMED so child views don't try to apply insets again
        WindowInsetsCompat.CONSUMED
    }
}