package com.wapo.view.segmentedview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.viewpager.widget.ViewPager
import com.wapo.view.R

class SegmentedView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    class Button(val text: CharSequence)

    interface SegmentSelectedListener {
        fun onSegmentSelected(position: Int)
    }

    private val selectedPaint: Paint
    private val selectedRect = RectF()
    private val divider = ResourcesCompat.getDrawable(context.resources, R.drawable.segmented_divider, null)!!
    private var textAppearanceNormal: Int
    private var textAppearanceSelected: Int

    init {

        val attr = context.theme.obtainStyledAttributes(attrs, R.styleable.SegmentedView, defStyleAttr, 0)
        try {
            textAppearanceNormal = attr.getResourceId(R.styleable.SegmentedView_textAppearanceNormal, R.style.SegmentedButtonNormal)
            textAppearanceSelected = attr.getResourceId(R.styleable.SegmentedView_textAppearanceSelected, R.style.SegmentedButtonSelected)
        } finally {
            attr.recycle()
        }

        gravity = Gravity.CENTER
        orientation = HORIZONTAL
        setBackgroundResource(R.drawable.segmented_view_bg)
        selectedPaint = Paint()
        selectedPaint.color = ContextCompat.getColor(context, R.color.live_image_segmented_tab_selected)

        if (isInEditMode) {
            addButton(Button("Segmented button 1"))
            addButton(Button("Segmented button 2"))
            addButton(Button("Segmented button 3"))
        }
    }

    var selectedPosition: Int = -1
        set(value) {
            field = value
            for (i in 0 until childCount) {
                val textView = getChildAt(i).findViewById<TextView>(android.R.id.text1)
                if (i == field) {
                    TextViewCompat.setTextAppearance(textView, textAppearanceSelected)
                } else {
                    TextViewCompat.setTextAppearance(textView, textAppearanceNormal)
                }
            }
        }

    var segmentSelectedListener: SegmentSelectedListener? = null

    val buttons = mutableListOf<Button>()

    fun addButton(button: Button) {
        buttons.add(button)
        val view = LayoutInflater.from(context).inflate(R.layout.segmented_button, this, false)
        view.setOnClickListener {
            if (buttons.indexOf(button) != selectedPosition) {
                selectedPosition = buttons.indexOf(button)
                segmentSelectedListener?.onSegmentSelected(selectedPosition)
                invalidate()
            }
        }
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = button.text
        TextViewCompat.setTextAppearance(textView, textAppearanceNormal)
        addView(view)

        if (childCount == 1) {
            selectedPosition = 0
        }
    }

    fun setupWithViewPager(viewPager: ViewPager) {
        segmentSelectedListener = object : SegmentSelectedListener {
            override fun onSegmentSelected(position: Int) {
                viewPager.currentItem = position
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                selectedPosition = position
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        val adapter = viewPager.adapter
        this.buttons.clear()
        removeAllViews()
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                addButton(Button(adapter.getPageTitle(i) ?: ""))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (selectedPosition < 0 || childCount == 0 || selectedPosition > childCount - 1) return
        val selectedView = getChildAt(selectedPosition)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
        selectedRect.set(selectedView.left.toFloat() + padding, selectedView.top.toFloat() + padding, selectedView.right.toFloat() - padding, selectedView.bottom.toFloat() - padding)
        canvas.drawRoundRect(selectedRect, 16f.dp, 16f.dp, selectedPaint)

        drawDividers(canvas)
    }

    private fun drawDividers(canvas: Canvas) {
        if (childCount > 1) {
            for (i in 1 until childCount) {
                if (i != selectedPosition && i - 1 != selectedPosition) {
                    val leftView = getChildAt(i - 1)
                    val rightView = getChildAt(i)
                    val centerX = (leftView.right + rightView.left) / 2
                    val centerY = (leftView.top + leftView.bottom) / 2
                    divider.setBounds(
                            centerX - divider.intrinsicWidth / 2,
                            centerY - divider.intrinsicHeight / 2,
                            centerX + divider.intrinsicWidth / 2,
                            centerY + divider.intrinsicHeight / 2)
                    divider.draw(canvas)
                }
            }
        }
    }

    private val Float.dp: Float
        get() =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)
}