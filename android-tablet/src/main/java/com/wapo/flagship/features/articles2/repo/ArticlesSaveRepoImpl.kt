/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.articles2.repo

import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val WIDGET_FILES_DIR = "article_widgets"

class ArticlesSaveRepoImpl @Inject constructor(
    private val cacheFile: File
): ArticlesSaveRepo {

    private val MAX_FILE_AGE_MS = TimeUnit.HOURS.toMillis(24)

    override suspend fun saveArticles(widgetId: String, items: List<String>?) {
        withContext(Dispatchers.IO) {
            if (!widgetId.isBlank()) {

                val directory = File(cacheFile, WIDGET_FILES_DIR)
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                cleanupOldWidgetFiles(directory)

                val file = File(directory, widgetId)

                try {
                    if (items.isNullOrEmpty()) {
                        if (file.exists()) {
                            file.delete()
                        }
                    } else {
                        file.bufferedWriter(Charsets.UTF_8).use { out ->
                            items.forEach { item ->
                                out.write(item)
                                out.newLine()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


        }
    }

    override suspend fun getArticleUrlsById(widgetId: String): List<String> {
        if (widgetId.isBlank()) return emptyList()

        val directory = File(cacheFile, WIDGET_FILES_DIR)
        val file = File(directory, widgetId)

        return try {
            if (file.exists()) {
                file.bufferedReader(Charsets.UTF_8).useLines { it.toList() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun removeArticleUrls(widgetId: String) {
        withContext(Dispatchers.IO) {
            if (widgetId.isNotEmpty()) {
                val directory = File(cacheFile, WIDGET_FILES_DIR)
                if (directory.exists()) {
                    val file = File(directory, widgetId)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun cleanupOldWidgetFiles(directory: File) {
        try {
            val currentTime = System.currentTimeMillis()
            directory.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > MAX_FILE_AGE_MS) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Errors during cleanup should not crash the app.
            e.printStackTrace()
        }
    }
}
