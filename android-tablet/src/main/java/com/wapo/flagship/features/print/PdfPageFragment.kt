package com.wapo.flagship.features.print

import android.annotation.SuppressLint
import android.graphics.RectF
import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.radaee.pdf.Document
import com.radaee.view.WapoPDFViewPager
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.audio.R
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.views.TouchZoomImageView
import com.washingtonpost.android.databinding.FragmentPdfPageBinding
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception

@AndroidEntryPoint
class PdfPageFragment :
    Fragment(),
    TouchZoomImageView.OnPhotoTapListener {
    private val pdf2ViewModel: Pdf2ViewModel by activityViewModels()

    private var _viewBinding: FragmentPdfPageBinding? = null
    private val viewBinding get() = _viewBinding!!

    private var item: PdfBitmapItem? = null
    private var position: Int? = 0
    private var filePath: String? = ""

    companion object {
        private const val TAG = "PdfFragment"

        @JvmStatic
        fun newInstance(bundle: Bundle): PdfPageFragment {
            var pdfFragment = PdfPageFragment()
            pdfFragment.arguments = bundle
            return pdfFragment
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentPdfPageBinding.inflate(layoutInflater)

        position = arguments?.getInt("position")
        filePath = arguments?.getString("filePath")

        position?.let { pdf2ViewModel.loadPdf(it, filePath) }
        pdf2ViewModel.bitmapItem.observe(viewLifecycleOwner) {
            if (it.position == position) {
                // Recycle bitmap from the previous item to release bitmap resources.
                // Usually placeholder bitmap is returned first and then the actual print pdf once
                // it is loaded. So this will release placeholder bitmap in that case.
                item?.bitmap?.recycle()
                item = it
                val animation = AlphaAnimation(0f, 1f)
                animation.duration = 150
                viewBinding.ivPage.setImageBitmap(it.bitmap)
                viewBinding.ivPage.startAnimation(animation)
                viewBinding.ivPage.setPhotoTapListener(this)
                viewBinding.retryContainer.visibility = if (it.isError) View.VISIBLE else View.GONE
                viewBinding.progressBarPdf.visibility = if (it.isPlaceholder) View.VISIBLE else View.GONE
            }
        }
        viewBinding.retryButton.setOnClickListener {
            if (AppContextUtils.isConnectingOrConnected()) {
                viewBinding.retryContainer.visibility = View.GONE
                viewBinding.progressBarPdf.visibility = View.VISIBLE
                pdf2ViewModel.dispatchRetryEvent(position ?: 0)
            } else {
                toastNetworkError()
            }
        }
        return viewBinding.root
    }

    override fun onPhotoTap(
        x: Float,
        y: Float,
    ) {
        try {
            val doc = Document()
            doc.Open(filePath, null)
            val pageWidth = doc.GetPageWidth(0)
            val pageHeight = doc.GetPageHeight(0)
            val page = doc.GetPage(0)
            page?.ObjsStart()
            val annotCount = page?.GetAnnotCount() ?: 0
            val rects = arrayListOf<RectF>()
            for (i in 0 until annotCount) {
                val annot = page?.GetAnnot(i)
                val rect = annot?.GetRect()
                rect?.let {
                    rects.add(RectF(it[0], it[1], it[2], it[3]))
                }
                Logger.d(
                    TAG,
                    "PdfDebug: position=$position, name=${annot?.GetURI()}, annotRect=${rect?.get(0)}:${
                        rect?.get(1)
                    }:${rect?.get(2)}:${rect?.get(3)}",
                )
            }

            val srcW = viewBinding.ivPage.measuredWidth
            val srcH = viewBinding.ivPage.measuredHeight
            val transformedX = getTransformedX(x, srcW, pageWidth)
            val transformedY = getTransformedY(y, srcH, pageHeight)

            val annot = page?.GetAnnotFromPoint(transformedX, transformedY)
            val uri: String? = annot?.GetURI()
            uri?.let {
                (activity as? WapoPDFViewPager.WapoReaderListener)?.onOpenURI(uri, true)
            }
            page.Close()
            doc.Close()
        } catch (e: Exception) {
            EventLog
                .Builder()
                .apply {
                    setMessage("PDF Path Open Failed Error")
                    setModule(LogModules.PRINT)
                    setErrorMessage(e.message)
                    set("url", filePath)
                }.run {
                    RemoteLog.e(FlagshipApplication.getInstance(), build())
                }
        }
    }

    private fun getTransformedX(
        eventX: Float,
        srcW: Int,
        pageWidth: Float,
    ): Float {
        // Radaee coordinates have its own sizes. Mapping iv coordinates to Radaee coordinates
        return (pageWidth * eventX) / srcW
    }

    private fun getTransformedY(
        eventY: Float,
        srcH: Int,
        pageHeight: Float,
    ): Float {
        // Radaee coordinates have its own sizes. Mapping iv coordinates to Radaee coordinates
        val transformedY = (pageHeight * eventY) / srcH
        // Radaee coordinates starts from bottom left and touch event coordinates from top left.
        // Mapping touch to Radaee coordinates.
        return Math.abs(pageHeight - transformedY)
    }

    private fun toastNetworkError() {
        Toast
            .makeText(
                requireContext(),
                requireContext().getString(R.string.audio_error_offline),
                Toast.LENGTH_SHORT,
            ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
        item?.bitmap?.recycle()
        item = null
    }
}
