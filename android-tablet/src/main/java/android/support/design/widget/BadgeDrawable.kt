package android.support.design.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.wapo.flagship.AppContext
import com.washingtonpost.android.R

class BadgeDrawable(
    val context: Context,
) : Drawable() {
    private var mBadgePaint = Paint()
    private var mTextPaint = Paint()
    private var mTxtRect = Rect()

    private var mCount = ""
    private var mWillDraw = false

    private var mPadding: Float = 0f
    private var mTextSize = context.resources.getDimension(R.dimen.badge_text_size)

    init {
        mBadgePaint.color = ContextCompat.getColor(context, com.washingtonpost.android.sections.R.color.br_news_red)
        mBadgePaint.isAntiAlias = true
        mBadgePaint.style = Paint.Style.FILL

        mTextPaint.color = Color.WHITE
        mTextPaint.typeface = Typeface.DEFAULT_BOLD
        mTextPaint.textSize = mTextSize
        mTextPaint.isAntiAlias = true
        mTextPaint.textAlign = Paint.Align.CENTER

        mPadding =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                0.0f,
                context.resources.displayMetrics,
            )
    }

    override fun draw(canvas: Canvas) {
        if (!mWillDraw) {
            return
        }

        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top

        val tw = mTextPaint.measureText(mCount)
        val th = mTextPaint.descent() - mTextPaint.ascent()

        // Position the badge in the top-right quadrant of the icon.
        val radius = (Math.sqrt((tw * tw + th * th).toDouble()) / 2 + mPadding).toFloat()
        val centerX = width - radius
        val centerY = radius

        // Draw badge circle.
        canvas.drawCircle(centerX, centerY, radius, mBadgePaint)

        if ((AppContext.getAlertsLaunchCount() != 0) && Integer.valueOf(mCount) > 0) {
            // Draw badge count text inside the circle.
            mTextPaint.getTextBounds(mCount, 0, mCount.count(), mTxtRect)
            val textHeight = mTxtRect.bottom - mTxtRect.top
            val textY = centerY + (textHeight / 2f)
            canvas.drawText(mCount, centerX, textY, mTextPaint)
        }
    }

    fun setCount(count: Int) {
        mCount = Integer.toString(count)

        // Only draw a badge if there are notifications.
        mWillDraw = count > 0 || ((AppContext.getAlertsLaunchCount() == 0) && count > -1)
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.UNKNOWN

    override fun setColorFilter(cf: ColorFilter?) {
    }
}
