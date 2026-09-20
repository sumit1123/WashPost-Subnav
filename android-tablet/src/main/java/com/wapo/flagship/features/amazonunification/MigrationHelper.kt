package com.wapo.flagship.features.amazonunification

import android.content.Context
import android.preference.PreferenceManager
import com.wapo.android.commons.util.Logger
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils
import com.wapo.flagship.data.CacheMetadataDb
import com.wapo.flagship.di.core.modules.DefaultOnDataMismatchAdapter
import com.wapo.flagship.features.amazonunification.database.RainbowAppDatabase
import com.wapo.flagship.features.amazonunification.database.model.UserPreferenceEntry
import com.wapo.flagship.features.amazonunification.models.RainbowArticle
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.mypost.SaveProviderImpl
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * Class to handle data migration from Rainbow to Classic.
 * Handles:
 * 1. Saved Stories table [CacheMetadataDb.Name] migration
 * 2. Deletes the rest of Rainbow's CacheMetadataDb table
 * 3. Deletes local files specific to Rainbow
 * 4. Deletes shared prefs specific to Rainbow
 */
object MigrationHelper {
    private var tag = MigrationHelper::class.java.simpleName
    private const val rainbowMetadataDbName = CacheMetadataDb.Name
    const val rainbowMetadataDbNewName = "Rainbow" + CacheMetadataDb.Name
    private const val RAINBOW_IMAGES_ROOT = "images"
    private const val RAINBOW_WEB_ROOT = "web"
    private const val RAINBOW_FILES_ROOT = "localfiles"
    private const val RAINBOW_ZIP_ROOT = "zips"
    private const val RAINBOW_ZSYNC_TMP = "zsynctmp"
    private val appContext: Context get() = FlagshipApplication.getInstance().applicationContext

    /**
     * Renames Rainbow's [rainbowMetadataDbName] if it is not done yet.
     * Both Classic and Rainbow apps are using the same Database [CacheMetadataDb.Name] names that
     * conflict with each other.
     * So should rename Rainbow's [CacheMetadataDb.Name] before Classic is trying to load
     * [CacheMetadataDb.Name] Database in the unification app.
     * Conditions for renaming:
     * 1. Platform/Flavor is Amazon
     * 2. First time Classic is launched with (com.washingtonpost.rainbow) id
     */
    fun renameRainbowDatabaseIfNotDoneYet(appContext: Context) {
        Logger.d(tag, "Unification: Rename Rainbow Database if not done yet")
        if (Utils.isProductFlavorAmazon() && PrefUtils.hasRenamedRainbowDatabase(appContext) == "false") {
            // Renaming Rainbow's Database files here to keep them and not to conflict with
            // Classic's files. Actual data migration calls can happen from an on-boarding entries
            // or from any specific parts of the app after this.
            checkAndRenameDatabase(appContext,rainbowMetadataDbName, rainbowMetadataDbNewName)
        }
    }

    /**
     * Renames Database and its journal files to the newNames.
     * Need to make sure this gets called only once after upgrade.
     */
    private fun checkAndRenameDatabase(
        appContext: Context,
        oldName: String,
        newName: String,
    ) {
        val oldDatabaseFile: File = appContext.getDatabasePath(oldName)
        val oldDatabaseJournal: File = appContext.getDatabasePath("$oldName-journal")

        Logger.d(tag, "Unification: Files Before renaming ${CacheMetadataDb.Name}")
        oldDatabaseFile.parentFile
            ?.listFiles()
            ?.forEach { Logger.d(tag, "Unification: file: ${it.absoluteFile}") }

        if (oldDatabaseFile.exists() || oldDatabaseJournal.exists()) {
            val newDatabaseFile: File = appContext.getDatabasePath(newName)
            val newDatabaseJournal: File = appContext.getDatabasePath("$newName-journal")

            if (oldDatabaseFile.exists()) {
                if (newDatabaseFile.exists()) {
                    newDatabaseFile.delete()
                }
                oldDatabaseFile.renameTo(newDatabaseFile)
            }

            if (oldDatabaseJournal.exists()) {
                if (newDatabaseJournal.exists()) {
                    newDatabaseJournal.delete()
                }
                oldDatabaseJournal.renameTo(newDatabaseJournal)
            }

            Logger.d(tag, "Unification: Files After renaming ${CacheMetadataDb.Name}")
            oldDatabaseFile.parentFile
                ?.listFiles()
                ?.forEach { Logger.d(tag, "Unification: file: ${it.absoluteFile}") }

            PrefUtils.renamedRainbowDatabase(appContext, "true")
        } else {
            PrefUtils.renamedRainbowDatabase(appContext, "na")
        }
    }

    /**
     * Migrates Saved Stories from Rainbow to Classic tables if it is not done yet.
     * Conditions for migration:
     * 1. Platform/Flavor is Amazon
     * 2. There are renamed Rainbow Database files in the storage.
     *    2.1. [renameRainbowDatabaseIfNotDoneYet] should be called first to set these.
     * 3. Migration is not done yet.
     */
    fun migrateRainbowSavedStoriesIfNotDoneYet(deleteDatabaseAfterDone: Boolean = true) {
        Logger.d(tag, "Unification: Migrating Saved stories if not done yet")
        if (Utils.isProductFlavorAmazon() &&
            PrefUtils.hasRenamedRainbowDatabase(appContext()) == "true" &&
            !PrefUtils.hasMigratedRainbowSavedStories(appContext())
        ) {
            migrateRainbowSavedStories(deleteDatabaseAfterDone)

            // set [PREF_HAS_MIGRATED_RAINBOW_SAVED_STORIES] flag.
            PrefUtils.migratedRainbowSavedStories(appContext())
        }
    }

    /**
     * Migrates Saved Stories from [rainbowMetadataDbNewName]'s [UserPreferenceEntry] data to
     * Classic's Saved Stories
     */
    private fun migrateRainbowSavedStories(deleteDatabaseAfterDone: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            Logger.d(tag, "Unification: Migrating Saved stories")
            val savedManager = SavedArticleManager.getInstance(SavedArticleManager.Params(SaveProviderImpl(appContext)))
            // Iterate table entries

            val entries =
                RainbowAppDatabase.getInstance(appContext()).userPreferenceEntryDao().getEntries()
            entries.forEach { entry ->
                // Parse binary item
                entry.binary?.let { binary ->
                    try {
                        // Construct Moshi Builder and parse binary
                        val article =
                            getMoshiBuilder()
                                .build()
                                .adapter(RainbowArticle::class.java)
                                .fromJson(binary.toString(Charsets.UTF_8))
                        article?.let {
                            Logger.d(
                                tag,
                                "Unification: ${entry.fullUrl}, " +
                                    "${article.title}, ${article.contentUrl}, ${article.sourceUrl}}",
                            )
                            // Save only when sourceUrl is not null
                            article.sourceUrl?.let { url ->
                                val lmt = System.currentTimeMillis()
                                val savedArticleModel =
                                    SavedArticleModel(url, lmt)
                                val metadataModel =
                                    MetadataModel(url, lmt).apply {
                                        imageURL = article.socialImage
                                        headline = article.title
                                        blurb = article.blurb
                                        // get byline and date from items list
                                        article.items?.forEach {
                                            when (it) {
                                                is ByLine -> {
                                                    byline = it.content
                                                }
                                                is Date -> {
                                                    publishedTime = it.content ?: article.published
                                                }
                                            }
                                        }
                                    }
                                Logger.d(
                                    tag,
                                    "Unification: url=$url, lmt=$lmt, title=${article.title}, " +
                                        "blurb=${article.blurb}, byline=${metadataModel.byline}, " +
                                        "pubTime=${metadataModel.publishedTime}",
                                )

                                // Add to Classic tables.
                                savedManager.addArticle(savedArticleModel, metadataModel)
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e(tag, "Unification: migrateRainbowSavedStories error for ${entry.fullUrl}", e)
                    }
                }
            }

            // Deleting Database file here once Saved Stories migration is done.
            if (deleteDatabaseAfterDone) {
                // This can be moved to a different place and can be called once all data migration work is done.
                deleteRainbowDatabaseFile()
            }
        }
    }

    private fun getMoshiBuilder(): Moshi.Builder =
        Moshi
            .Builder()
            .add(DefaultOnDataMismatchAdapter.newFactory(Item::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ByLine::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Date::class.java, null))
            .add(
                PolymorphicJsonAdapterFactory
                    .of(Item::class.java, "type")
                    .withSubtype(ByLine::class.java, "byline")
                    .withSubtype(Date::class.java, "date")
                    .withDefaultValue(Item("default")),
            )

    /**
     * Close and Delete Rainbow's Database.
     * It can be called once required data migration is done.
     */
    private fun deleteRainbowDatabaseFile() {
        Logger.d(tag, "Unification: Delete Rainbow Database")
        RainbowAppDatabase.getInstance(appContext()).close()
        appContext().deleteDatabase(rainbowMetadataDbNewName)
    }

    /**
     * Deletes unused files in rainbow's local file directories
     */
    fun deleteRainbowLocalFiles() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                FileUtils.cleanDirectory(getImageDir(appContext()))
            } catch (e: IOException) {
                Logger.e(tag, "Exception occurred while cleaning image dir", e)
            }

            try {
                FileUtils.cleanDirectory(getWebDir(appContext()))
            } catch (e: IOException) {
                Logger.e(tag, "Exception occurred while clearing web directory", e)
            }

            try {
                FileUtils.cleanDirectory(getZipTargetDir(appContext()))
            } catch (e: IOException) {
                Logger.e(tag, "Exception occurred while cleaning zip dir", e)
            }
            try {
                FileUtils.cleanDirectory(getZipZsyncTargetDir(appContext()))
            } catch (e: IOException) {
                Logger.e(tag, "Exception occurred while cleaning zsync dir", e)
            }
        }
    }

    private fun appContext(): Context = FlagshipApplication.getInstance().applicationContext

    private fun getWebDir(context: Context): File? {
        val target = getTargetDir(context)
        var webTargetDir: File? = null
        try {
            webTargetDir = File(target, RAINBOW_WEB_ROOT)
            FileUtils.forceMkdir(webTargetDir)
        } catch (e: IOException) {
            Logger.e(tag, "Failed to make webDir", e)
        }
        return webTargetDir
    }

    private fun getImageDir(context: Context): File? {
        val target = getTargetDir(context)
        var imageTargetDir: File? = null
        try {
            imageTargetDir = File(target, RAINBOW_IMAGES_ROOT)
            FileUtils.forceMkdir(imageTargetDir)
        } catch (e: IOException) {
            Logger.e(tag, "Failed to make imageDir", e)
        }
        return imageTargetDir
    }

    private fun getZipTargetDir(context: Context): File? {
        val target = getTargetDir(context)
        var zipTargetDir: File? = null
        try {
            zipTargetDir = File(target, RAINBOW_ZIP_ROOT)
            FileUtils.forceMkdir(zipTargetDir)
        } catch (e: IOException) {
            Logger.e(tag, "Failed to make targetDir", e)
        }
        return zipTargetDir
    }

    private fun getZipZsyncTargetDir(context: Context): File? {
        val target = getTargetDir(context)
        var zipSyncTargetDir: File? = null
        try {
            zipSyncTargetDir = File(target, RAINBOW_ZSYNC_TMP)
            FileUtils.forceMkdir(zipSyncTargetDir)
        } catch (e: IOException) {
            Logger.e(tag, "Failed to make zipSyncTargetDir", e)
        }
        return zipSyncTargetDir
    }

    private fun getTargetDir(context: Context): File {
        val targetDir = context.getDir(RAINBOW_FILES_ROOT, Context.MODE_PRIVATE)
        try {
            FileUtils.forceMkdir(targetDir)
        } catch (e: IOException) {
            Logger.e(tag, "Failed to make targetDir", e)
        }
        return targetDir
    }

    /**
     * Deletes shared prefs from app side PrefUtils in Rainbow. This excludes the migration flag, paywall related prefs, and the logging id which is the same in classic
     */
    fun deleteRainbowSharedPrefs() {
        GlobalScope.launch(Dispatchers.IO) {
            val defaultPrefs =
                arrayOf(
                    "pref.first_launch",
                    "pref.track_launch",
                    "pref.content_url",
                    "pref.section_id",
                    "pref.last_viewed_time",
                    "pref.user_article_tooltip_shown",
                    "pref.user_guide_shown",
                    "pref.last_activity_background_time",
                    "pref.latest_activity_resume_time",
                    "pref.last_main_activity_background_time",
                    "pref.last_noconnection_shown_time",
                    "pref.rate_app_count",
                    "pref.launched_count",
                    "pref.GCM.ADM.regID",
                    "pref.background_data_enabled",
                    "pref.text_to_speech_enabled",
                    "pref.text_selection_enabled",
                    "pref.preview_done",
                    "pref.cover_letter_shown",
                    "pref.breaking_news_enabled",
                    "pref.sampled_for_metrics",
                    "pref.resampled_for_metrics",
                    "pref.save_msg_shown",
                    "pref.text_to_speech_msg_shown",
                    "pref.text_to_speech_player_state",
                    "pref.next_notification_id",
                    "pref.carousel_next_update_time",
                    "pref.carousel_next_download_time",
                    "pref.carousel_image_size",
                    "pref.first_fullscreen_tap",
                    "pref.last_synced_time",
                    "pref.last_sync_section_update_time",
                    "pref.first_sync_time",
                    "pref.user_pref_start_time",
                    "pref.registered_for_push_news_alerts",
                    "pref.PREF_SHOULD_SHOW_OPENING_ANIMATION",
                    "pref.NEVER_ASK_FOR_NIGHT_MODE",
                    "pref.PREF_DOLLAR_ONE_NEXT_CHECK",
                    "pref.PREF_SUBSCRIPTION_LAST_NOTIFICATION",
                    "pref.NATIVE_TOAST_SHOWN",
                    "pref.PREF_FIRST_APP_OPEN_TIME",
                    "pref.notified_free_stories_available",
                    "pref.NEXT_AD_TAG_UPDATE",
                    "pref.AD_FREQUENCY",
                    "pref.AD_OFFSET",
                    "pref.widget_sections_selected_key_set",
                    "pref.login_after_iap",
                    "pref.IS_APP_EVER_LAUNCHED",
                    "pref.LAST_APP_OPEN_DATE",
                    "pref.active_zodiac_pos",
                    "pref.tts_service_status",
                    "pref.samsung_offer_shown",
                    "pref.PREF_USER_SAMPLE_SEGMENT",
                    "pref.HAS_VERIFIED_SUBSCRIPTION",
                    "pref.PREF_FIRST_TIME_CLEAN_UP_FOR_BUNDLE_FREE",
                    "pref.PREF_SECOND_TIME_CLEAN_UP_FOR_BUNDLE_FREE",
                    "pref.PREF_HAS_HISTORY_ITEMS",
                    "pref.PREF_AMAZON_FT_LOGIN_PROMO_ARTICLE_CONSUMED",
                    "pref.PREF_CONTENT_PMR_RECOMMENDATION_HISTORY",
                    "pref.PREF_CONTENT_LIVECARDS_RECOMMENDATION_HISTORY",
                    "pref.PREF_LAST_USED_NOTIFICATION_ID",
                    "pref.PREF_CONSUMED_RECOMMENDATIONS",
                    "notification.pref.name",
                    "pref.PREF_FAILOVER_STATUS",
                    "pref.PREF_CLEAR_REGISTRATION_FLAG",
                    "pref.PREF_HISTORY_FROM_USER_PREFERENCE_TABLE_CLEARED",
                    "pref.PREF_SAVED_ARTICLE_COUNT_SUMO",
                    "APP_MEASUREMENT_CACHE",
                    "pref.PREF_APP_MEASUREMENT_PAYWALL_SUBSCRIBER_TYPE_TRACKING_INFO",
                    "pref.PREF_APP_MEASUREMENT_PAYWALL_SUBSCRIBER_ATTRIBUTES",
                    "pref.PREF_APP_FORCE_SUB_VERIFY_FOR_SUB_TRACKING_INFO",
                    "pref.registered_for_push_alerts",
                    "pref.has_migrated_airship_push_registration",
                    "pref.has_airship_tags_unregistered",
                    "pref.PREF_ONBAORDING_SCREEN_SHOWN",
                    "pref.PREF_AB_BLOCKER",
                    "pref.PREF_AB_ONBOARDING",
                    "pref.PREF_AB_ALLOCATION_ONBOARDING",
                    "pref.has_reset_carousel_icon",
                )
            // delete from default shared pref
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext())
            val editor = sharedPreferences.edit()
            for (pref in defaultPrefs) {
                editor.remove(pref)
                editor.apply()
            }

            // delete rainbow specific shared pref file
            appContext()
                .getSharedPreferences("LMT_PREF_STORAGE", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
