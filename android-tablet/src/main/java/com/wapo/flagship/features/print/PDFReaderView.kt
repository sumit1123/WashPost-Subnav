package com.wapo.flagship.features.print

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.radaee.pdf.Document
import com.radaee.view.PDFView
import com.radaee.view.PDFVPage
import com.radaee.view.PDFViewVert

class PDFReaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PDFView.PDFViewListener {

    private var pdfView: PDFViewVert? = null
    private var doc: Document? = null

    fun openPdf(path: String) {
        doc = Document()
        doc?.Open(path, null)
        pdfView = PDFViewVert(context)
        pdfView?.vOpen(doc, 4, -0x333334, this)
        invalidate()
    }

    fun closePdf() {
        pdfView?.vClose()
        pdfView = null
        doc?.Close()
        doc = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pdfView?.vResize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pdfView?.vDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        pdfView?.vTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        pdfView?.vComputeScroll()
    }

    // PDFView.PDFViewListener implementation
    override fun OnPDFPageChanged(pageno: Int) {}
    override fun OnPDFDoubleTapped(x: Float, y: Float): Boolean = false

    override fun OnPDFSingleTapped(x: Float, y: Float): Boolean {
        val pos = pdfView?.vGetPos(x.toInt(), y.toInt()) ?: return false
        val page = doc?.GetPage(pos.pageno) ?: return false
        val annot = page.GetAnnotFromPoint(pos.x, pos.y)

        if (annot != null) {
            val uri = annot.GetURI()
            if (uri != null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    context.startActivity(intent)
                    return true // We handled the tap
                } catch (e: Exception) {
                }
            }

            val dest = annot.GetDest()
            if (dest >= 0) {
                pdfView?.vGotoPage(dest)
                return true
            }
        }
        return false
    }

    override fun OnPDFLongPressed(x: Float, y: Float) {}
    override fun OnPDFShowPressed(x: Float, y: Float) {}
    override fun OnPDFSelectEnd() {}
    override fun OnPDFFound(found: Boolean) {}
    override fun OnPDFInvalidate(post: Boolean) {
        if (post) {
            postInvalidate()
        } else {
            invalidate()
        }
    }
    override fun OnPDFPageDisplayed(canvas: Canvas?, vpage: PDFVPage?) {}
    override fun OnPDFSelecting(canvas: Canvas?, rect1: IntArray?, rect2: IntArray?) {}
    override fun OnPDFZoomStart() {}
    override fun OnPDFZoomEnd() {}
    override fun OnPDFPageRendered(pageno: Int) {}
}
