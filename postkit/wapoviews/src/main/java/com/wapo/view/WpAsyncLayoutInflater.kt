package com.wapo.view

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.core.util.Pools
import java.util.concurrent.ArrayBlockingQueue

@Deprecated("Use OkLayoutInflater instead")
class WpAsyncLayoutInflater(context: Context) {

    private val mHandlerCallback = Handler.Callback { msg ->
        val request = msg.obj as InflateRequest
        if (request.view == null) {
            request.view = mInflater.inflate(request.resid, request.parent, false)
        }
        request.onFinishCallback?.onInflateFinished(request.view, request.resid, request.parent)
        mInflateThread.releaseRequest(request)
        true
    }

    private var mInflater: LayoutInflater = BasicInflater(context)
    private var mHandler: Handler = Handler(mHandlerCallback)
    private var mInflateThread: InflateThread = InflateThread.getInstance()

    @UiThread
    fun inflate(
        @LayoutRes resid: Int,
        parent: ViewGroup?,
        onFinishCallback: OnInflateFinishedListener,
        onInflateCallback: OnInflateListener?
    ) {
        val request = mInflateThread.obtainRequest()
        request.inflater = this
        request.resid = resid
        request.parent = parent
        request.onFinishCallback = onFinishCallback
        request.onInflateCallback = onInflateCallback
        mInflateThread.enqueue(request)
    }

    interface OnInflateListener {
        fun onInflate(view: View?, @LayoutRes resid: Int, parent: ViewGroup?)
    }

    interface OnInflateFinishedListener {
        fun onInflateFinished(view: View?, @LayoutRes resid: Int, parent: ViewGroup?)
    }

    class InflateRequest internal constructor() {
        var inflater: WpAsyncLayoutInflater? = null
        var parent: ViewGroup? = null
        var resid = 0
        var view: View? = null
        var onFinishCallback: OnInflateFinishedListener? = null
        var onInflateCallback: OnInflateListener? = null
    }

    private class BasicInflater internal constructor(context: Context?) :
        LayoutInflater(context) {
        override fun cloneInContext(newContext: Context): LayoutInflater {
            return BasicInflater(newContext)
        }

        @Throws(ClassNotFoundException::class)
        override fun onCreateView(name: String, attrs: AttributeSet): View {
            for (prefix in sClassPrefixList) {
                try {
                    val view = createView(name, prefix, attrs)
                    if (view != null) {
                        return view
                    }
                } catch (e: ClassNotFoundException) {
                    // In this case we want to let the base class take a crack
                    // at it.
                }
            }
            return super.onCreateView(name, attrs)
        }

        companion object {
            private val sClassPrefixList = arrayOf(
                "android.widget.", "android.webkit.", "android.app."
            )
        }
    }

    class InflateThread private constructor() : Thread() {
        private val mQueue = ArrayBlockingQueue<InflateRequest>(10)
        private val mRequestPool = Pools.SynchronizedPool<InflateRequest>(10)

        init {
            name = TAG
            start()
        }

        // Extracted to its own method to ensure locals have a constrained liveness
        // scope by the GC. This is needed to avoid keeping previous request references
        // alive for an indeterminate amount of time, see b/33158143 for details
        fun runInner() {
            val request = try {
                mQueue.take()
            } catch (ex: InterruptedException) {
                // Odd, just continue
                Logger.e(TAG, null, ex)
                return
            }
            try {
                request.view = request.inflater!!.mInflater.inflate(
                    request.resid, request.parent, false
                )
                request.onInflateCallback?.onInflate(request.view, request.resid, request.parent)
            } catch (ex: RuntimeException) {
                // Probably a Looper failure, retry on the UI thread
                Logger.e(
                    TAG, "Failed to inflate resource in the background! Retrying on the UI"
                            + " thread", ex
                )
            }
            Message.obtain(request.inflater!!.mHandler, 0, request).sendToTarget()
        }

        override fun run() {
            while (true) {
                runInner()
            }
        }

        fun obtainRequest(): InflateRequest {
            var obj = mRequestPool.acquire()
            if (obj == null) {
                obj = InflateRequest()
            }
            return obj
        }

        fun releaseRequest(obj: InflateRequest) {
            obj.onInflateCallback = null
            obj.onFinishCallback = null
            obj.inflater = null
            obj.parent = null
            obj.resid = 0
            obj.view = null
            mRequestPool.release(obj)
        }

        fun enqueue(request: InflateRequest) {
            try {
                mQueue.put(request)
            } catch (e: InterruptedException) {
                throw RuntimeException("Failed to enqueue async inflate request", e)
            }
        }

        companion object {
            @Volatile
            private var INSTANCE: InflateThread? = null

            @JvmStatic
            fun getInstance(): InflateThread =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: InflateThread().also { INSTANCE = it }
                }
        }
    }

    companion object {
        private const val TAG = "WpAsyncLayoutInflater"
    }
}