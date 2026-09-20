package com.wapo.flagship.external.foryouwidget.workers

import android.content.Context
import androidx.work.*
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.external.foryouwidget.data.updateWidgetRecommendations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import com.wapo.flagship.external.foryouwidget.data.ForYouWidgetMapper
import androidx.datastore.preferences.core.edit
import com.wapo.flagship.external.foryouwidget.data.LAST_UPDATE_TIME_KEY
import com.wapo.flagship.external.foryouwidget.data.WidgetGlanceStateDefinition
import com.washingtonpost.android.R
import kotlinx.coroutines.flow.first

private const val TAG = "ForYouWidgetUpdateWorker"

class ForYouWidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    val hourly = context.getString(R.string.for_you_widget_hourly_job)
    val medium = context.getString(R.string.for_you_widget_medium_job)
    val forceRefresh = context.getString(R.string.for_you_widget_force_refresh)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val updateType = inputData.getString("UPDATE_TYPE") ?: "HOURLY"
            Logger.d(TAG, "Starting $updateType widget update")

            val app = applicationContext as FlagshipApplication
            val repository = app.forYouFeedRepo
            val tenMinutesInMillis = 10 * 60 * 1000L

            when (updateType) {
                hourly, forceRefresh -> {
                    // Hourly or Force Refresh - Fetch a new set of recommendations
                    Logger.d(TAG, "Performing $updateType update - fetching fresh data")
                    repository.makeWidgetCall()

                    val firstPageRecommendations = repository.getCachedArticlesPage(0)

                    val initialWidgetItems = ForYouWidgetMapper.mapToWidgetItems(firstPageRecommendations)
                    updateWidgetRecommendations(
                        context = applicationContext,
                        recommendations = initialWidgetItems,
                        pageIndex = 0,
                        articleIndex = 0
                    )
                }
                medium -> {
                    // 20min - Rotate through sets of articles
                    // Skips if update took place in the last 10 minutes
                    // Get current page index from repository 
                    val dataStore = WidgetGlanceStateDefinition.getDataStore(applicationContext, "")
                    val lastUpdateTime = dataStore.data.first()[LAST_UPDATE_TIME_KEY] ?: 0
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastUpdateTime < tenMinutesInMillis) {
                        Logger.d(TAG, "Skipping $medium update - last update was ${(currentTime - lastUpdateTime) / 60000} minutes ago")
                        return@withContext Result.success()
                    }
                    val currentPage = getCurrentPageIndex()
                    val newPageIndex = (currentPage + 1)

                    val pageRecommendations = repository.getCachedArticlesPage(newPageIndex)
                    val widgetItems = ForYouWidgetMapper.mapToWidgetItems(pageRecommendations)
                    updateWidgetRecommendations(
                        context = applicationContext,
                        recommendations = widgetItems,
                        pageIndex = 0,
                        articleIndex = 0
                    )

                    Logger.d(TAG, "$medium update - rotated to page: $newPageIndex")
                    dataStore.edit { preferences ->
                        preferences[LAST_UPDATE_TIME_KEY] = currentTime
                    }
                }
            }


                Logger.d(TAG, "Widget update completed successfully: $updateType")
                        Result . success ()
        } catch (e: Exception) {
                Logger.e(TAG, "Failed to update widget: ${e.message}", e)
                Result.retry()
            }
        }
    
    // Helper function to get the current index of page being dsplayed
    private fun getCurrentPageIndex(): Int {
        val prefs = applicationContext.getSharedPreferences("widget_worker_prefs", Context.MODE_PRIVATE)
        val currentPage = prefs.getInt("current_page", 0)
        prefs.edit { putInt("current_page", currentPage) }
        return currentPage
    }
    companion object {
        private const val HOURLY_WORK_NAME = "ForYouWidget_Hourly"
        private const val MEDIUM_WORK_NAME = "ForYouWidget_Medium"
        private const val FORCE_REFRESH_WORK_NAME = "ForYouWidget_ForceRefresh"

        fun scheduleAllUpdates(context: Context) {
            scheduleHourlyUpdate(context)
            scheduleMediumUpdate(context)
            Logger.d(TAG, "Scheduled all widget update workers")
        }

        fun scheduleForceRefresh(context: Context) {
            val tag = context.getString(R.string.for_you_widget_force_refresh) // reuse your string for consistency

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ForYouWidgetUpdateWorker>()
                .setInputData(workDataOf("UPDATE_TYPE" to tag))
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                FORCE_REFRESH_WORK_NAME,                     // unique name for one-time job
                ExistingWorkPolicy.REPLACE,             // replace if one is already running
                workRequest
            )
        }

        private fun scheduleHourlyUpdate(context: Context) {
            val hourly = context.getString(R.string.for_you_widget_hourly_job)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ForYouWidgetUpdateWorker>(
                1, TimeUnit.HOURS
            )
                .setInputData(workDataOf("UPDATE_TYPE" to hourly))
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                HOURLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        private fun scheduleMediumUpdate(context: Context) {
            val medium = context.getString(R.string.for_you_widget_medium_job)
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ForYouWidgetUpdateWorker>(
                20, TimeUnit.MINUTES
            )
                .setInputData(workDataOf("UPDATE_TYPE" to medium))
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                MEDIUM_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelAllUpdates(context: Context?) {
            if (context != null){
                WorkManager.getInstance(context).apply {
                    cancelUniqueWork(HOURLY_WORK_NAME)
                    cancelUniqueWork(MEDIUM_WORK_NAME)
                }
                Logger.d(TAG, "Cancelled all widget update workers")
            }
        }
    }
}