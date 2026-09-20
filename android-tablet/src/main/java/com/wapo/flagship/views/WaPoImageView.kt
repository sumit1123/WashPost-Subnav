package com.wapo.flagship.views

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.ImageView
import com.wapo.flagship.content.image.ImageService
import com.washingtonpost.android.wapocontent.ImageRequestData
import com.washingtonpost.android.wapocontent.Priority
import rx.Observable
import rx.Subscription

class WaPoImageView : ImageView {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private var url: String? = null
    private var subscription: Subscription? = null

    private var layoutListener: LayoutListener? = null

    fun setImageUrl(
        url: String,
        imageService: ImageService,
    ) {
        this.url = url
        subscription?.unsubscribe()

        val sizesObservable =
            Observable.create<Pair<Int, Int>>({ subscriber ->
                layoutListener =
                    object : LayoutListener {
                        override fun onLayout(
                            left: Int,
                            top: Int,
                            right: Int,
                            bottom: Int,
                        ) {
                            subscriber.onNext(Pair(right - left, bottom - top))
                        }
                    }
            })

        subscription =
            sizesObservable
                .filter { it.first > 0 && it.second > 0 }
                .take(1)
                .flatMap { size ->
                    val requestData = ImageRequestData(url, size.first, size.second)
                    requestData.priority = Priority(Priority.Group.BACKGROUND, 1)
                    imageService.getImage(requestData)
                }.subscribe(
                    { imageResponse ->
                        if (imageResponse.data is Bitmap) {
                            setImageBitmap(imageResponse.data as Bitmap)
                        }
                    },
                    {
                        setImageDrawable(null)
                    },
                )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscription?.unsubscribe()
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        layoutListener?.onLayout(left, top, right, bottom)
    }

    interface LayoutListener {
        fun onLayout(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        )
    }
}
