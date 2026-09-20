package com.washingtonpost.android.config.data.datasources.local

import android.content.Context
import androidx.annotation.RawRes
import com.wapo.android.commons.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.use
import java.io.File
import java.io.FileWriter

class FilesHelper(
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun readFromFile(
        filename: String,
        clearFileOnError: Boolean = true,
    ): String? = withContext(ioDispatcher) {
        try {
            val file = File(appContext.filesDir, filename)
            if (!file.exists()) return@withContext null
            file.inputStream().bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Logger.e(TAG, "Could not read file: $filename, error=$e")
            if (clearFileOnError) clearFile(filename)
            null
        }
    }

    suspend fun saveToFile(data: String, filename: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val file = File(appContext.filesDir, filename)
                if (file.exists()) file.delete()
                FileWriter(file).use { it.write(data) }
                true
            } catch (e: Exception) {
                Logger.e(TAG, "Could not save to file: $filename, data=$data, error=$e")
                false
            }
        }

    suspend fun clearFile(filename: String): Boolean = withContext(ioDispatcher) {
        try {
            val file = File(appContext.filesDir, filename)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Unable to delete file: $filename, error=$e")
            false
        }
    }

    suspend fun readFromResources(@RawRes resId: Int): String? = withContext(ioDispatcher) {
        try {
            val inputStream = appContext.resources.openRawResource(resId)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Logger.e(TAG, "Could not read resource: $resId, error=$e")
            null
        }
    }

    suspend fun readFromAssets(filename: String): String? = withContext(ioDispatcher) {
        try {
            val inputStream = appContext.assets.open(filename)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Logger.e(TAG, "Could not read asset: $filename, error=$e")
            null
        }
    }

    companion object {
        private const val TAG = "FilesHelper"
    }
}