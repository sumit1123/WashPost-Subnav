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
 * Created by curacamalitod on 4/19/17.
 */

package com.wapo.flagship.data;

import android.app.DownloadManager;
import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.wapo.android.commons.util.Download;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.model.PrintManifestResponse;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import static org.junit.Assert.*;

/**
 * Created by curacamalitod on 4/19/17.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class ArchiveManagerIntegrationTest {

    private static ArchiveManager am;
    private Context context;
    private static Date yesterday;
    private static long label;
    private static Archive archive;
    private static final String section = "A";

    /**
     * Sets up static members of the class to be used across all test cases.
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception{
        am = FlagshipApplication.getInstance().getArchiveManager();
        yesterday = new Date();
        yesterday.setTime(yesterday.getTime() - Utils.oneDayinMilliseconds);
        label = Utils.dateToEDTLabel(yesterday);
        archive = am.scheduleFileDownload(label, section, 0);
    }

    /**
     * Defines the context to be used before each test case.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
    }

    /**
     * Cleans up archives that were created during testing.
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ArchiveManager.deleteArchiveFiles(InstrumentationRegistry.getTargetContext());
    }

    /**
     * Tests the getSynchronizedArchive and getArchiveByLabelAndSection methods.
     * @throws Exception
     */
    @Test(timeout = 120000)
    public void getArchiveByLabelAndSection() throws Exception {
        long downloadId = archive.getDownloadId();
        while (Download.get(context, downloadId).getStatus() != DownloadManager.STATUS_SUCCESSFUL
                && Download.get(context, downloadId).getStatus() != DownloadManager.STATUS_FAILED) {
            Thread.sleep(3000);
        }
        am.getSynchronizedArchiveObs(label, section).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Archive archive) {
                assertNotNull("Failed to get a synchronized archive.", archive);
                assertNull("DownloadId not properly cleared when running through getSynchronizedArchive().", archive.getDownloadId());
                assertTrue("Archive folder was not properly created.", ArchiveManager.getArchiveFolder(context, label, section).exists());
                assertNotNull("Archive was not stored in the SQLite table.", am.getArchiveByLabelAndSection(label, section));
            }
        });
    }

    /**
     * Tests updating of archive's timestamp. This is required for smart logic / counting reads.
     * @throws Exception
     */
    @Test
    public void updateArchive() throws Exception {
        assertNotNull("Archive is null before trying to update it.", archive);
        long timestamp = System.currentTimeMillis();
        archive.updateTimestamp();
        am.updateArchive(archive);
        assertTrue("Timestamp was not successfully updated.", archive.getTimestamp() >= timestamp);
    }

    /**
     * Tests ArchiveManager's ability to delete individual sections, along with their corresponding zip file.
     * @throws Exception
     */
    /*
    @Test
    public void deleteArchiveAsync() throws Exception {
        final File zip = ArchiveManager.getZippedArchiveFile(context, label, section);
        assertNotNull("GetZippedArchiveFile returned null.", zip);
        assertTrue("Zip file for archive doesn't exist when it's expected to.", zip.exists());
        am.deleteArchiveAsync(archive, true).subscribe(new Subscriber<Boolean>() {
            @Override
            public void onCompleted() {}

            @Override
            public void onError(Throwable throwable) {
                fail();
            }
            @Override
            public void onNext(Boolean aBoolean) {
                assertTrue("Archive was not deleted successfully.", aBoolean);
                assertFalse("Zip file still exists.", zip.exists());
            }
        });
    }
    */

    /**
     * Tests download of manifest from server. Also tests for success of downloadNeededSections method.
     * @throws Exception
     */
    @Test
    public void getPrintManifest() throws Exception {
        am.getPrintManifest(yesterday).subscribe(new Subscriber<PrintManifestResponse>() {
            @Override
            public void onCompleted() {}
            @Override
            public void onError(Throwable throwable) {
                fail("Failed to get print manifest.");
            }
            @Override
            public void onNext(PrintManifestResponse printManifestResponse) {
                assertNotNull("Manifest response was null.", printManifestResponse);
            }
        });
    }

    /**
     * Tests for the ability of user to enable push notifications for print edition.
     * @throws Exception
     */
    @Test
    public void enablePrintEditionPush() throws Exception {
        am.enablePrintEditionPush();
        assertTrue("Print edition push value set to false.", am.isPrintEditionPushEnabled());
    }

    /**
     * Tests the ability to disable the print tutorial.
     * @throws Exception
     */
    @Test
    public void setPrintTutorialEnabled() throws Exception {
        ArchiveManager.setPrintTutorialEnabled(false);
        assertFalse("Print tutorial was still enabled.", ArchiveManager.isPrintTutorialEnabled());
    }

    /**
     * Tests the ability to mark if print edition has been run before.
     * @throws Exception
     */
    @Test
    public void setPrintEditionFirstRun() throws Exception {
        ArchiveManager.setPrintEditionFirstRun(context, false);
        assertFalse("Print edition still showed first run.", ArchiveManager.isPrintEditionFirstRun(context));
    }

    /**
     * Tests the ability to keep track of how many time print edition has been run. (Caps at 4, as additional runs don't matter after that.)
     * @throws Exception
     */
    @Test
    public void getPrintEditionRunCount() throws Exception {
        ArchiveManager.setPrintEditionRunCount(context, 4);
        assertTrue("Print run count was not updated.", ArchiveManager.getPrintEditionRunCount(context) == 4);
    }

    /**
     * Tests that the preset sections list has the correct content, particularly for the daily sections.
     * @throws Exception
     */
    @Test
    public void getSectionsList() throws Exception {
        String[] sections = am.getSectionsList();
        assertNotNull("Sections list returns null.", sections);
        assertTrue("Incorrect number of sections.", sections.length == 16);
        assertTrue("First section not labeled correctly.", sections[0].equalsIgnoreCase("A Section"));
        assertTrue("Second section not labeled correctly.", sections[1].equalsIgnoreCase("Metro"));
        assertTrue("Third section not labeled correctly.", sections[2].equalsIgnoreCase("Sports"));
        assertTrue("Fourth section not labeled correctly.", sections[3].equalsIgnoreCase("Style"));
    }

}