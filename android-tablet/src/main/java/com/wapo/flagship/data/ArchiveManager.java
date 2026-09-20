package com.wapo.flagship.data;

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Process;
import android.preference.PreferenceManager;
import com.wapo.android.commons.util.Logger;
import android.util.Log;
import com.wapo.android.commons.util.FileUtils;
import com.wapo.flagship.AppContext;
import com.wapo.android.commons.util.Download;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.print.network.PrintApiClient;
import com.wapo.flagship.features.settings.AppPreferences;
import com.wapo.flagship.json.ResourceManifest;
import com.wapo.flagship.json.Section;
import com.wapo.flagship.model.PrintManifestResponse;
import com.wapo.flagship.model.PrintSection;
import com.wapo.flagship.model.PrintSectionPage;
import com.wapo.flagship.push.PushListener;
import com.wapo.flagship.util.ConnectivityMonitor;
import com.wapo.flagship.util.ReachabilityUtil;
import com.wapo.android.commons.util.ZipUtil;
import com.wapo.flagship.util.tracking.Measurement;
import com.washingtonpost.android.R;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.PrintConfigStub;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class ArchiveManager {
    private static final String TAG = ArchiveManager.class.getName();
    public static final String ZIP_ARCHIVE_FORMAT = "pc-%d-%s.zip";
    public static final String PREVIEW_SECTION = "preview";
    private static final String FORMATTED_PREVIEW = "Preview";
    public static final String BUNDLE_MANIFEST_JSON_FILE_FORMAT = "%s-pcdata.json";
    protected static final String PREF_PRINT_TUTORIAL_ENABLED = "prefPrintTutorialEnabled";
    protected static final String PREF_PRINT_FIRST_RUN = "prefPrintEditionFirstRun";
    protected static final String PREF_PRINT_RUN_COUNT = "prefPrintEditionRunCount";
    protected static final String PREF_PRINT_DOWNLOAD_WIFI_ONLY = "prefPrintDownloadWifiOnly";
    protected static final String PREF_PRINT_DAILY_DOWNLOAD = "prefPrintDailyDownload";
    private static final String PREF_PRINT_DATE = "prefPrintDate";
    private static final String PREF_LAST_VIEWED_TIME = "prefLastViewedTime";
    private static final String PREF_SMART_LOGIC_SETUP = "prefSmartLogicSetup";

    public static final String ARCHIVE_DIRECTORY = "wparchive";
    public static final String PLACEHOLDER_PDF_FILENAME = "placeholder.pdf";
    public static final String TUTORIAL_SECTION_NAME = "Tutorial";
    public static final String TUTORIAL_PAGE_ONE_HIGH_RES_FILENAME = "tutorialhighresone.jpg";
    public static final String TUTORIAL_PAGE_ONE_THUMBNAIL_FILENAME = "tutorialthumbnailone.jpg";
    public static final String TUTORIAL_PAGE_TWO_THUMBNAIL_FILENAME = "tutorialthumbnailtwo.jpg";
    public static final String TUTORIAL_PAGE_ONE_PDF_FILENAME = "tutorialpdfone.pdf";
    public static final String TUTORIAL_PAGE_TWO_PDF_FILENAME = "tutorialpdftwo.pdf";
    public static boolean PREVIEW_ENABLED = true;
    private static final int EXPIRED_DATE_COUNT = 3;
    private static final int STALE_DATE_COUNT = 14;

    private static final String A_SECTION = "A Section";
    private static final String METRO = "Metro";
    private static final String STYLE = "Style";
    private static final String SPORTS = "Sports";
    private static final String BUSINESS = "Business";
    private static final String MAGAZINE = "Magazine";
    private static final String FOOD = "Food";
    private static final String HEALTH = "Health";
    private static final String LOCAL_LIVING = "Local Living";
    private static final String COMICS = "Comics";
    private static final String OUTLOOK = "Outlook";
    private static final String TRAVEL = "Travel";
    private static final String ARTS = "Arts";
    private static final String WEEKEND = "Weekend";
    private static final String REAL_ESTATE = "Real Estate";
    private static final String SPECIAL_ADVERTISING = "Special Advertising";

    public static final int DEFAULT_PAGE_HEIGHT = 1709;
    public static final int DEFAULT_PAGE_WIDTH = 967;
    private static final String TUTORIAL_PAGE_ONE_NAME = "Training 01";
    private static final String TUTORIAL_PAGE_TWO_NAME = "Training 02";
    private final HashSet<String> DAILY_SECTIONS = new HashSet<>(4, 1);

    private final String[] SECTIONS;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private final SimpleDateFormat downloadDateFormat = new SimpleDateFormat("EEEE MMMM d, yyyy", Locale.US);
    private final PrintApiClient.PrintApiService printApiAWSService;

    private final Context _context;
    private final CacheManager _cacheManager;
    private final PrintConfigStub _printConfigStub;

    private final ExecutorService fileProcessingExecutorService;
    private Scheduler fileServiceScheduler;
    private Scheduler scheduler;

    /**
     * ArchiveManager constructor.  Creates two executor services for background threads,
     * gets a ArchiveManager singleton from FlagshipApplication,
     * @param context app context
     */
    public ArchiveManager(Context context) {
        this(context, FlagshipApplication.getInstance().getCacheManager(), ConfigManager.Companion.getInstance().getConfig().getPrintConfig());
    }

    public ArchiveManager(Context context, CacheManager cacheManager, PrintConfigStub printConfigStub) {
        if (context == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        _context = context;
        _cacheManager = cacheManager;
        _printConfigStub = printConfigStub;

        ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r) {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        super.run();
                    }
                };
            }
        });
        fileProcessingExecutorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r) {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        super.run();
                    }
                };
            }
        });
        scheduler = Schedulers.from(executorService);
        fileServiceScheduler = Schedulers.from(fileProcessingExecutorService);

        String baseUrl = _printConfigStub.getNewsstandBaseURL();
        if (baseUrl != null) {
            PrintApiClient apiAWSClient = new PrintApiClient(baseUrl, context.getCacheDir(), context);
            this.printApiAWSService = apiAWSClient.getClient().create(PrintApiClient.PrintApiService.class);
        } else {
            this.printApiAWSService = null;
        }

        PREVIEW_ENABLED = _printConfigStub.getPreviewEnabled();

        Resources res = _context.getResources();
        if (res != null) {
            DAILY_SECTIONS.add(res.getString(R.string.pref_A_section));
            DAILY_SECTIONS.add(res.getString(R.string.pref_metro));
            DAILY_SECTIONS.add(res.getString(R.string.pref_sports));
            DAILY_SECTIONS.add(res.getString(R.string.pref_style));

            SECTIONS = new String[]{res.getString(R.string.pref_A_section),
                    res.getString(R.string.pref_metro),
                    res.getString(R.string.pref_sports),
                    res.getString(R.string.pref_style),
                    res.getString(R.string.pref_business),
                    res.getString(R.string.pref_magazine),
                    res.getString(R.string.pref_food),
                    res.getString(R.string.pref_health),
                    res.getString(R.string.pref_local_living),
                    res.getString(R.string.comics),
                    res.getString(R.string.pref_outlook),
                    res.getString(R.string.pref_travel),
                    res.getString(R.string.pref_arts),
                    res.getString(R.string.pref_weekend),
                    res.getString(R.string.pref_real_estate),
                    res.getString(R.string.pref_special_advertising)};
        } else {
            DAILY_SECTIONS.add(A_SECTION);
            DAILY_SECTIONS.add(METRO);
            DAILY_SECTIONS.add(SPORTS);
            DAILY_SECTIONS.add(STYLE);

            SECTIONS = new String[]{A_SECTION, METRO, SPORTS, STYLE, BUSINESS, MAGAZINE, FOOD, HEALTH, LOCAL_LIVING, COMICS, OUTLOOK, TRAVEL, ARTS, WEEKEND, REAL_ESTATE, SPECIAL_ADVERTISING};
        }
        dateFormat.setTimeZone(Utils.getDefaultAppTimeZone());
        loadPrintEditionAssets();
    }

    /**
     * Gets the folder where a given Archive's zip & content is stored on disk.
     * @param context android context
     * @param label date of the archive in question
     * @param sectionLetter the letter of the section in question
     * @return File pointing to folder containing archive.
     */
    public static File getArchiveFolder(Context context, long label, String sectionLetter) {
        return new File(ArchiveManager.getPdfRootFolder(context), Long.toString(label));
    }


    /**
     * Returns a File pointing to the zip archive for the date and section provided.
     * @param context android context
     * @param label date of the archive in question
     * @param sectionLetter the letter of the section in question
     * @return File pointing to zip file for date and section
     */
    public static File getZippedArchiveFile(Context context, long label, String sectionLetter) {
        return new File(ArchiveManager.getPdfRootFolder(context), String.format(Locale.US, ZIP_ARCHIVE_FORMAT, label, sectionLetter));
    }

    /**
     * Returns the full path of a file that's expected in an archive folder.
     * @param context android context
     * @param label date of the archive in question
     * @param sectionLetter the letter of the section in question
     * @param fileName file name to find
     * @return File pointing to a specific file within an archive folder
     */
    public static File getFullFilePath(Context context, long label, String sectionLetter, String fileName) {
        return new File(ArchiveManager.getArchiveFolder(context, label, sectionLetter).getPath() + File.separator + fileName);
    }

    /**
     * Returns a File pointing to the root folder where PDF files are stored.
     * @param context android context
     * @return File of root folder
     */
    static File getPdfRootFolder(Context context) {
        // TODO: define external storage policy
        File extDir = context.getExternalFilesDir(null);
        File pdfRootDir = new File(extDir, ARCHIVE_DIRECTORY);
        if (!pdfRootDir.exists()) {
            pdfRootDir.mkdirs();
        }
        return pdfRootDir;
    }

    /**
     * Takes a given date and section and schedules a Download to get the archive from the internet.
     * @param label date of the archive to be downloaded
     * @param sectionLetter section letter of the archive to be downloaded
     * @param sectionLmt lmt of the section as indicated by the manifest
     * @return Archive object with archive information and download ID connected to download.
     * @throws IOException throws exception if there's an error setting up the download
     */
    public Archive scheduleFileDownload(long label, String sectionLetter, long sectionLmt) throws IOException {
        File zip = getZippedArchiveFile(_context, label, sectionLetter);
        int year = (int)(label / 10000);
        int month = (int)((label % 10000) / 100);
        int day = (int)(label % 100);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DATE, day);
        String baseUrl = _printConfigStub.getNewsstandBaseURL();
        //Make non-retina bundle an option? Upgrade if only non-retina was DL'd? At the moment, product says retina only.
        String pathTemplate = _printConfigStub.getRetinaBundleURLTemplate();
        String archiveUrl = baseUrl + String.format(pathTemplate, label, sectionLetter);

        Download download = new Download(_context, Uri.parse(archiveUrl), zip.getPath());
        String downloadTitle;
        String downloadDescription;
        Resources res = _context.getResources();
        if (res != null) {
            String downloadTitleTemplate = res.getString(R.string.archive_download_notification_title_template);
            downloadTitle =
                    String.format(downloadTitleTemplate,
                            sectionLetter.equals(PREVIEW_SECTION) ? FORMATTED_PREVIEW : sectionLetter, downloadDateFormat.format(c.getTime())
                    );
            downloadDescription = res.getString(R.string.archive_download_notification_descriptor);
        } else {
            downloadTitle = "Washington Post Print Edition";
            downloadDescription = "A Washington Post section for the date.";
        }
        download.setTitle(downloadTitle);
        download.setDescription(downloadDescription);
        download.setMimeType("application/wapo-pdf");
        long downloadId = Download.enqueue(download);
        Logger.d(TAG, String.format("Downloading archive %s-%s with downloadId %s", label, sectionLetter, downloadId));

        if (res != null) {
            Measurement.trackPrintDownload(zip.getPath(), "epaperdownload", String.format("%s:%s", downloadDateFormat.format(c.getTime()), sectionLetter));
        }

        CacheManager cm = getCacheManager();
        Archive a = cm.getArchiveByLabelAndSection(label, sectionLetter);
        a = a == null ? new Archive(label, sectionLetter) : a;
        a.setStatus(Archive.Status.None);
        a.setDownloadId(downloadId);
        a.setLmt(sectionLmt);
        a.updateTimestamp();
        return cm.createArchive(a);
    }

    /**
     * Helper method to schedule file download for preview section for a given date.
     * @param label date of preview section to download
     * @return Archive object with archive information and download ID connected to download.
     */
    public Archive downloadPreviewArchiveForLabel(long label) {
        try {
            return scheduleFileDownload(label, PREVIEW_SECTION, 0);
        } catch (IOException e) {
            Logger.e(TAG, String.format("Error downloading preview for date=%s", label), e);
        }
        return null;
    }

    /**
     * Helper method to get synchronized preview archive for label.
     * @param label date of preview section for which to get synchronized archive
     * @return Archive object for preview from provided date
     * @throws IOException throws exception if archive folder does not exist and cannot be created
     * @throws DownloadManagerException thrown when download status is something other than pass, fail or in progress.
     */
    private Archive getSynchronizedPreviewArchiveForLabel(long label) throws IOException, DownloadManagerException {
        return getSynchronizedArchive(label, PREVIEW_SECTION);
    }

    public Observable<Archive> getSynchronizedPreviewArchiveForLabelObs(final long label) {
        return Observable.fromCallable(new Callable<Archive>() {
            @Override
            public Archive call() throws Exception {
                return getSynchronizedPreviewArchiveForLabel(label);
            }
        }).subscribeOn(scheduler);
    }

    /**
     * Returns an Archive object describing the current state of the archive for the given date and section.
     * If this is the first time an Archive is synchronized and the zip file has been totally downloaded,
     * this method will remove the download from the queue, unzip the zip file, and add the articles to the cache.
     * @param label date of section for which to get synchronized archive
     * @param sectionLetter section letter of desired archive
     * @return Archive object for preview from provided date
     * @throws IOException throws exception if archive folder does not exist and cannot be created
     * @throws DownloadManagerException thrown when download status is something other than pass, fail or in progress.
     */
    private Archive getSynchronizedArchive(long label, String sectionLetter) throws IOException, DownloadManagerException {
        CacheManager cm = getCacheManager();
        //
        // 1. find archive by label and section
        Archive archive = cm.getArchiveByLabelAndSection(label, sectionLetter);
        if (archive == null) {
            return null;
        }

        if (archive.getStatus() != Archive.Status.None) {
            Logger.w(TAG, String.format("Returning archive for %s-%s with abnormal status.", label, sectionLetter));
            return archive;
        }

        final Long downloadId = archive.getDownloadId();

        if (downloadId == null) {
            if (archive.getPath() != null && !Utils.exists(archive.getPath())) {
                // data inconsistency: reload
                Logger.w(TAG, String.format("Archive path does not exist for %s-%s. Deleting.", label, sectionLetter));
                cm.deleteArchive(archive.getId());
                return null;
            }
            Logger.d(TAG, String.format("Returning archive for %s-%s.", label, sectionLetter));
            return archive;
        }

        Download download = Download.get(_context, downloadId);
        if (download == null) {
            // archive record is invalid; delete
            Logger.w(TAG, String.format("Download object not found for %s-%s.  Deleting.", label, sectionLetter));
            cm.deleteArchive(archive.getId());
            return null;
        }

        int status = download.getStatus();
        if (status == DownloadManager.STATUS_FAILED) {
            if (download.getReason() == DownloadManager.ERROR_FILE_ALREADY_EXISTS) {
                status = DownloadManager.STATUS_SUCCESSFUL;
            } else {
                throw new DownloadManagerException(download.getReason());
            }
        }

        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            File archiveFile = getZippedArchiveFile(_context, label, sectionLetter);
            if (!archiveFile.exists()) {
                // data inconsistency: reload
                Logger.w(TAG, String.format("Zip archive not found for %s-%s. Deleting archive entry and download object.", label, sectionLetter));
                cm.deleteArchive(archive.getId());
                Download.remove(_context, download.getId());
                return null;
            }
            File targetFolder = getArchiveFolder(_context, label, sectionLetter);
            if (!targetFolder.exists()) {
                if (!targetFolder.mkdirs()) {
                    throw new IOException("Unable to create archive's folder " + targetFolder.getName());
                }
            }

            //Set downloadId to null before unzipping so that only one unzipping attempt happens at once.
            archive.setDownloadId(null);
            archive.setPath(targetFolder.getPath());
            cm.updateArchive(archive);

            ZipUtil.extract(archiveFile, targetFolder);
            Download.remove(_context, downloadId);

            //Monitor behavior as it can be erratic.
            processBundle(archive);
        }

        return archive;
    }

    public Observable<Archive> getSynchronizedArchiveObs(final long label, final String sectionLetter) {
        return Observable.fromCallable(new Callable<Archive>() {
            @Override
            public Archive call() throws Exception {
                return getSynchronizedArchive(label, sectionLetter);
            }
        }).subscribeOn(scheduler);
    }

    private void processBundle(final Archive archive) throws IOException {
        //Consider whether having it on separate executor service is wise. Should prevent file locking / DB locking issues, but does it slow it down?
        Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return processArticles(archive);
            }
        }).subscribeOn(fileServiceScheduler).subscribe();
    }

    private boolean processArticles(final Archive archive) {
        if (archive == null) {
            Logger.w(TAG, "Failed to process null archive.");
            return false;
        }
        final CacheManager cm = getCacheManager();
        final long label = archive.getDate();
        final String sectionLetter = archive.getSection();
        final File archiveFile = getZippedArchiveFile(_context, label, sectionLetter);
        final File targetFolder = getArchiveFolder(_context, label, sectionLetter);
        try {
            long articleStartTime = System.currentTimeMillis();
            Logger.d(TAG, String.format("Starting print article caching for %s-%s", label, sectionLetter));
            File pcdataFile = new File(targetFolder, String.format(Locale.US, BUNDLE_MANIFEST_JSON_FILE_FORMAT, sectionLetter));

            if (!pcdataFile.exists() || !pcdataFile.canRead()) {
                throw new IOException("Unable to find / read pcdata file for " + archiveFile.getAbsolutePath());
            }
            ContentBundle bundle;

            FileInputStream fileInputStream = new FileInputStream(pcdataFile);
            String jsonStr = Utils.inputStreamToString(fileInputStream);
            JSONArray articles = new JSONArray(jsonStr);
            fileInputStream.close();

            bundle = cm.getArchiveBundleByLabel(label);
            if (bundle == null) {
                Logger.d(TAG, "Creating new bundle for " + label);
                bundle = new ContentBundle(Long.toString(label), ContentBundle.Type.Archive);
                bundle = cm.createBundle(bundle);
            }

            Logger.d(TAG, String.format("%s-%s pcdata json array contains %s articles.", label, sectionLetter, articles.length()));

            // extract native articles from bundle
            for (int i = 0, len = articles.length(); i < len; i++) {
                JSONObject jsonNativeContent = articles.getJSONObject(i);
                String contenturl = jsonNativeContent.optString("contenturl", null);
                long lmt = jsonNativeContent.optLong("lmt", 0);

                if (contenturl == null) {
                    String id = jsonNativeContent.optString("id", null);
                    Logger.e(TAG, String.format("article with id: %s: contentUrl is null", id));
                }

                //
                // create file
                long hash = CacheManager.getHashCode(contenturl);

                FileMeta fm = cm.getFileMetaByHash(hash);
                if (fm != null && fm.getServerDate() > lmt) {
                    continue;
                }

                String path = fm == null ? _cacheManager.getPathByHash(hash) : fm.getPath();

                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
                    bw.write(jsonNativeContent.toString());
                    bw.flush();
                    bw.close();
                } catch (Exception e) {
                    Logger.e(TAG, Utils.exceptionToString(e));
                    continue;
                }

                if (fm == null) {
                    fm = new FileMeta(bundle.getId(), path, contenturl, hash, "UTF-8", "text/html", lmt, lmt);
                    cm.createOrMergeFileMeta(fm);
                } else {
                    fm.setServerDate(lmt == 0 ? System.currentTimeMillis() : lmt);
                    cm.updateFileMeta(fm);
                }
            }
            Logger.d(TAG, String.format("Finishing print article caching for %s-%s in %s ms.", label, sectionLetter, (System.currentTimeMillis() - articleStartTime)));

            // process images
            File imagesJson = new File(targetFolder, sectionLetter + "-images.json");
            if (!imagesJson.exists() || !imagesJson.canRead()) {
                Logger.w(TAG, String.format("Images JSON not found for %s-%s", label, sectionLetter));
                return true;
            }
            long imagesStartTime = System.currentTimeMillis();
            Logger.d(TAG, String.format("Starting print article image caching for %s-%s.", label, sectionLetter));
            processImages(imagesJson, targetFolder, cm, bundle);
            Logger.d(TAG, String.format("Finishing print article image caching for %s-%s in %s ms.", label, sectionLetter, (System.currentTimeMillis() - imagesStartTime)));
            return true;

        } catch (Exception e) {
            Logger.e(TAG, String.format("Error unbundling print articles for %s-%s", label, sectionLetter), e);
            return false;
        }
    }

    public Observable<Boolean> getProcessArticlesObs(final Archive archive){
        return Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //Set the download id to -1 because it won't be used when archive has already been loaded.
                return processArticles(archive);
            }
        }).subscribeOn(fileServiceScheduler);
    }

    /**
     * Method from processing images from bundle included JSON.
     * @param imagesJson file where the json describing images is located.
     * @param targetFolder target folder where images should be placed.
     * @param cm reference to the cache manager instance.
     * @param bundle reference to the content bundle for the archive.
     */
    private void processImages(File imagesJson, File targetFolder, CacheManager cm, ContentBundle bundle) {
        try {
            FileInputStream fileInputStream = new FileInputStream(imagesJson);
            Section imagesSection = Section.parseJson(Utils.inputStreamToString(fileInputStream));
            fileInputStream.close();
            byte[] buff = new byte[8192];
            for (ResourceManifest rm : imagesSection.getArticles()) {
                String sourcePath = rm.getFilePath();
                if (sourcePath == null) {
                    Uri uri = Uri.parse(rm.getUrl());
                    sourcePath = uri.getHost() + uri.getEncodedPath() + uri.getEncodedQuery();
                    if (sourcePath.endsWith("/")) {
                        sourcePath = sourcePath.substring(0, sourcePath.length() - 1);
                    }
                }

                File sourceFile = new File(targetFolder, ZipUtil.adjustFileName(sourcePath));
                if (!sourceFile.exists()) {
                    continue;
                }

                long lmt = rm.getLmt();
                FileMeta fm = cm.getFileMetaByUrl(rm.getUrl());
                if (fm != null) {
                    if (fm.getServerDate() >= lmt) {
                        //
                        // no need to keep duplicates
                        if (!sourceFile.delete()) {
                            Logger.w(TAG, "Unable to delete file: " + sourceFile.getPath());
                        }
                        continue;
                    }
                }

                if (fm == null) {
                    long hash = CacheManager.getHashCode(rm.getUrl());
                    String path = _cacheManager.getPathByHash(hash);
                    fm = new FileMeta(
                            bundle.getId(),
                            path,
                            rm.getUrl(),
                            hash,
                            rm.getContentType(),
                            null,
                            lmt > 0 ? lmt : System.currentTimeMillis(),
                            lmt
                    );
                } else {
                    fm.setServerDate(lmt);
                    if (fm.getBundleId() == null) {
                        fm.setBundleId(bundle.getId());
                    }
                }

                File targetFile = new File(fm.getPath());
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
                try {
                    int len = 0;
                    while ((len = bis.read(buff)) > 0) {
                        bos.write(buff, 0, len);
                    }
                    bos.flush();
                    bos.close();
                    bis.close();
                } catch (IOException ioEx) {
                    Logger.e(TAG, "Unable to copy file:" + sourceFile.getPath() + " -> " + targetFile.getPath());
                    Logger.e(TAG, Log.getStackTraceString(ioEx));
                    bis.close();
                    bos.close();
                    continue;
                }

                if (!sourceFile.delete()) {
                    Logger.w(TAG, "Unable to delete: " + sourceFile.getPath());
                }

                if (fm.isNew()) {
                    cm.createOrMergeFileMeta(fm);
                } else {
                    cm.updateFileMeta(fm);
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, Log.getStackTraceString(e));
        }
    }


    public List<Archive> getArchivesByLabel(long label) {
        return getCacheManager().getArchivesByLabel(label);
    }

    public Archive getArchiveByLabelAndSection(long label, String sectionLetter) {
        return getCacheManager().getArchiveByLabelAndSection(label, sectionLetter);
    }

    public Archive getPreviewArchiveForLabel(long label) {
        return getArchiveByLabelAndSection(label, PREVIEW_SECTION);
    }

    private Archive getArchiveByDownloadId(long id) {
        return getCacheManager().getArchiveByDownloadId(id);
    }

    public Observable<Archive> getArchiveByDownloadIdObs(final long id) {
        return Observable.fromCallable(new Callable<Archive>() {
            @Override
            public Archive call() throws Exception {
                return getArchiveByDownloadId(id);
            }
        }).subscribeOn(scheduler);
    }


    /**
     * Returns a list of archives that matches the given query, or all archives is query is null.
     * @param query query used to search sqlite list of archives
     * @return list of Archive objects
     */
    List<Archive> getArchives(String query) {
        return getCacheManager().getArchives(query);
    }

    /**
     * Helper method to delete all archives from SQLite database.
     */
    private static void wipeArchives() {
        FlagshipApplication.getInstance().getCacheManager().wipeArchives();
    }

    /**
     * Helper method to delete an archive asynchronously.
     * @param archive archive to delete
     * @param fullDelete boolean to determine if all archive content is deleted, or preview is retained
     * @return observable to subscribe to progress of deleting the archive.
     */
    public Observable<Boolean> deleteArchiveAsync(final Archive archive, final boolean fullDelete) {
        return Observable.just(deleteArchive(archive, fullDelete)).subscribeOn(fileServiceScheduler);
    }

    //Returns a boolean based on if it succeeded.
    private boolean deleteArchive(final Archive archive, final boolean fullDelete) {
        if (!deleteDownloadForArchive(archive)) {
            return false;
        }

        boolean result = deleteZipFileForArchive(archive);
        if (fullDelete) {
            File pathFolder = ArchiveManager.getArchiveFolder(getContext(), archive.getDate(), archive.getSection());
            if (pathFolder.exists() && pathFolder.canWrite()) {
                result = result && Utils.deleteFileOrFolder(pathFolder);
            }
        } else {
            //Perform this delete while maintaining preview pages.
            result = result && deleteFilesForArchivePath(archive.getDate(), archive.getSection(), false);
        }
        Logger.d(TAG, String.format(Locale.US, "Deleting archive with section=%s date=%s result=%b",
                archive.getSection(), archive.getDate(), result));

        if (result) {
            //Only delete archive record from table if actual files were deleted.  Otherwise we leak space.
            removeArchiveFromTable(archive);
            Logger.d(TAG, String.format("Finished deleting archive with section=%s date=%s from table.", archive.getSection(), archive.getDate()));
        } else {
            Logger.e(TAG, String.format("Failed to delete archive with section=%s date=%s, leaving in table.", archive.getSection(), archive.getDate()));
        }

        return result;
    }

    /**
     * Method to remove the download for the archive in question.
     * @param archive archive that needs its download removed, if it exists
     * @return returns false if the download is currently active, or if archive is null.
     */
    private boolean deleteDownloadForArchive(Archive archive) {
        if (archive == null) {
            return false;
        }
        Long downloadId = archive.getDownloadId();
        final Download download = downloadId == null ? null : Download.get(getContext(), downloadId);
        if (download != null) {
            int status = download.getStatus();
            if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
                return false;
            }
            Download.remove(getContext(), download.getId());
        }
        return true;
    }

    private boolean deleteZipFileForArchive(Archive archive) {
        boolean result = true;
        File pathZip = getZippedArchiveFile(getContext(), archive.getDate(), archive.getSection());
        if (pathZip.exists() && pathZip.canWrite()) {
            result = Utils.deleteFileOrFolder(pathZip);
        }
        return result;
    }

    private void removeArchiveFromTable(Archive archive) {
        CacheManager cm = getCacheManager();
        ContentBundle bundle = cm.getArchiveBundleByLabel(archive.getDate());
        if (bundle != null) {
            cm.deleteBundle(bundle, false);
        }
        cm.deleteArchive(archive.getId());
    }

    public void updateArchive(Archive archive) {
        getCacheManager().updateArchive(archive);
    }

    private boolean deleteFilesForArchivePath(long date, String sectionLetter, boolean fullDelete) {
        return deleteFilesForArchivePath(ArchiveManager.getArchiveFolder(getContext(), date, sectionLetter), sectionLetter, fullDelete);
    }


    private boolean deleteFilesForArchivePath(File targetDir, String sectionLetter, boolean fullDelete) {
        if (targetDir.exists() && targetDir.canWrite() && targetDir.isDirectory()) {
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (deleteArchiveFile(f.getName(), sectionLetter, fullDelete)) {
                        if (!f.delete()) {
                            return false;
                        }
                    }
                }
                if (targetDir.listFiles() == null || targetDir.listFiles().length == 0) {
                    return targetDir.delete();
                }
                return true;
            }
        }
        return false;
    }

    //Consider a regex for this?
    private static boolean deleteArchiveFile(String fileName, String sectionLetter, boolean fullDelete) {
        if (fileName == null || sectionLetter == null || fileName.length() < 2) {
            return false;
        }
        return (fileName.startsWith(sectionLetter) && (fullDelete || !(fileName.startsWith("01", 1) || fileName.startsWith("02", 1))));
    }

    public Observable<PrintManifestResponse> getPrintManifest(Date date) {
        String dateStr = dateFormat.format(date);
        Logger.d(TAG, String.format("Downloading manifest for %s", dateStr));
        String fileUrl = String.format(_printConfigStub.getMetadataURLTemplateLmt(), dateStr);

        Observable<PrintManifestResponse> obs = printApiAWSService != null ?
                (printApiAWSService.getPrintManifestObs(fileUrl)) : Observable.<PrintManifestResponse>error(new NullPointerException("printApiAWSService is null."));
        return obs.subscribeOn(scheduler);
    }

    public void performDailyUpdate() {
        Date currentDate = getCurrentDate();
        setPrintEditionDate(getContext(), currentDate.getTime());
        performDailyUpdate(currentDate);
    }

    private void performDailyUpdate(Date date) {

        //Start by cleaning out old archives.  This should happen regardless of update settings.
        cleanOldArchives(date);

        //Next, manage the auto-download toggles.  This needs to be done regardless of daily download.
        if (SECTIONS != null && !isPrintEditionFirstRun(getContext())) {
            for (String section : SECTIONS) {
                countSectionReads(section);
            }
        }

        if (_printConfigStub != null && !_printConfigStub.getDailyDownloadEnabled()) {
            Logger.d(TAG, "Daily update skipped, feature disabled via config.");
            return;
        }
        if (isDailyDownloadOn()) {
            Logger.d(TAG, "Performing daily update for " + Utils.dateToEDTLabel(date));
            getPrintManifest(date).subscribe(new Subscriber<PrintManifestResponse>() {
                @Override
                public void onCompleted() {}

                @Override
                public void onError(Throwable e) {
                    Logger.e(TAG, "Error getting manifest for daily update.", e);
                }

                @Override
                public void onNext(PrintManifestResponse printManifestResponse) {
                    downloadNeededSections(printManifestResponse);
                }
            });
        } else {
            Logger.d(TAG, "Daily update skipped, disabled via settings.");
        }
    }

    public void downloadNeededSections(PrintManifestResponse printManifestResponse) {
        long pubdate = printManifestResponse.getIssue().getPubdate();
        if ((isPrintDownloadWifiOnly() && !ReachabilityUtil.isOnWiFi(getContext()))
                || ConnectivityMonitor.getInstance(getContext()).hasDeviceLevelDataRestriction()) {
            Logger.d(TAG, "Skipping downloads, wifi/data requirement not met.");
            return;
        }
        if (PREVIEW_ENABLED && (getPreviewArchiveForLabel(pubdate) == null)) {
            downloadPreviewSectionIfNeeded(pubdate);
        }
        for (PrintSection section : printManifestResponse.getIssue().getSections()) {
            long sectionLmt = section.getLmt() != null ? Long.parseLong(section.getLmt()) : 0;
            //This works because the tag names for the preferences are the exact section names.
            downloadSectionIfNeeded(pubdate, section.getSectionLetter(), isAutoDownloadEnabledForSection(section.getSectionName()), sectionLmt);
        }
    }

    public boolean isPrintEditionPushEnabled() {
        return AppContext.isTopicEnabled(PushListener.TODAY_PAPER_TOPIC_NAME);
    }

    private void countSectionReads(String sectionName) {
        //If the user has manually set this at any point, stop here.  No point in going on, as no changes will be made.
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(sectionName + "Locked", false)) {
            return;
        }
        HashSet<String> readStrings = getSectionReads(sectionName);

        boolean isDaily = DAILY_SECTIONS.contains(sectionName);
        long currentTime = System.currentTimeMillis();
        String[] readStringsArray = readStrings.toArray(new String[readStrings.size()]);
        for (String readString : readStringsArray) {
            try {
                long readTime = Long.parseLong(readString);
                //If it's been a given length of time since the view was recorded, remove it.
                //Length of time is based on if the section is daily or weekly.
                long timePassedSinceRead = currentTime - readTime;
                if (timePassedSinceRead > (Utils.oneDayinMilliseconds * (isDaily ? 7.00 : 30.00))) {
                    readStrings.remove(readString);
                }
            } catch (NumberFormatException e) {
                Logger.e(TAG, "Error reading read times.", e);
                readStrings.remove(readString);
            }
        }
        //Once all removing is done, save the new trimmed date set.
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putStringSet(sectionName + "ReadArray", readStrings).commit();
        int readCount = readStrings.size();
        //If they had enough views in the time frame, then enable the auto-download.
        //Default is 3 per week for daily sections, 2 per month for weekly sections.
        if (readCount >= (isDaily ? 3 : 2)) {
            setAutoDownloadEnabledForSection(sectionName, true);
        }
        //If there are no views in the time frame, then disable the auto-download.
        else if (readCount == 0) {
            setAutoDownloadEnabledForSection(sectionName, false);
        }
    }

    private HashSet<String> getSectionReads(String sectionName) {
        return (HashSet<String>) PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet(sectionName + "ReadArray", new HashSet<String>(5));
    }

    /**
     * Add a read to the read counter for each section.  This causes the smart toggles to not set to off on first load.
     * @return if all sections had reads added successfully
     */
    public boolean setupSectionReadsForFirstRun() {
        boolean result = true;
        for (String section : SECTIONS) {
            result = result && addSectionRead(section);
        }
        return result;
    }

    public static boolean isSmartLogicInitialized(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SMART_LOGIC_SETUP, false);
    }

    public static boolean setSmartLogicInitialized(Context context, boolean isSetup) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_SMART_LOGIC_SETUP, isSetup).commit();
    }

    public boolean addSectionRead(String sectionName) {
        HashSet<String> reads = getSectionReads(sectionName);
        reads.add(Long.toString(System.currentTimeMillis()));
        return PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putStringSet(sectionName + "ReadArray", reads).commit();
    }

    public boolean lockSection(String sectionName) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(sectionName + "Locked", true).commit();
    }

    public void enablePrintEditionPush() {
        FlagshipApplication.getInstance().getAlertsSettings().enableAlertsTopic(PushListener.TODAY_PAPER_TOPIC_NAME, true);
    }

    public void setDailyDownloadOn(boolean dailyDownloadOn) {
        AppPreferences.INSTANCE.setDownloadDailyPaperOn(dailyDownloadOn);
    }

    private boolean isDailyDownloadOn() {
        return AppPreferences.INSTANCE.isDownloadDailyPaperOn();
    }

    private boolean isAutoDownloadEnabledForSection(String sectionName) {
        //Needs to be true so that sections auto-download on first run.
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(sectionName, true);
    }

    private void setAutoDownloadEnabledForSection(String sectionName, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(sectionName, enabled).apply();
    }

    private void downloadPreviewSectionIfNeeded(long label) {
        downloadSectionIfNeeded(label, PREVIEW_SECTION, true, 0);
    }

    private void downloadSectionIfNeeded(final long label, final String sectionLetter, final boolean isAutoDownloadEnabled, final long sectionLmt) {
        getSynchronizedArchiveObs(label, sectionLetter).subscribe(new Subscriber<Archive>() {
            @Override
            public void onCompleted() {}
            @Override
            public void onError(Throwable e) {
                Logger.e(TAG, String.format("Error checking if section %s-%s needs to be downloaded.",
                        label, sectionLetter), e);
            }
            @Override
            public void onNext(Archive archive) {
                boolean result = false;
                if (archive == null || archive.getStatus() == Archive.Status.Canceled) {
                    //Archive doesn't exist or was canceled.
                    if (isAutoDownloadEnabled) {
                        //Auto-download is enabled, meaning the app will download the latest version.
                        try {
                            scheduleFileDownload(label, sectionLetter, sectionLmt);
                            result = true;
                        } catch (IOException e) {
                            Logger.e(TAG, String.format("Error scheduling auto-download of section %s-%s.", label, sectionLetter), e);
                            result = false;
                        }
                    } else {
                        Logger.d(TAG, String.format("Auto download disabled for section %s-%s.", label, sectionLetter));
                    }
                } else if (archive.getStatus() == Archive.Status.Deleted) {
                    Logger.d(TAG, String.format("Skipping section %s-%s download because of previous deletion.", label, sectionLetter));
                } else {
                    downloadIfArchiveResetNeeded(archive, sectionLmt);
                }
                Logger.d(TAG, String.format("Downloading %s section (%s) = %b", label, sectionLetter, result));
            }
        });
    }

    private void downloadIfArchiveResetNeeded(Archive archive, long sectionLmt) {
        final long label = archive.getDate();
        final String sectionLetter = archive.getSection();
        final long currentArchiveLmt = archive.getLmt();
        //The app has already downloaded the archive, and we should check the lmt to see if a delete and re-download is needed.
        if (sectionLmt <= 0) {
            Logger.w(TAG, String.format("Section %s-%s was unable to find a lmt in the manifest.", label, sectionLetter));
            return;
        }
        //TODO: Consider the behavior if archive.getLmt is 0 but sectionLmt isn't. Should we just clear and redownload?
        if (currentArchiveLmt < sectionLmt) {
            if (!deleteDownloadForArchive(archive)) {
                Logger.w(TAG, String.format("Section %s-%s is currently downloading, ending lmt check.", label, sectionLetter));
                return;
            }
            boolean deleteSuccessful = deleteZipFileForArchive(archive);
            deleteSuccessful = deleteSuccessful && deleteFilesForArchivePath(label, sectionLetter, true);
            if (deleteSuccessful) {
                Logger.d(TAG, String.format("Section %s-%s with old lmt %s has been deleted successfully.", label, sectionLetter, currentArchiveLmt));
                removeArchiveFromTable(archive);
                //Schedule a new download of the section.
                try {
                    scheduleFileDownload(label, sectionLetter, sectionLmt);
                } catch (IOException e) {
                    Logger.e(TAG, String.format("Error scheduling redownload of section %s-%s with expired lmt %s.", label, sectionLetter, currentArchiveLmt), e);
                }
            } else {
                Logger.w(TAG, String.format("Section %s-%s with old lmt encountered an issue while deleting.", label, sectionLetter));
            }
        } else {
            Logger.d(TAG, String.format("Section %s-%s is up-to-date with current lmt %s (old lmt %s).", label, sectionLetter, currentArchiveLmt, sectionLmt));
        }
    }

    private void cleanOldArchives(Date date) {
        long pubdate = Utils.dateToEDTLabel(date);
        double initialSize = getPrintEditionSizeInDisk(getContext());
        Logger.d(TAG, String.format(Locale.US, "Cleaning out archives for dateParam=%s initial dir size=%.2f MB", pubdate, initialSize));
        for (Archive archive : getArchives(null)) {
            Logger.d(TAG, String.format("Reviewing archive with date=%s Timestamp=%s", archive.getDate(), archive.getTimestamp()));
            Date currentArchiveDate = Utils.edtLabelToDate(archive.getDate());
            //If the archive is older than the specified number of days (default of 14), mark it for deletion.
            boolean staleDate = currentArchiveDate == null || (currentArchiveDate.getTime() <= (date.getTime() - (Utils.oneDayinMilliseconds * STALE_DATE_COUNT)));
            boolean deleted = false;
            //If the date is stale, then we can just delete the whole folder for that day.  Save processing time.
            if (staleDate) {
                deleted = deleteArchive(archive, true);
            }
            if (deleted) {
                Logger.d(TAG, String.format(Locale.US, "Deleted archive with section=%s date=%s, over %d days old=%b",
                        archive.getSection(), archive.getDate(), STALE_DATE_COUNT, staleDate));
            } else if (staleDate) {
                Logger.e(TAG, String.format(Locale.US, "Failed to delete archive with section=%s date=%s, over %d days old=%b",
                        archive.getSection(), archive.getDate(), STALE_DATE_COUNT, staleDate));
            } else {
                Logger.d(TAG, String.format(Locale.US, "Skipped archive with section=%s date=%s", archive.getSection(), archive.getDate()));
            }
        }
        double newSize = getPrintEditionSizeInDisk(getContext());
        Logger.d(TAG, String.format(Locale.US, "Finished cleaning archives for dateParam=%s new dir size=%.2f MB, removed=%.2f", pubdate, newSize, initialSize - newSize));
    }


    public CacheManager getCacheManager() {
        return _cacheManager;
    }

    public Context getContext() {
        return _context;
    }


    public static class DownloadManagerException extends Exception {
        private int _reason;

        DownloadManagerException(int reason) {
            _reason = reason;
        }

        public int getReason() {
            return _reason;
        }
    }

    public static boolean deleteArchiveFiles(Context ctx) {
        wipeArchives();
        return Utils.deleteFileOrFolder(getPdfRootFolder(ctx));
    }

    public static double getPrintEditionSizeInDisk(Context ctx) {
        long bytes = FileUtils.dirSize(getPdfRootFolder(ctx));
        return (bytes / (1024D * 1024D));
    }

    private static String getAssetFilePath(Context ctx, String fileName) {
        return ctx.getFilesDir().getPath() + File.separator + fileName;
    }

    public static String getPlaceholderPDFFilePath(Context ctx) {
        return getAssetFilePath(ctx, PLACEHOLDER_PDF_FILENAME);
    }

    public static String getTutorialAssetFilePath(Context ctx, String fileName) {
        return getAssetFilePath(ctx, fileName);
    }

    public static String getTutorialCoverImageFilename(Context ctx) {
        return getAssetFilePath(ctx, TUTORIAL_PAGE_ONE_HIGH_RES_FILENAME);
    }

    public static ArrayList<PrintSectionPage> getTutorialPages() {
        ArrayList<PrintSectionPage> pages = new ArrayList<>(2);
        pages.add(new PrintSectionPage(TUTORIAL_PAGE_ONE_NAME, DEFAULT_PAGE_HEIGHT, DEFAULT_PAGE_WIDTH, TUTORIAL_PAGE_ONE_HIGH_RES_FILENAME,
                TUTORIAL_PAGE_ONE_THUMBNAIL_FILENAME, TUTORIAL_PAGE_ONE_PDF_FILENAME, 1, null, TUTORIAL_SECTION_NAME, null));
        pages.add(new PrintSectionPage(TUTORIAL_PAGE_TWO_NAME, DEFAULT_PAGE_HEIGHT, DEFAULT_PAGE_WIDTH, null,
                TUTORIAL_PAGE_TWO_THUMBNAIL_FILENAME, TUTORIAL_PAGE_TWO_PDF_FILENAME, 2, null, TUTORIAL_SECTION_NAME, null));

        return pages;
    }

    //Consider change in logic for getting print edition date.
    //At the moment, we just subtract a day if the user is a day ahead for time / timezone reasons.
    public static Date getCurrentDate() {
        return new Date();
    }

    public static Date getPrintEditionDate(Context context) {
        long lastViewedCutoff = System.currentTimeMillis() - (ConfigManager.Companion.getInstance().getConfig().getPrintConfig().getArticleDateTimeoutHours() * Utils.oneHourinMilliseconds);
        if (lastViewedCutoff > getLastViewedTime(context)) {
            Date currentDate = getCurrentDate();
            setPrintEditionDate(context, currentDate.getTime());
            return currentDate;
        }
        return new Date(PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_PRINT_DATE, System.currentTimeMillis()));
    }

    public static boolean setPrintEditionDate(Context context, long lastViewedTime) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PREF_PRINT_DATE, lastViewedTime).commit();
    }

    private static long getLastViewedTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_LAST_VIEWED_TIME, System.currentTimeMillis());
    }

    public static boolean setLastViewedTime(Context context, long lastViewedTime) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PREF_LAST_VIEWED_TIME, lastViewedTime).commit();
    }

    public static boolean isPrintTutorialEnabled(){
        return AppPreferences.INSTANCE.isPrintTutorialEnabled();
    }

    public static void setPrintTutorialEnabled(boolean showPrintTutorial) {
        AppPreferences.INSTANCE.setPrintTutorialEnabled(showPrintTutorial);
    }

    public static boolean isPrintEditionFirstRun(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_PRINT_FIRST_RUN, true);
    }

    public static boolean setPrintEditionFirstRun(Context context, boolean isFirstRun) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_PRINT_FIRST_RUN, isFirstRun).commit();
    }

    public static int getPrintEditionRunCount(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_PRINT_RUN_COUNT, 0);
    }

    public static boolean setPrintEditionRunCount(Context context, int runCount) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_PRINT_RUN_COUNT, runCount).commit();
    }

    boolean isPrintDownloadWifiOnly(){
        return AppPreferences.INSTANCE.isUpdatesOnWifiOnlyEnabled();
    }

    public void loadPrintEditionAssets() {
        final Context ctx = getContext();
        //Needed to add a locally accessible placeholder pdf for Print Edition.
        fileProcessingExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                loadAsset(ctx, getPlaceholderPDFFilePath(ctx), PLACEHOLDER_PDF_FILENAME);
                loadAsset(ctx, getTutorialCoverImageFilename(ctx), TUTORIAL_PAGE_ONE_HIGH_RES_FILENAME);
                loadAsset(ctx, getTutorialAssetFilePath(ctx, TUTORIAL_PAGE_ONE_THUMBNAIL_FILENAME), TUTORIAL_PAGE_ONE_THUMBNAIL_FILENAME);
                loadAsset(ctx, getTutorialAssetFilePath(ctx, TUTORIAL_PAGE_TWO_THUMBNAIL_FILENAME), TUTORIAL_PAGE_TWO_THUMBNAIL_FILENAME);
                loadAsset(ctx, getTutorialAssetFilePath(ctx, TUTORIAL_PAGE_ONE_PDF_FILENAME), TUTORIAL_PAGE_ONE_PDF_FILENAME);
                loadAsset(ctx, getTutorialAssetFilePath(ctx, TUTORIAL_PAGE_TWO_PDF_FILENAME), TUTORIAL_PAGE_TWO_PDF_FILENAME);
            }
        });

    }

    private static void loadAsset(Context ctx, String filePath, String fileName) {
        try {
            if (!new File(filePath).exists()) {
                copyFromAssetsToStorage(ctx, fileName, filePath);
            }
        } catch (IOException | NullPointerException e) {
            Logger.e(ctx.getPackageName(), String.format("Failure to load asset %s at path %s.", fileName, filePath), e);
        }
    }

    private static void copyFromAssetsToStorage(Context ctx, String sourceFile, String destinationFile) throws IOException {
        InputStream inputStream = ctx.getAssets().open(sourceFile);
        OutputStream outputStream = new FileOutputStream(destinationFile);
        copyStream(inputStream, outputStream);
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }
    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[5120];
        int length = inputStream.read(buffer);
        while (length > 0) {
            outputStream.write(buffer, 0, length);
            length = inputStream.read(buffer);
        }
    }

    /**
     * Method written to assist with unit testing.  This should not be used outside of testing.
     * @param scheduler default scheduler to use with ArchiveManager.
     */
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Method written to assist with unit testing.  This should not be used outside of testing.
     * @param fileServiceScheduler file processing scheduler to use with ArchiveManager.
     */
    void setFileServiceScheduler(Scheduler fileServiceScheduler) {
        this.fileServiceScheduler = fileServiceScheduler;
    }

    public String[] getSectionsList() {
        return SECTIONS;
    }
}
