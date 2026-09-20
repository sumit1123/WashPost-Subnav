package com.wapo.flagship.features.print

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.radaee.pdf.Global

class InlinePdfActivity : ComponentActivity() {

    companion object {
        const val PDF_PATH = "pdf_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Global.Init(this)

        val pdfPath = intent.getStringExtra(PDF_PATH)

        setContent {
            if (pdfPath != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        // Manually instantiate the view instead of using findViewById
                        PDFReaderView(context).apply {
                            openPdf(pdfPath)
                        }
                    },
                    onRelease = { view ->
                        view.closePdf()
                    }
                )
            }
        }
    }
}