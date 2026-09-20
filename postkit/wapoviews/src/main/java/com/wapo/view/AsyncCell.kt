package com.wapo.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.wapo.android.commons.util.Logger

class AsyncCell(
    val cont: Context,
    val attrs: AttributeSet?,
    val defStyleAttr: Int
) : FrameLayout(cont, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val okLayoutInflater by lazy { OkLayoutInflater(this) }
    private val placeHolderHeight = context.resources.getDimensionPixelSize(R.dimen.async_cell_placeholder_height)

    init {
        layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private var isInflated = false
    private val bindingFunctions: MutableList<AsyncCell.() -> Unit> = mutableListOf()
    private var layoutId: Int = -1
    private var viewType: Int = -1
    private var enableRiffleEffect = false

    fun setLayoutId(@LayoutRes layoutId: Int) {
        this.layoutId = layoutId
    }

    fun setViewType(viewType: Int) {
        this.viewType = viewType
    }

    fun enableRiffleEffect(enable: Boolean) {
        enableRiffleEffect = enable
    }

    fun getLayoutView(): View? {
        return if (childCount > 0) getChildAt(0) else null
    }

    fun inflate(skipAsync: Boolean = false) {
        if (skipAsync) {
            LayoutInflater.from(this.context).inflate(layoutId, this, false).also { view ->
                Logger.d(TAG, "onInflateFinished(non-async), viewType=$viewType, view=$view")
                handlePreBinding(view)
                bindView()
            }
        } else {
            okLayoutInflater.inflate(layoutId, this) { inflatedView ->
                Logger.d(TAG, "onInflateFinished, viewType=$viewType, view=$inflatedView")
                handlePreBinding(inflatedView)
                bindView()
            }
        }
    }

    private fun handlePreBinding(view: View?) {
        isInflated = true
        addView(view)
        if (enableRiffleEffect) {
            view?.let { RippleHelper.addRippleEffectToView(it) }
        }
    }

    private fun bindView() {
        with(bindingFunctions) {
            forEach { it() }
            clear()
        }
    }

    fun bindWhenInflated(bindFunc: AsyncCell.() -> Unit) {
        if (isInflated) {
            bindFunc()
        } else {
            bindingFunctions.clear()
            bindingFunctions.add(bindFunc)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!isInflated) {
            setMeasuredDimension(measuredWidth, placeHolderHeight)
        }
    }

    companion object {
        private const val TAG = "AsyncCell"
    }
}