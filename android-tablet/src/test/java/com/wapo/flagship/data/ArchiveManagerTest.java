/*
 * Copyright (C) 2017. The Washington Post. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by curacamalitod on 7/10/17.
 */

package com.wapo.flagship.data;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.wapo.android.commons.util.Download;
import com.wapo.android.commons.util.FileUtils;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.model.PrintSectionPage;
import com.washingtonpost.android.config.domain.models.config.PrintConfigStub;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static com.wapo.flagship.data.ArchiveManager.BUNDLE_MANIFEST_JSON_FILE_FORMAT;
import static com.wapo.flagship.data.ArchiveManager.PREF_PRINT_FIRST_RUN;
import static com.wapo.flagship.data.ArchiveManager.PREF_PRINT_RUN_COUNT;
import static com.wapo.flagship.data.ArchiveManager.PREVIEW_SECTION;
import static com.wapo.flagship.data.ArchiveManager.ZIP_ARCHIVE_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class ArchiveManagerTest {

    @Mock
    private Context mMockContext;
    @Mock
    private CacheManager mMockCacheManager;
    @Mock
    private CacheManagerImpl mMockCacheManagerImpl;
    @Mock
    private PrintConfigStub mMockPrintConfigStub;
    @Mock
    private SQLiteDatabase mMockSQLiteDatebase;
    @Mock
    private FlagshipApplication flagshipApplication;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private Download mMockDownload;
    @Mock
    private ContentBundle mMockContentBundle;

    private MockedStatic<Download> download;
    private MockedStatic<PreferenceManager> prefManager;
    private MockedStatic<FlagshipApplication> flagshipApp;
    private MockedStatic<FileUtils> fileUtils;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ArchiveManager archiveManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mMockPrintConfigStub.getRetinaBundleURLTemplate()).thenReturn("ipad-retina/%s/%s.zip");
        when(mMockContext.getExternalCacheDir()).thenReturn(new File(""));
        archiveManager = new ArchiveManager(mMockContext, mMockCacheManager, mMockPrintConfigStub);
        archiveManager.setScheduler(Schedulers.immediate());
        archiveManager.setFileServiceScheduler(Schedulers.immediate());
        download = Mockito.mockStatic(Download.class);
        prefManager = Mockito.mockStatic(PreferenceManager.class);
        flagshipApp = Mockito.mockStatic(FlagshipApplication.class);
        fileUtils = Mockito.mockStatic(FileUtils.class);
        ArchiveManager.PREVIEW_ENABLED = true;
    }

    @After
    public void tearDown() {
        download.close();
        prefManager.close();
        flagshipApp.close();
        fileUtils.close();
    }

    @Test
    public void getArchiveFolder() throws Exception {
        long label = 20170704;
        String sectionLetter = "A";
        assertTrue(ArchiveManager.getArchiveFolder(mMockContext, label, sectionLetter).getPath().equals(ArchiveManager.ARCHIVE_DIRECTORY + "/" + label));
    }

    @Test
    public void getZippedArchiveFile() throws Exception {
        long label = 20170705;
        String sectionLetter = "C";
        String zippedArchivePath = ArchiveManager.getZippedArchiveFile(mMockContext, label, sectionLetter).getPath();
        String expectedZipFormat = ArchiveManager.ARCHIVE_DIRECTORY + "/" + String.format(ZIP_ARCHIVE_FORMAT, label, sectionLetter);
        assertTrue(zippedArchivePath.equals(expectedZipFormat));
    }

    @Test
    public void getFullFilePath() throws Exception {
        long label = 20170703;
        String sectionLetter = "B";
        String testFileName = "file.pdf";
        String fullFilePath = ArchiveManager.getFullFilePath(mMockContext, label, sectionLetter, testFileName).getPath();
        String expectedFullFilePath = ArchiveManager.ARCHIVE_DIRECTORY + File.separator + label + File.separator + testFileName;
        assertTrue(fullFilePath.equals(expectedFullFilePath));
    }

    @Test
    public void getPdfRootFolder() throws Exception {
        assertTrue(ArchiveManager.getPdfRootFolder(mMockContext).getPath().equals(ArchiveManager.ARCHIVE_DIRECTORY));
    }

    @Test
    public void scheduleFileDownload() throws Exception {
        long expectedDownloadId = 1010;
        Mockito.when(Download.enqueue(any(Download.class))).thenAnswer(invocation -> expectedDownloadId);
        long label = 20170704;
        String sectionLetter = "B";
        Archive testArchive = new Archive(label, sectionLetter);
        when(mMockCacheManager.getArchiveByLabelAndSection(label, sectionLetter)).thenReturn(testArchive);
        when(mMockCacheManager.createArchive(testArchive)).thenReturn(testArchive);
        Archive result = archiveManager.scheduleFileDownload(label, sectionLetter, 0);
        assertNotNull(result);
        assertTrue(result.getDownloadId() == expectedDownloadId);
    }

    @Test
    public void downloadPreviewArchiveForLabelPass() throws Exception {
        long expectedDownloadId = 1010;
        Mockito.when(Download.enqueue(any(Download.class))).thenAnswer(invocation -> expectedDownloadId);
        long label = 20170704;
        Archive testArchive = new Archive(label, PREVIEW_SECTION);
        when(mMockCacheManager.getArchiveByLabelAndSection(label, PREVIEW_SECTION)).thenReturn(testArchive);
        when(mMockCacheManager.createArchive(testArchive)).thenReturn(testArchive);
        Archive result = archiveManager.downloadPreviewArchiveForLabel(label);
        assertNotNull(result);
        assertTrue(result.getDownloadId() == expectedDownloadId);
    }

    @Test
    public void downloadPreviewArchiveForLabelFail() throws Exception {
        long expectedDownloadId = 1010;
        Mockito.when(Download.enqueue(any(Download.class))).thenAnswer(invocation -> expectedDownloadId);
        long label = 20170704;
        assertNull(archiveManager.downloadPreviewArchiveForLabel(label));
    }

    @Test
    public void getSynchronizedArchiveObs() throws Exception {
        long label = 20170804;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);

        when(mMockContext.getExternalFilesDir(null)).thenReturn(folder.getRoot());
        folder.newFolder("wparchive", String.valueOf(label));
        folder.newFile(String.format("wparchive/" + ZIP_ARCHIVE_FORMAT, label, section));
        folder.newFile(String.format("wparchive/%s/" + BUNDLE_MANIFEST_JSON_FILE_FORMAT, label, section));

        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getSynchronizedArchiveObsTargetFolderMissing() throws Exception {
        long label = 20170804;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);
        when(mMockContext.getExternalFilesDir(null)).thenReturn(folder.getRoot());
        folder.newFolder("wparchive");
        folder.newFile(String.format("wparchive/" + ZIP_ARCHIVE_FORMAT, label, section));

        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getSynchronizedPreviewArchiveForLabelObs() throws Exception {
        long label = 20170804;
        Archive testArchive = new Archive(label, PREVIEW_SECTION);
        testArchive.setDownloadId(null);
        testArchive.setPath(null);
        when(mMockCacheManager.getArchiveByLabelAndSection(label, PREVIEW_SECTION)).thenReturn(testArchive);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedPreviewArchiveForLabelObs(label).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getSynchronizedArchiveObsInvalid() throws Exception {
        long label = 20170804;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> null);
        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(null);
    }

    @Test
    public void getSynchronizedArchiveObsDownloadingError() throws Exception {
        long label = 20170804;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);
        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_FAILED);
        when(mMockDownload.getReason()).thenReturn(DownloadManager.ERROR_DEVICE_NOT_FOUND);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        //Assert that it ended on an Error.
        testSubscriber.assertTerminalEvent();
    }

    @Test
    public void getSynchronizedArchiveObsDownloadingPaused() throws Exception {
        long label = 20170804;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);

        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_PAUSED);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getSynchronizedArchiveObsDownloadingDuplicate() throws Exception {
        long label = 20170804;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);

        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_FAILED);
        when(mMockDownload.getReason()).thenReturn(DownloadManager.ERROR_FILE_ALREADY_EXISTS);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(null);
    }

    @Test
    public void getSynchronizedArchiveObsProcessed() throws Exception {
        long label = 20170804;
        String section = "A";
        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(null);
        testArchive.setPath(null);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getSynchronizedArchiveObsInconsistency() throws Exception {
        long label = 20170804;
        String section = "A";
        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(null);
        testArchive.setPath("testpath");
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(null);
    }

    @Test
    public void getSynchronizedArchiveObsUnusualStatus() throws Exception {
        long label = 20170804;
        String section = "A";
        Archive testArchive = new Archive(label, section);
        testArchive.setStatus(Archive.Status.Deleted);
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(testArchive);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getSynchronizedArchiveObsNotFound() throws Exception {
        long label = 20170804;
        String section = "A";
        when(mMockCacheManager.getArchiveByLabelAndSection(anyLong(), anyString())).thenReturn(null);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getSynchronizedArchiveObs(label, section).subscribe(testSubscriber);
        testSubscriber.assertValue(null);
    }

    /**
     * This method cannot test the full processing functionality, as JSONArray is part of android classes.
     * As a result, this method must be fully testing within instrumentation tests.
     *
     * @throws Exception
     */
    @Test
    public void getProcessArticlesObs() throws Exception {
        long label = 20170804;
        String section = "A";
        Archive testArchive = new Archive(label, section);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        when(mMockContext.getExternalFilesDir(null)).thenReturn(folder.getRoot());
        when(mMockCacheManager.getArchiveBundleByLabel(label)).thenReturn(mMockContentBundle);
        when(mMockContentBundle.getId()).thenReturn((long) 404);
        folder.newFolder("wparchive", String.valueOf(label));
        folder.newFile(String.format("wparchive/%s/" + BUNDLE_MANIFEST_JSON_FILE_FORMAT, label, section));
        archiveManager.getProcessArticlesObs(testArchive).subscribe(testSubscriber);
        testSubscriber.assertTerminalEvent();
    }

    @Test
    public void getProcessArticlesObsNullBundle() throws Exception {
        long label = 20170804;
        String section = "A";
        Archive testArchive = new Archive(label, section);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        when(mMockContext.getExternalFilesDir(null)).thenReturn(folder.getRoot());
        folder.newFolder("wparchive", String.valueOf(label));
        folder.newFile(String.format("wparchive/%s/" + BUNDLE_MANIFEST_JSON_FILE_FORMAT, label, section));
        archiveManager.getProcessArticlesObs(testArchive).subscribe(testSubscriber);
        testSubscriber.assertTerminalEvent();
    }

    @Test
    public void getProcessArticlesObsNoPcData() throws Exception {
        long label = 20170804;
        String section = "A";
        Archive testArchive = new Archive(label, section);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        archiveManager.getProcessArticlesObs(testArchive).subscribe(testSubscriber);
        testSubscriber.assertTerminalEvent();
    }

    @Test
    public void getProcessArticlesObsNull() throws Exception {
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        archiveManager.getProcessArticlesObs(null).subscribe(testSubscriber);
        testSubscriber.assertValue(false);
    }

    @Test
    public void getArchivesByLabel() throws Exception {
        long label = 20170629;
        when(mMockCacheManager.getArchivesByLabel(anyLong())).thenReturn(new LinkedList<Archive>());
        assertNotNull(archiveManager.getArchivesByLabel(label));
    }

    @Test
    public void getPreviewArchiveForLabel() throws Exception {
        long label = 20170629;
        Archive testArchive = new Archive(label, PREVIEW_SECTION);
        when(mMockCacheManager.getArchiveByLabelAndSection(label, PREVIEW_SECTION)).thenReturn(testArchive);
        assertTrue(archiveManager.getPreviewArchiveForLabel(label).getSection().equals(PREVIEW_SECTION));
    }

    @Test
    public void getArchiveByDownloadIdObs() throws Exception {
        long label = 20170710;
        String sectionLetter = "A";
        Archive testArchive = new Archive(label, sectionLetter);
        testArchive.setDownloadId((long) 404);
        when(mMockCacheManager.getArchiveByDownloadId(anyLong())).thenReturn(testArchive);
        TestSubscriber<Archive> testSubscriber = new TestSubscriber<>();
        archiveManager.getArchiveByDownloadIdObs(404).subscribe(testSubscriber);
        testSubscriber.assertValue(testArchive);
    }

    @Test
    public void getArchives() throws Exception {
        when(mMockCacheManager.getArchives(null)).thenReturn(new LinkedList<Archive>());
        assertNotNull(archiveManager.getArchives(null));
    }

    @Test
    public void deleteArchiveAsync() throws Exception {
        long label = 20170803;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(mMockContext.getExternalFilesDir(null)).thenReturn(folder.getRoot());
        when(mMockCacheManager.getArchiveBundleByLabel(label)).thenReturn(mMockContentBundle);
        folder.newFolder("wparchive", String.valueOf(label));
        folder.newFile(String.format("wparchive/" + ZIP_ARCHIVE_FORMAT, label, section));
        folder.newFile(String.format("wparchive/%s/" + BUNDLE_MANIFEST_JSON_FILE_FORMAT, label, section));

        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        archiveManager.deleteArchiveAsync(testArchive, false).subscribe(testSubscriber);
        testSubscriber.assertValue(true);
    }

    @Test
    public void deleteArchiveAsyncFilesNotThere() throws Exception {
        long label = 20170803;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);
        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_SUCCESSFUL);

        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        archiveManager.deleteArchiveAsync(testArchive, false).subscribe(testSubscriber);
        testSubscriber.assertValue(false);
    }

    @Test
    public void deleteArchiveAsyncFullDelete() throws Exception {
        long label = 20170803;
        String section = "A";

        when(mMockContext.getExternalFilesDir(null)).thenReturn(folder.getRoot());
        folder.newFolder("wparchive", String.valueOf(label));
        folder.newFile(String.format("wparchive/" + ZIP_ARCHIVE_FORMAT, label, section));
        folder.newFile(String.format("wparchive/%s/" + BUNDLE_MANIFEST_JSON_FILE_FORMAT, label, section));

        Archive testArchive = new Archive(label, section);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        archiveManager.deleteArchiveAsync(testArchive, true).subscribe(testSubscriber);
        testSubscriber.assertValue(true);
    }

    @Test
    public void deleteArchiveAsyncDownloading() throws Exception {
        long label = 20170803;
        String section = "A";
        long downloadId = 404;

        Mockito.when(Download.get(mMockContext, downloadId)).thenAnswer(invocation -> mMockDownload);

        when(mMockDownload.getStatus()).thenReturn(DownloadManager.STATUS_RUNNING);
        Archive testArchive = new Archive(label, section);
        testArchive.setDownloadId(downloadId);
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        archiveManager.deleteArchiveAsync(testArchive, false).subscribe(testSubscriber);
        testSubscriber.assertValue(false);
    }

    @Test
    public void updateArchive() throws Exception {
        long label = 20170713;
        String sectionLetter = "A";
        Archive testArchive = new Archive(label, sectionLetter);
        testArchive.setStatus(Archive.Status.Deleted);
        archiveManager.updateArchive(testArchive);
        assertNotNull(testArchive);
    }

    /*
    @Test
    public void getPrintManifest() throws Exception {
        TestSubscriber<PrintManifestResponse> testSubscriber = new TestSubscriber<>();
        archiveManager.getPrintManifest(new Date()).subscribe(testSubscriber);
        testSubscriber.assertError(NullPointerException.class);
    }

    @Test
    public void performDailyUpdate() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(mMockPrintConfigStub.isDailyDownloadEnabled()).thenReturn(true);
        ArrayList<Archive> testOldArchives = new ArrayList<>();
        Archive testArchive = new Archive(20170808, "A");
        testArchive.updateTimestamp();
        testOldArchives.add(testArchive);
        when(mMockCacheManager.getArchives(null)).thenReturn(testOldArchives);
        archiveManager.performDailyUpdate();
    }

    @Test
    public void performDailyUpdateDisabled() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(mMockPrintConfigStub.isDailyDownloadEnabled()).thenReturn(false);
        archiveManager.performDailyUpdate();
    }

    @Test
    public void performDailyUpdateSectionReadsUpdate() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);

        HashSet<String> sectionReads = new HashSet<>();
        sectionReads.add(String.valueOf(System.currentTimeMillis() - 360000));
        sectionReads.add(String.valueOf(System.currentTimeMillis()));
        sectionReads.add(String.valueOf(System.currentTimeMillis()));

        when(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false);
        when(sharedPreferences.getStringSet(anyString(), Matchers.<Set<String>>anyObject())).thenReturn(sectionReads);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(editor.putStringSet(anyString(), Matchers.<Set<String>>anyObject())).thenReturn(editor);
        when(mMockPrintConfigStub.isDailyDownloadEnabled()).thenReturn(false);
        archiveManager.performDailyUpdate();
    }

    @Test
    public void downloadNeededSections() throws Exception {
        long label = 20170808;
        long testDownloadId = 1010;

        Mockito.mockStatic(PreferenceManager.class, ReachabilityUtil.class, Download.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        BDDMockito.given(ReachabilityUtil.isOnWiFi(mMockContext)).willReturn(true);
        BDDMockito.given(Download.enqueue(any(Download.class))).willReturn(testDownloadId);

        Archive testPreviewArchive = new Archive(label, PREVIEW_SECTION);
        Archive testASectionArchive = new Archive(label, "A");
        testASectionArchive.setStatus(Archive.Status.Canceled);
        Archive testMetroArchive =  new Archive(label, "B");
        testMetroArchive.setStatus(Archive.Status.Deleted);
        ArrayList<PrintSection> printSections = new ArrayList<>();
        printSections.add(new PrintSection("A", "A Section", null, null));
        printSections.add(new PrintSection("B", "Metro", null,null));
        printSections.add(new PrintSection("C", "Style", null, null));

        when(mMockCacheManager.getArchiveByLabelAndSection(label, PREVIEW_SECTION)).thenReturn(testPreviewArchive);
        when(mMockCacheManager.getArchiveByLabelAndSection(label, "A")).thenReturn(testASectionArchive);
        when(mMockCacheManager.getArchiveByLabelAndSection(label, "B")).thenReturn(testMetroArchive);
        when(mMockCacheManager.createArchive(testASectionArchive)).thenReturn(testASectionArchive);

        //Cannot mock PrintManifestResponse as it is a data class in kotlin and cannot be subclassed.
        PrintManifestResponse printManifestResponse = new PrintManifestResponse(new Issue(label, printSections));
        when(sharedPreferences.getBoolean(PREF_PRINT_DOWNLOAD_WIFI_ONLY, true)).thenReturn(false);
        when(sharedPreferences.getBoolean("A Section", true)).thenReturn(true);
        when(sharedPreferences.getBoolean("Metro", true)).thenReturn(true);
        when(sharedPreferences.getBoolean("Style", true)).thenReturn(false);
        archiveManager.downloadNeededSections(printManifestResponse);
        assertTrue(testMetroArchive.getDownloadId() == null);
        assertTrue(testPreviewArchive.getDownloadId() == null);
        assertTrue(testASectionArchive.getDownloadId() == 1010);
    }

    @Test
    public void downloadNeededSectionsPreview() throws Exception {
        Mockito.mockStatic(PreferenceManager.class, ReachabilityUtil.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        BDDMockito.given(ReachabilityUtil.isOnWiFi(mMockContext)).willReturn(true);
        long label = 20170808;

        //Indicate that the preview archive is needed.
        when(mMockCacheManager.getArchiveByLabelAndSection(label, PREVIEW_SECTION)).thenReturn(null);

        //Cannot mock PrintManifestResponse as it is a data class in kotlin and cannot be subclassed.
        PrintManifestResponse printManifestResponse = new PrintManifestResponse(new Issue(label, new ArrayList<PrintSection>()));
        when(sharedPreferences.getBoolean(PREF_PRINT_DOWNLOAD_WIFI_ONLY, true)).thenReturn(false);
        archiveManager.downloadNeededSections(printManifestResponse);

    }

    @Test
    public void downloadNeededSectionsPrintWifiOnly() throws Exception {
        Mockito.mockStatic(PreferenceManager.class, ReachabilityUtil.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        BDDMockito.given(ReachabilityUtil.isOnWiFi(mMockContext)).willReturn(false);
        long pubdate = 20170808;

        //Cannot mock PrintManifestResponse as it is a data class in kotlin and cannot be subclassed.
        PrintManifestResponse printManifestResponse = new PrintManifestResponse(new Issue(pubdate, new ArrayList<PrintSection>()));
        when(sharedPreferences.getBoolean(PREF_PRINT_DOWNLOAD_WIFI_ONLY, true)).thenReturn(true);
        archiveManager.downloadNeededSections(printManifestResponse);
    }
    */

    @Test
    public void isPrintEditionPushEnabled() throws Exception {
        //Test the default behavior, which is to say that it is enabled.
        assertTrue(archiveManager.isPrintEditionPushEnabled());
    }

    @Test
    public void setupSectionReadsForFirstRun() throws Exception {
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putStringSet(anyString(), any())).thenReturn(editor);
        when(editor.commit()).thenReturn(true);
        assertTrue(archiveManager.setupSectionReadsForFirstRun());
    }

    @Test
    public void setupSectionReadsForFirstRunFail() throws Exception {
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putStringSet(anyString(), any())).thenReturn(editor);
        when(editor.commit()).thenReturn(false);
        assertFalse(archiveManager.setupSectionReadsForFirstRun());
    }

    @Test
    public void lockSection() throws Exception {
        String testSection = "A Section";
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putBoolean(testSection + "Locked", true)).thenReturn(editor);
        when(editor.commit()).thenReturn(true);
        //This tests the logic because it will hit an exception if the String format above is changed.
        assertTrue(archiveManager.lockSection(testSection));
    }

    /*
    @Test
    public void setDailyDownloadOn() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(editor.putBoolean(PREF_PRINT_DAILY_DOWNLOAD, true)).thenReturn(editor);
        //Should be false because the edit will fail because the classes are mocked.
        archiveManager.setDailyDownloadOn(true);
        assertFalse(sharedPreferences.getBoolean(PREF_PRINT_DAILY_DOWNLOAD, false));
    }
    */

    @Test
    public void deleteArchiveFiles() throws Exception {
        Mockito.when(FlagshipApplication.getInstance()).thenAnswer(invocation -> flagshipApplication);
        when(flagshipApplication.getCacheManager()).thenReturn(mMockCacheManagerImpl);
        when(mMockCacheManagerImpl.getDb()).thenReturn(mMockSQLiteDatebase);
        when(ArchiveManager.getPdfRootFolder(mMockContext)).thenReturn(new File(""));
        assertNotNull(ArchiveManager.deleteArchiveFiles(mMockContext));
    }

    @Test
    public void getPrintEditionSizeInDisk() throws Exception {
        when(ArchiveManager.getPdfRootFolder(mMockContext)).thenReturn(new File(""));
        Mockito.when(FileUtils.dirSize(any(File.class))).thenAnswer(invocation -> Long.valueOf(1048576));
        assertTrue(ArchiveManager.getPrintEditionSizeInDisk(mMockContext) == 1);
    }

    @Test
    public void getPlaceholderPDFFilePath() throws Exception {
        String testPath = "test";
        when(mMockContext.getFilesDir()).thenReturn(new File(testPath));
        assertTrue(ArchiveManager.getPlaceholderPDFFilePath(mMockContext)
                .equals(testPath + File.separator + ArchiveManager.PLACEHOLDER_PDF_FILENAME));
    }

    @Test
    public void getTutorialAssetFilePath() throws Exception {
        String testPath = "test";
        String testFileName = "tutorial.pdf";
        when(mMockContext.getFilesDir()).thenReturn(new File(testPath));
        assertTrue(ArchiveManager.getTutorialAssetFilePath(mMockContext, testFileName)
                .equals(testPath + File.separator + testFileName));
    }

    @Test
    public void getTutorialCoverImageFilename() throws Exception {
        String testPath = "test";
        when(mMockContext.getFilesDir()).thenReturn(new File(testPath));
        assertTrue(ArchiveManager.getTutorialCoverImageFilename(mMockContext)
                .equals(testPath + File.separator + ArchiveManager.TUTORIAL_PAGE_ONE_HIGH_RES_FILENAME));
    }

    /**
     * Tests the method to get the pre-set tutorial pages, and checks that their content is correct.
     *
     * @throws Exception standard test Exception
     */
    @Test
    public void getTutorialPages() throws Exception {
        ArrayList<PrintSectionPage> tutorialPages = ArchiveManager.getTutorialPages();
        assertNotNull("Tutorial pages were null.", tutorialPages);
        assertTrue("Incorrect number of tutorial pages.", tutorialPages.size() == 2);
        PrintSectionPage pageOne = tutorialPages.get(0);
        PrintSectionPage pageTwo = tutorialPages.get(1);
        assertTrue("Page one of tutorial has incorrect name.", pageOne.getPageName().equalsIgnoreCase("Training 01"));
        assertTrue("Page two of tutorial has incorrect name.", pageTwo.getPageName().equalsIgnoreCase("Training 02"));
        assertTrue("Widths of tutorial pages are not equal.", pageOne.getPageWidth() == pageTwo.getPageWidth());
    }

    /**
     * Tests that the method to get "today's date" for print edition returns the correct date.
     *
     * @throws Exception standard test Exception
     */
    @Test
    public void getCurrentPrintEditionDate() throws Exception {
        Calendar currentPrintEditionDate = Calendar.getInstance();
        currentPrintEditionDate.setTime(ArchiveManager.getCurrentDate());
        Calendar testDate = Calendar.getInstance();
        testDate.setTime(new Date());
        assertEquals("Current date and print edition date are not equal.", currentPrintEditionDate.get(Calendar.DAY_OF_MONTH), testDate.get(Calendar.DAY_OF_MONTH));
    }

    /*
    @Test
    public void isPrintTutorialEnabled() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(sharedPreferences.getBoolean(PREF_PRINT_TUTORIAL_ENABLED, true)).thenReturn(true);
        assertTrue(ArchiveManager.isPrintTutorialEnabled());
    }

    @Test
    public void setPrintTutorialEnabled() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(editor.putBoolean(PREF_PRINT_TUTORIAL_ENABLED, true)).thenReturn(editor);
        //Should be false because the edit will fail because the classes are mocked.
        ArchiveManager.setPrintTutorialEnabled(true);
        assertFalse(PreferenceManager.getDefaultSharedPreferences(mMockContext).getBoolean(SettingsFragment.PREF_PRINT_TUTORIAL_ENABLED, true));
    }
    */

    @Test
    public void isPrintEditionFirstRun() throws Exception {
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(sharedPreferences.getBoolean(PREF_PRINT_FIRST_RUN, true)).thenReturn(true);
        assertTrue(ArchiveManager.isPrintEditionFirstRun(mMockContext));
    }

    @Test
    public void setPrintEditionFirstRun() throws Exception {
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(editor.putBoolean(PREF_PRINT_FIRST_RUN, true)).thenReturn(editor);
        //Should be false because the edit will fail because the classes are mocked.
        assertFalse(ArchiveManager.setPrintEditionFirstRun(mMockContext, true));
    }

    @Test
    public void getPrintEditionRunCount() throws Exception {
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        //Testing for the exact int value ensures that this test will fail if the default value is changed.
        when(sharedPreferences.getInt(PREF_PRINT_RUN_COUNT, 0)).thenReturn(0);
        assertTrue(ArchiveManager.getPrintEditionRunCount(mMockContext) == 0);
    }

    @Test
    public void setPrintEditionRunCount() throws Exception {
        Mockito.when(PreferenceManager.getDefaultSharedPreferences(mMockContext)).thenAnswer(invocation -> sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(editor.putInt(PREF_PRINT_RUN_COUNT, 0)).thenReturn(editor);
        //Should be false because the edit will fail because the classes are mocked.
        assertFalse(ArchiveManager.setPrintEditionRunCount(mMockContext, 0));
    }

    /*
    @Test
    public void isPrintDownloadWifiOnly() throws Exception {
        Mockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(mMockContext)).willReturn(sharedPreferences);
        //Testing for the exact boolean value ensures that this test will fail if the default value is changed.
        when(sharedPreferences.getBoolean(PREF_PRINT_DOWNLOAD_WIFI_ONLY, true)).thenReturn(true);
        assertTrue(archiveManager.isPrintDownloadWifiOnly());
    }
    */

    @Test
    public void getCacheManager() throws Exception {
        assertTrue(archiveManager.getCacheManager().equals(mMockCacheManager));
    }

    @Test
    public void getContext() throws Exception {
        assertTrue(archiveManager.getContext().equals(mMockContext));
    }

    @Test
    public void testDownloadManagerException() throws Exception {
        int reason = 1;
        ArchiveManager.DownloadManagerException exception = new ArchiveManager.DownloadManagerException(reason);
        assertTrue(exception.getReason() == reason);
    }

    @Test
    public void getSectionsList() throws Exception {
        String[] sections = archiveManager.getSectionsList();
        assertNotNull("Sections list returns null.", sections);
        assertTrue("Incorrect number of sections.", sections.length == 16);
        assertTrue("First section not labeled correctly.", sections[0].equalsIgnoreCase("A Section"));
        assertTrue("Second section not labeled correctly.", sections[1].equalsIgnoreCase("Metro"));
        assertTrue("Third section not labeled correctly.", sections[2].equalsIgnoreCase("Sports"));
        assertTrue("Fourth section not labeled correctly.", sections[3].equalsIgnoreCase("Style"));
    }
}