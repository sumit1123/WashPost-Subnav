package com.wapo.flagship.features.articles3.views

import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.radaee.pdf.DIB
import com.radaee.pdf.Document
import com.radaee.pdf.Global
import com.radaee.pdf.Matrix
import com.radaee.pdf.Page
import com.wapo.flagship.features.articles3.models.ui.PdfUiModel
import com.wapo.flagship.features.print.InlinePdfActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

private sealed class PdfState {
    object Loading : PdfState()
    data class Success(val bitmap: Bitmap, val file: File) : PdfState()
    object Error : PdfState()
}

enum class PdfUiStyle {
    DEFAULT
}

@Composable
fun PdfView(
    item: PdfUiModel,
) {
    val context = LocalContext.current
    var pdfState by remember { mutableStateOf<PdfState>(PdfState.Loading) }

    LaunchedEffect(item.url) {
        Global.Init(context as ContextWrapper)
        val result = withContext(Dispatchers.IO) {
            try {
                val url = URL(item.url)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val file = File.createTempFile("temp", ".pdf", context.cacheDir)
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()

                val doc = Document()
                doc.Open(file.absolutePath, null)
                val page: Page = doc.GetPage(0)
                val cropBox = page.GetCropBox()
                val pageWidth = cropBox[2] - cropBox[0]
                val pageHeight = cropBox[3] - cropBox[1]

                val displayMetrics = context.resources.displayMetrics
                val targetHeightPx = (500 * displayMetrics.density).toInt()
                val targetWidthPx = (targetHeightPx * (pageWidth / pageHeight)).toInt()

                val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(0xFFFFFFFF.toInt())

                val scaleX = targetWidthPx / pageWidth
                val scaleY = targetHeightPx / pageHeight
                val matrix = Matrix(scaleX, -scaleY, -cropBox[0] * scaleX, (pageHeight + cropBox[1]) * scaleY)

                page.RenderPrepare(null as DIB?)
                page.RenderToBmp(bitmap, matrix)
                doc.Close()

                PdfState.Success(bitmap, file)
            } catch (e: Exception) {
                PdfState.Error
            }
        }
        pdfState = result
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = pdfState) {
            is PdfState.Loading -> {
                CircularProgressIndicator()
            }
            is PdfState.Success -> {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = "pdf",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(context, InlinePdfActivity::class.java).apply {
                                putExtra(InlinePdfActivity.PDF_PATH, state.file.absolutePath)
                            }
                            context.startActivity(intent)
                        },
                    contentScale = ContentScale.Fit
                )
            }
            is PdfState.Error -> {
                Text("Failed to load PDF")
            }
        }
    }
}