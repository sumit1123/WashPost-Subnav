package com.wapo.zendesk.repository

import android.net.Uri
import androidx.annotation.WorkerThread
import com.wapo.zendesk.ZendeskProvider
import com.wapo.zendesk.model.TicketForm
import com.wapo.zendesk.network.ZendeskTicketFormsRequest
import com.washingtonpost.android.volley.DefaultRetryPolicy
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import zendesk.support.CreateRequest
import zendesk.support.Request
import zendesk.support.Support
import zendesk.support.UploadResponse
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ZendeskRepository(private val zendeskProvider: ZendeskProvider) {
    private val retryPolicy = DefaultRetryPolicy(5000, -1, 0f)

    @WorkerThread
    suspend fun getTicketForms(): List<TicketForm>? =
        suspendCancellableCoroutine { cont ->
            val zendeskTicketFormsRequest = ZendeskTicketFormsRequest(
                "${zendeskProvider.config.url}/api/v2/ticket_forms.json?active=true",
                {
                    if (cont.isActive) {
                        cont.resume(it.ticketForms)
                    }
                },
                {
                    if (cont.isActive) {
                        cont.resumeWithException(it)
                    }
                })
            zendeskTicketFormsRequest.retryPolicy = retryPolicy
            zendeskProvider.requestQueue.add(zendeskTicketFormsRequest)
        }

    @WorkerThread
    suspend fun uploadImage(uri: String, fileName: String, mimeType: String): String? =
        suspendCancellableCoroutine { cont ->
            val tempFile = saveToTempFile(uri, fileName)
            if (tempFile == null) {
                cont.resumeWithException(NullPointerException("temp file is null"))
                return@suspendCancellableCoroutine
            }
            val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
            if (uploadProvider != null) {
                uploadProvider.uploadAttachment(fileName, tempFile, mimeType, object :
                    ZendeskCallback<UploadResponse>() {
                    override fun onSuccess(uploadResponse: UploadResponse?) {
                        tempFile.delete()
                        cont.resume(uploadResponse?.token)
                    }

                    override fun onError(errorResponse: ErrorResponse?) {
                        tempFile.delete()
                        cont.resumeWithException(IllegalStateException(errorResponse?.reason))
                    }
                })
            } else {
                tempFile.delete()
                cont.resumeWithException(NullPointerException("Upload provider is null"))
            }
        }

    @WorkerThread
    suspend fun createRequest(createRequest: CreateRequest): Result? =
        suspendCancellableCoroutine { cont ->
            val provider = Support.INSTANCE.provider()
            if (provider == null) {
                cont.resumeWithException(NullPointerException("Provider is null"))
                return@suspendCancellableCoroutine
            }
            provider.requestProvider()
                .createRequest(createRequest, object : ZendeskCallback<Request>() {
                    override fun onSuccess(p0: Request?) {
                        cont.resume(Result.Success)
                    }

                    override fun onError(error: ErrorResponse?) {
                        cont.resume(Result.Error(error?.reason ?: ""))
                    }
                })
        }

    private fun saveToTempFile(uri: String, fileName: String): File? {
        return try {
            val cacheDir = zendeskProvider.cacheDir
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            file.delete()
            file.outputStream().use { oStream ->
                zendeskProvider.contentResolver.openInputStream(Uri.parse(uri)).use { inStream ->
                    inStream?.copyTo(oStream)
                }
            }
            file
        } catch (t: Throwable) {
            null
        }
    }
}