package com.wapo.flagship.features.print

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.radaee.pdf.Global
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.data.ArchiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
@SuppressLint("NewApi")
class Pdf2ViewModel
    @Inject
    constructor(
        private val application: Application,
    ) : ViewModel() {
        val offscreenPageLimit = 1
        private var processNextItemJob: Job? = null
        private var isJobRunning = AtomicBoolean(false)
        private val requestQueue = ConcurrentLinkedDeque<PdfItem>()

        /**
         * Item to deliver currently loaded bitmap for a given position.
         * UI can observe and based on their positions.
         * Setting the max sizes for bitmaps to 3000 x 3000
         */
        private val _bitmapItem = LiveEvent<PdfBitmapItem>()
        val bitmapItem: LiveData<PdfBitmapItem> = _bitmapItem
        private val bitmapMaxSize = 3000f * 3000f
        private val placeholderPath: String = ArchiveManager.getPlaceholderPDFFilePath(application)
        private var placeholderBitmap: Bitmap? = null

        /**
         * LiveEvent to dispatch retry button event
         */
        private val _retryEvent = LiveEvent<Int>()
        val retryEvent: LiveEvent<Int> = _retryEvent

        fun init() {
            // Initializing Radaee
            Global.Init(application)
            Global.render_mode = 0
        }

    fun release() {
        // Release Radaee
        Global.RemoveTmp()

    }

    /**
     * Method to update _bitmapItem LiveData with a bitmap copy from placeholderBitmap
     */
    private fun showPlaceholder(itemPosition: Int) {
        getPlaceholderBitmap()?.let { bitmap ->
            val placeholderCopy = bitmap.config?.let { bitmap.copy(it, true) }
            _bitmapItem.postValue(
                PdfBitmapItem(
                    itemPosition,
                    placeholderCopy,
                    isPlaceholder = true
                )
            )
        }
    }

        /**
         * Method to create and cache placeholder bitmap in memory
         */
        private fun getPlaceholderBitmap(): Bitmap? {
            if (placeholderBitmap != null) return placeholderBitmap
            var pdfRenderer: PdfRenderer? = null
            var pdfPage: PdfRenderer.Page? = null
            try {
                val input =
                    ParcelFileDescriptor.open(
                        File(placeholderPath),
                        ParcelFileDescriptor.MODE_READ_ONLY,
                    )
                pdfRenderer = PdfRenderer(input)
                pdfPage = pdfRenderer.openPage(0)
                val pageBitmap = createBitmap(pdfPage.width, pdfPage.height)
                placeholderBitmap = pdfPage.render(pageBitmap)
            } finally {
                closeRendererAndPage(pdfPage, pdfRenderer)
            }
            return placeholderBitmap
        }


        private fun showError(itemPosition: Int) {
            _bitmapItem.postValue(PdfBitmapItem(itemPosition, null, isError = true))
        }

        fun loadPdf(
            position: Int,
            filePath: String?,
        ) {
            // Process offscreen pages for left and right sides and then one for current and one extra.
            // Removing other queued requests from head as they are already unloaded.
            while (requestQueue.size > (offscreenPageLimit + 2) + 1) {
                requestQueue.poll()
            }
            requestQueue.offer(PdfItem(position, filePath))
            showPlaceholder(position)
            processNextItem()
        }

        private fun processNextItem() {
            if (!isJobRunning.get() && requestQueue.size > 0) {
                isJobRunning.set(true)
                processNextItemJob =
                    CoroutineScope(Dispatchers.IO).launch {
                        // Process last offered one first as that should be the latest one.
                        processItem(requestQueue.pollLast())
                        withContext(Dispatchers.Main) {
                            isJobRunning.set(false)
                            if (requestQueue.size > 0) processNextItem()
                        }
                    }
            }
        }

        private suspend fun processItem(item: PdfItem) {
            if (item.filePath.isNullOrEmpty()) {
                showPlaceholder(item.position)
            } else if (!File(item.filePath).exists()) {
                showError(item.position)
            } else {
                var pdfRenderer: PdfRenderer? = null
                var pdfPage: PdfRenderer.Page? = null
                try {
                    val input =
                        ParcelFileDescriptor.open(
                            File(item.filePath),
                            ParcelFileDescriptor.MODE_READ_ONLY,
                        )
                    pdfRenderer = PdfRenderer(input)
                    pdfPage = pdfRenderer.openPage(0)
                    val pageBitmap = createBitmap(pdfPage.width, pdfPage.height)
                    val finalBitmap = pdfPage.render(pageBitmap)
                    withContext(Dispatchers.Main) {
                        _bitmapItem.value = PdfBitmapItem(item.position, finalBitmap)
                    }
                } catch (oom: OutOfMemoryError) {
                    val info = ActivityManager.MemoryInfo()
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("PDF OOM Error")
                            setModule(LogModules.PRINT)
                            setErrorMessage(oom.message)
                            set("url", item.filePath)
                            set("total_memory", info.totalMem)
                            set("available_memory", info.availMem)
                        }.run {
                            RemoteLog.e(FlagshipApplication.getInstance(), build())
                        }
                    System.gc()
                    showError(item.position)
                } catch (e: Exception) {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("PDF Path Open Failed Error")
                            setModule(LogModules.PRINT)
                            setErrorMessage(e.message)
                            set("url", item.filePath)
                        }.run {
                            RemoteLog.e(FlagshipApplication.getInstance(), build())
                        }
                    showError(item.position)
                } finally {
                    closeRendererAndPage(pdfPage, pdfRenderer)
                }
            }
        }

        private fun PdfRenderer.Page.render(bitmap: Bitmap) =
            use {
                render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }

        private fun closeRendererAndPage(
            pdfPage: PdfRenderer.Page?,
            pdfRenderer: PdfRenderer?,
        ) {
            try {
                pdfPage?.close()
            } catch (_: Exception) {
            }
            try {
                pdfRenderer?.close()
            } catch (_: Exception) {
            }
        }

        private fun createBitmap(
            pageWidth: Int,
            pageHeight: Int,
        ): Bitmap {
            // Diving by 72 we get inches and multiplying by DPI we get target pixels.
            // In other words to match the quality of the printing device of the display we should
            // Handling the case to limit large size bitmaps while creating
            // In any case we will reduce the target size if the actual bitmap sizes are too large
            // increase the size of the image rendered as default PDF resolution is 72 DPI.
            // We should use AppContextUtils.getDeviceDpi() as targetPageDp but it creates really large images
            // on high end devices and throwing OOM errors.
            // So we are taking half of the dpi or 160 (mdpi) whichever is maximum and it is

            val targetPageDp = 160
            val bitmapWidth = targetPageDp * pageWidth / 72
            val bitmapHeight = targetPageDp * pageHeight / 72
            val actualBitmap = bitmapWidth * bitmapHeight
            var bitmap: Bitmap
            if (actualBitmap > bitmapMaxSize) {
                var timesLarger = 0f
                var reqPageWidth = 0f
                var reqPageHeight = 0f
                var reqBitmapW = 0
                var reqBitmapH = 0
                timesLarger = actualBitmap / bitmapMaxSize
                reqPageWidth = pageWidth / sqrt(timesLarger)
                reqPageHeight = pageHeight / sqrt(timesLarger)
                reqBitmapW = (targetPageDp * reqPageWidth / 72).toInt()
                reqBitmapH = (targetPageDp * reqPageHeight / 72).toInt()
                bitmap = Bitmap.createBitmap(reqBitmapW, reqBitmapH, Bitmap.Config.ARGB_8888)
                EventLog
                    .Builder()
                    .apply {
                        setMessage("PDF exceeding Bitmap Size")
                        setModule(LogModules.PRINT)
                        set("width", pageWidth)
                        set("height", pageHeight)
                        set("actual_bitmap_size", actualBitmap)
                    }.run {
                        RemoteLog.d(FlagshipApplication.getInstance(), build())
                    }
            } else {
                bitmap =
                    Bitmap.createBitmap(
                        bitmapWidth,
                        bitmapHeight,
                        Bitmap.Config.ARGB_8888,
                    )
            }
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            return bitmap
        }

        fun dispatchRetryEvent(itemPosition: Int) {
            _retryEvent.postValue(itemPosition)
        }

        override fun onCleared() {
            placeholderBitmap?.recycle()
            placeholderBitmap = null
            processNextItemJob?.cancel()
            processNextItemJob = null
            super.onCleared()
        }
    }

/**
 * Class to process the requests
 */
private data class PdfItem(
    val position: Int,
    val filePath: String?,
)

/**
 * Class to maintain loaded bitmap for the position.
 */
data class PdfBitmapItem(
    val position: Int,
    val bitmap: Bitmap?,
    val isError: Boolean = false,
    val isPlaceholder: Boolean = false,
)
