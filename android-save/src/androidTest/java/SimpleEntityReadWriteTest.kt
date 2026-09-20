/* Copyright (c) 2019 The Washington Post. All rights reserved. */

import android.content.Context
import com.wapo.android.commons.util.Logger
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.gson.Gson
import com.wapo.android.commons.util.AppContextUtils
import com.washingtonpost.android.save.SaveProvider
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.SavedArticleDB
import com.washingtonpost.android.save.database.SavedArticleDBHelper
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.android.save.network.Metadata
import com.washingtonpost.android.save.views.ArticleListViewModel
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SimpleEntityReadWriteTest {
    private lateinit var db: SavedArticleDB
    private lateinit var savedArticleManager: SavedArticleManager
    private lateinit var savedArticleDBHelper: SavedArticleDBHelper
    private lateinit var testScope: CoroutineScope
    private var runs = 0

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ArticleListViewModel

    @Before
    fun createDb() {
        val appContext = InstrumentationRegistry.getTargetContext()
        AppContextUtils.init(appContext, "appName")
        testScope = CoroutineScope(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(appContext, SavedArticleDB::class.java)
            .allowMainThreadQueries()
            .build()
        val saveProvider = object : SaveProvider {
            override fun isConnected(): Boolean {
                return true
            }

            override fun logPreferenceSyncException(t: Throwable) {
            }

            override fun logMetadataSyncException(t: Throwable) {
            }

            override fun logPreferenceErrorResponse(code: Int, errorResponse: String) {
            }

            override fun logMetadataErrorResponse(code: Int, errorResponse: String) {
            }

            override fun logExtras(message: String) {
            }

            override fun openWebViewActivity(url: String, context: Context) {
            }

            override fun openLoginActivity(context: Context) {
            }

            override fun isNightModeOn(): Boolean {
                return false
            }

            override fun openArticles(
                context: Context?,
                navigationBehavior: String,
                urls: Array<String>,
                url: String,
                sectionDisplayName: String
            ) {
            }

            override fun isLoggedInUser(): Boolean {
                return false
            }

            override fun isLoggedInUserAndSubscriber(): Boolean {
                return false
            }

            override fun getPreferencesRequestHeaders(isArchive: Boolean): HashMap<String, String> {
                return hashMapOf()
            }

            override fun getPreferenceBaseURL(): String {
                return "https://id.digitalink.com/"
            }

            override fun getMetadataBaseUrl(): String {
                return "https://api.washingtonpost.com/metadata-service/v1/metadata/canonical/"
            }

            override fun getAnimatedImageLoader(): AnimatedImageLoader {
                TODO("not implemented")
            }

            override fun getSavedArticleManager(): SavedArticleManager {
                return savedArticleManager
            }

            override fun updateArticlesIfNeeded(savedArticleList: List<ArticleAndMetadata>) {

            }

            override fun getAppContext(): Context {
                return appContext
            }

            override fun logException(throwable: Throwable) {
                Logger.e(TAG, "An error occurred", throwable)
            }
        }
        savedArticleDBHelper = SavedArticleDBHelper(saveProvider, db)
        savedArticleManager = SavedArticleManager.getInstance(
            SavedArticleManager.Params(
                saveProvider,
                testScope,
                savedArticleDBHelper
            )
        )
        viewModel = ArticleListViewModel(savedArticleManager)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
        SavedArticleManager.resetInstance()
    }

    @Test
    @Throws(Exception::class)
    fun saveArticle() = runTest {
        val article1 = getSavedArticle(url1)
        val article2 = getSavedArticle(url2)
        savedArticleManager.addArticle(article1, getMetadata(url1))
        advanceUntilIdle()
        assertEquals(
            1,
            savedArticleDBHelper.getArticleListQueue().size
        )
        savedArticleManager.addArticle(article2, getMetadata(url2))
        advanceUntilIdle()
        val articles2 = savedArticleDBHelper.getArticleListQueue()
        assertEquals(2, articles2.size)
        val result1 = savedArticleDBHelper.getArticleByUrl(
            article1.contentURL
        )
        assertTrue(result1?.canonicalURL == article1.contentURL)
        val result2 = savedArticleDBHelper.getArticleByUrl(
            article2.contentURL
        )
        assertTrue(result2?.contentURL == article2.contentURL)
    }

    @Test
    @Throws(Exception::class)
    fun deleteArticle() = runTest {
        val article1 = getSavedArticle(url1)
        val article2 = getSavedArticle(url2)
        savedArticleManager.addArticle(article1, getMetadata(url1))
        advanceUntilIdle()
        savedArticleManager.addArticle(article2, getMetadata(url2))
        advanceUntilIdle()
        assertEquals(
            2,
            savedArticleDBHelper.getArticlesToList(100).size
        )
        val result = savedArticleDBHelper.getArticleByUrl(
            article2.contentURL
        )
        savedArticleManager.removeArticles(listOf(result!!))
        advanceUntilIdle()
        assertEquals(
            1,
            savedArticleDBHelper.getArticlesToList(100).size
        )
    }

    private fun getSavedArticle(
        contentUrl: String,
    ): SavedArticleModel {
        return SavedArticleModel(contentUrl, System.currentTimeMillis() + (runs++))
    }

    @Throws(Exception::class)
    private fun getMetadata(contentUrl: String): MetadataModel {
        val json =
            """{"metadata":[{"canonical_url":"$contentUrl","headline":"Cyclone Fani batters India’s eastern coast; 1.1 million people evacuated","byline":"By Niha Masih","social_image":"https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/HZQX4NTNTYI6TO7HDR4Y7OAFGY.jpg","description":"The storm — the most dangerous in recent years — brought activity in the region to a halt.","display_date":"2019-05-03T12:31:19.708Z","label":{"basic":{"text":"World","url":"https://www.washingtonpost.com/world/","display":true},"transparency":{"text":"News","url":"","display":true}},"last_updated_date":"2019-05-03T13:56:18.053Z","content_restriction_code":"paywall_default","webview_preferred":false}]}"""
        val metadata = Gson().fromJson(json, Metadata::class.java).metadata?.firstOrNull()
            ?: throw IllegalStateException("Metadata must not be null")
        return MetadataModel(
            contentUrl,
            System.currentTimeMillis() + (runs++)
        ).apply {
            headline = metadata.headline
            byline = metadata.byLine
            blurb = metadata.byLine
            imageURL = metadata.socialImageUrl
            canonicalURL = metadata.canonicalUrl
        }
    }

    private fun logAllTables(database: SupportSQLiteDatabase? = null) {
        Logger.d(TAG, "----------")
        val db = database ?: db.openHelper.readableDatabase
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        while (cursor.moveToNext()) {
            val tableName = cursor.getString(0)
            if (tableName != "android_metadata" && !tableName.startsWith("sqlite_")) {
                val dataCursor = db.query("SELECT * FROM $tableName")
                Logger.d(TAG, "Table: $tableName")
                while (dataCursor.moveToNext()) {
                    val row = (0 until dataCursor.columnCount).joinToString { idx ->
                        "${dataCursor.getColumnName(idx)}=${dataCursor.getString(idx)}"
                    }
                    Logger.d(TAG, row)
                }
                dataCursor.close()
            }
        }
        cursor.close()
    }

    companion object {
        val TAG = SimpleEntityReadWriteTest::class.java.simpleName
        const val url1 =
            "https://www.washingtonpost.com/world/asia_pacific/cyclone-fani-hits-indias-east-coast-12-million-evacuated/2019/05/03/ce507e74-6d68-11e9-bbe7-1c798fb80536_story.html"
        const val url2 =
            "https://www.washingtonpost.com/world/2019/05/03/cyclone-fani-batters-indias-eastern-coast-million-people-evacuated/"
    }
}