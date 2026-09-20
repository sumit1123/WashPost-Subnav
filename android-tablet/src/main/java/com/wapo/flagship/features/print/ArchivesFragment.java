package com.wapo.flagship.features.print;

import static com.wapo.android.commons.util.URLParserKt.getCanonicalUrl;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.commons.util.Download;
import com.wapo.android.commons.util.Logger;
import com.wapo.android.remotelog.logger.EventTimerLog;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.wapomain.MainActivity;
import com.wapo.flagship.Utils;
import com.wapo.flagship.data.Archive;
import com.wapo.flagship.data.ArchiveManager;
import com.wapo.flagship.di.app.modules.features.readinghistory.ReadingHistoryViewModel;
import com.wapo.flagship.features.sections.ConnectivityActivity;
import com.wapo.flagship.features.shared.fragments.TopBarFragment;
import com.wapo.flagship.features.splash.SplashViewModel;
import com.wapo.flagship.model.PrintManifestResponse;
import com.wapo.flagship.model.PrintSection;
import com.wapo.flagship.model.PrintSectionPage;
import com.wapo.flagship.navigation.ui.BottomTab;
import com.wapo.flagship.network.HttpUtil;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.flagship.util.tracking.states.NavigationBehavior;
import com.wapo.flagship.wrappers.CrashWrapper;
import com.wapo.view.ProportionalLayout;
import com.washingtonpost.android.R;
import com.washingtonpost.android.save.database.model.MetadataModel;
import com.washingtonpost.android.save.database.model.ReadingHistoryModel;
import com.washingtonpost.android.save.views.ArticleListViewModel;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

@AndroidEntryPoint
public class ArchivesFragment extends Fragment implements PrintSectionClickListener {


    public static final String TAG = ArchivesFragment.class.getName();
    public static final String SCREEN_NAME = "Print";
    private static final SimpleDateFormat DayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.US);
    private static final String DateErrorsParam = ArchivesFragment.class.getSimpleName() + ".DateErrors";
    private static final String PreviewDownloadIdParam = ArchivesFragment.class.getSimpleName() + ".PreviewDownloadId";
    private static final int REQUEST_CODE = 1;
    private static final int MAX_DATE_ERRORS = 1;
    private static final int PUSH_PROMPT_THRESHOLD = 3;

    private CompositeSubscription compositeSubscription;
    private ArchiveManager archivesManager;
    private ViewGroup _view;
    private View downloadPrompt;
    private View noEditionFoundBar;
    private View pushPrompt;
    private List<ProgressBar> downloadBarList;
    private RecyclerView previewRecyclerView;
    private ProgressBar recyclerProgressBar;
    private TextView dayLabel;
    private TextView dateLabel;
    private MyHandler mHandler;
    private PrintManifestResponse manifest;
    private BroadcastReceiver mReceiver;
    private long previewDownloadId = -1;
    private boolean previewDownloaded = false;
    private Archive previewArchive = null;
    private Date archivesDate = null;
    private int dateErrors;
    private long lastViewedTime;
    private List<ArchiveItem> archiveItems;
    private ArticleListViewModel articleListViewModel;
    private ReadingHistoryViewModel readingHistoryViewModel;
    private SplashViewModel splashViewModel;

    public ArchivesFragment() {}

    public static ArchivesFragment newInstance() {
        return new ArchivesFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CrashWrapper.logExtras("Attach ArchivesFragment");
    }

    @Override
    public void onDetach() {
        CrashWrapper.logExtras("Detach ArchivesFragment");
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            dateErrors = savedInstanceState.getInt(DateErrorsParam, 0);
            previewDownloadId = savedInstanceState.getLong(PreviewDownloadIdParam, -1);
        } else {
            dateErrors = 0;
        }

        initReadingHistoryViewModel();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                try {
                    ArchiveManager am = FlagshipApplication.getInstance().getArchiveManager();
                    final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        if (ArchiveManager.PREVIEW_ENABLED && downloadId == previewDownloadId) {
                            if (manifest != null) {
                                previewDownloaded = true;
                                final long pubdate = manifest.getIssue().getPubdate();
                                Logger.d(TAG, String.format("Finished downloading preview section %s.", pubdate));
                                //Get synchronized archive as an observable on another thread in order to avoid UI hangups.
                                am.getSynchronizedPreviewArchiveForLabelObs(pubdate).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
                                    @Override
                                    public void onCompleted() {}
                                    @Override
                                    public void onError(Throwable e) {
                                        Logger.e(TAG, String.format(TAG, "Failed to synchronize preview for %s.", pubdate), e);
                                    }
                                    @Override
                                    public void onNext(Archive archive) {
                                        previewArchive = archive;
                                        updateAdapterContent();
                                        //Begin auto-download of sections once preview is finished (so preview gets all resources in interim.)
                                        getArchiveManager().downloadNeededSections(manifest);
                                    }
                                });
                            } else {
                                //This should catch the case where the user regains connectivity while looking at the screen, causing the download to resume.
                                loadForSelectedDate();
                                Logger.d(TAG, "Reloaded due to null manifest.");
                            }
                        } else {
                            am.getArchiveByDownloadIdObs(downloadId).take(1).flatMap(new Func1<Archive, Observable<Archive>>() {
                                @Override
                                public Observable<Archive> call(Archive archive) {
                                    if (archive == null) {
                                        Logger.w(TAG, String.format("No archive found with downloadId: %s,", downloadId));
                                        return Observable.empty();
                                    }
                                    long label = archive.getDate();
                                    String sectionLetter = archive.getSection();

                                    Logger.d(TAG, String.format("Finished downloading section %s-%s with downloadId: %s.", label, sectionLetter, downloadId));
                                    return getArchiveManager().getSynchronizedArchiveObs(label, sectionLetter);
                                }
                            }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
                                @Override
                                public void onCompleted() {
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Logger.e(TAG, String.format("Error synchronizing section with downloadId: %s", downloadId), e);
                                }

                                @Override
                                public void onNext(Archive archive) {
                                    if (archive != null) {
                                        updateAdapterPositionForSectionLetter(archive.getSection());
                                    } else {
                                        updateAdapterContent();
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "Exception in receiving download complete intent.", e);
                }
            }
        };
        articleListViewModel = FlagshipApplication.getInstance().getSavedArticleManager().getViewModel(this);
        addPrintToReadingHistory(getString(R.string.print_edition_path));
    }

    private void initReadingHistoryViewModel() {
        readingHistoryViewModel =
                new ViewModelProvider(this).get(ReadingHistoryViewModel.class);
    }

    private void addPrintToReadingHistory(String url) {
        ReadingHistoryModel readingHistoryModel =
                new ReadingHistoryModel(url, getCanonicalUrl(url), System.currentTimeMillis(), null, false);
        MetadataModel meta = new MetadataModel(
                readingHistoryModel.getContentUrl(),
                readingHistoryModel.getLmt()
        );
        readingHistoryViewModel.saveArticle(readingHistoryModel, meta);
    }


    @Override
    public void onDestroyView() {
        previewRecyclerView = null;
        _view = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(DateErrorsParam, dateErrors);
        outState.putLong(PreviewDownloadIdParam, previewDownloadId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _view = (ViewGroup) inflater.inflate(R.layout.fragment_archives, container, false);

        previewRecyclerView = (RecyclerView) _view.findViewById(R.id.pdf_archives_listview);
        previewRecyclerView.setHasFixedSize(true);

        dayLabel = (TextView) _view.findViewById(R.id.dayLabel);
        dateLabel = (TextView) _view.findViewById(R.id.dateLabel);
        recyclerProgressBar = (ProgressBar) _view.findViewById(R.id.recycler_progress_bar);

        Button editionsButton = (Button) _view.findViewById(R.id.editionsButton);
        editionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditionsActivity.class);
                startActivity(intent);
            }
        });

        noEditionFoundBar = _view.findViewById(R.id.no_edition_found_bar);
        setNoEditionFoundBarVisibility(false);

        downloadPrompt = _view.findViewById(R.id.download_prompt);
        pushPrompt = _view.findViewById(R.id.push_prompt);
        if (ArchiveManager.isPrintEditionFirstRun(getContext())) {
            if (downloadPrompt != null) {
                downloadPrompt.setVisibility(View.VISIBLE);
            }
            if (!ArchiveManager.isSmartLogicInitialized(getContext())) {
                //Give all sections an initial read date so that they don't turn off auto-download the next morning.
                getArchiveManager().setupSectionReadsForFirstRun();
                ArchiveManager.setSmartLogicInitialized(getContext(), true);
            }
        } else {
            int printEditionRunCount = ArchiveManager.getPrintEditionRunCount(getContext());
            if (printEditionRunCount < PUSH_PROMPT_THRESHOLD) {
                ArchiveManager.setPrintEditionRunCount(getContext(), ++printEditionRunCount);
            } else if (printEditionRunCount == PUSH_PROMPT_THRESHOLD && pushPrompt != null) {
                if (!getArchiveManager().isPrintEditionPushEnabled()) {
                    //Only show the prompt if the user didn't already go in and enable it on their own.
                    pushPrompt.setVisibility(View.VISIBLE);
                } else {
                    //If they've already enabled it, then set the run count above the threshold so they aren't asked again.
                    ArchiveManager.setPrintEditionRunCount(getContext(), PUSH_PROMPT_THRESHOLD + 1);
                }
            }
        }

        //Do these methods only need to happen when setting visibility to true?  Worth considering.
        setupDownloadPromptButtons();
        setupPushPromptButtons();

        downloadBarList = new ArrayList<>();
        observePageEngagement();
        return _view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mHandler == null) {
            mHandler = new MyHandler(this);
        }

        archivesDate = ArchiveManager.getPrintEditionDate(getContext());

        if (getActivity() != null) {
            ContextCompat.registerReceiver(getActivity(), mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }

        loadForSelectedDate();

        if (getActivity() != null && !getActivity().isFinishing()) {
            EventTimerLog.stopTimingEventAndLog(EventTimerLog.PRINT_EDITION_LOAD, EventTimerLog.PRINT_EDITION_LOAD, getActivity().getApplicationContext(), false);
        }
        ((ConnectivityActivity) getActivity()).checkConnectivity();
    }

    @Override
    public void onPause() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (getActivity() != null) {
            try {
                getActivity().unregisterReceiver(mReceiver);
            } catch (Exception e) {
                //ignore
            }
        }
        ArchiveManager.setLastViewedTime(getContext(), System.currentTimeMillis());
        //Needed to restore toolbars if rotation from landscape to portrait happens in another activity.
        if (getActivity() instanceof MainActivity && ! getActivity().isFinishing()
                && !UIUtil.isPortrait(getContext())) {
            ((MainActivity) getActivity()).showToolbars();
        }
        super.onPause();
    }

    private void observePageEngagement() {
        getLifecycle().addObserver(new PageEngagementLifecycleObserver(
                Measurement.getTrackingPageName(BottomTab.Print.getTrackingName()),
                BottomTab.Print.getTitle(),
                Measurement.CONTENT_TYPE_FRONT
        ));
    }

    private void setProgressVisibility(boolean visible) {
        if (recyclerProgressBar != null) {
            recyclerProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setNoEditionFoundBarVisibility(boolean visible) {
        if (noEditionFoundBar != null) {
            noEditionFoundBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void loadForSelectedDate() {
        previewDownloaded = false;
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
        setProgressVisibility(true);
        setNoEditionFoundBarVisibility(false);

        if (dateLabel != null) {
            dateLabel.setText(Utils.dateToDateString(archivesDate).toUpperCase());
        }
        if (dayLabel != null) {
            DayOfWeekFormat.setTimeZone(Utils.getDefaultAppTimeZone());
            dayLabel.setText(DayOfWeekFormat.format(archivesDate).toUpperCase());
        }

        //Clear the adapter to prevent mismatched date labels / section fronts.
        if (previewRecyclerView != null) {
            previewRecyclerView.setAdapter(null);
        }

        compositeSubscription = new CompositeSubscription();

        final long manifestTimerStart = System.currentTimeMillis();
        compositeSubscription.add(getArchiveManager().getPrintManifest(archivesDate).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<PrintManifestResponse>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        Logger.e(TAG, String.format("Error loading manifest for date=%s", Utils.dateToEDTLabel(archivesDate)), e);
                        if (e instanceof HttpException && HttpUtil.is400Exception(((HttpException) e).code()) && dateErrors <= MAX_DATE_ERRORS) {
                            archivesDate.setTime(archivesDate.getTime() - Utils.oneDayinMilliseconds);
                            Logger.d(TAG, String.format("Set archive date to %s", Utils.dateToEDTLabel(archivesDate)));
                            dateErrors++;
                            loadForSelectedDate();
                        } else {
                            Logger.e(TAG, String.format("Max number of errors reached. Unable to load date=%s. Displaying failure dialog.", Utils.dateToEDTLabel(archivesDate)));
                            RemoteLog.e(getContext(), new EventLog.Builder()
                                    .setMessage("Failed to load print edition manifest")
                                    .setModule(LogModules.PRINT)
                                    .set("date", Utils.dateToEDTLabel(archivesDate)).build());
                            setProgressVisibility(false);
                            setNoEditionFoundBarVisibility(true);
                        }
                    }

                    @Override
                    public void onNext(PrintManifestResponse printManifestResponse) {
                        manifest = printManifestResponse;

                        final long pubdate = manifest.getIssue().getPubdate();
                        Logger.d(TAG, String.format("Loading manifest for %s took %s ms.", pubdate, (System.currentTimeMillis() - manifestTimerStart)));

                        final ArchivesAdapter adapter = new ArchivesAdapter(new ArrayList<ArchiveItem>(), getContext(), ArchivesFragment.this);

                        //Check if the tutorial is enabled.  If so, we need to create an ArchiveItem for it.
                        if (ArchiveManager.isPrintTutorialEnabled()) {
                            Logger.d(TAG, "Tutorial enabled, adding to Archives list.");
                            ArchiveItem tutorialItem = new ArchiveItem();
                            tutorialItem.date = Utils.edtLabelToDate(pubdate);
                            tutorialItem.dateString = Utils.labelToDateString(pubdate);
                            tutorialItem.label = pubdate;
                            tutorialItem.sectionName = ArchiveManager.TUTORIAL_SECTION_NAME;
                            tutorialItem.sectionLetter = ArchiveManager.TUTORIAL_SECTION_NAME;
                            tutorialItem.pages = ArchiveManager.getTutorialPages();
                            tutorialItem.coverImagePath = ArchiveManager.getTutorialCoverImageFilename(getContext());
                            tutorialItem.height = ArchiveManager.DEFAULT_PAGE_HEIGHT;
                            tutorialItem.width = ArchiveManager.DEFAULT_PAGE_WIDTH;
                            tutorialItem.lmt = 0;

                            adapter.archiveItems.add(tutorialItem);
                        }

                        for (PrintSection section : manifest.getIssue().getSections()) {
                            ArchiveItem item = new ArchiveItem();
                            item.date = Utils.edtLabelToDate(pubdate);
                            item.dateString = Utils.labelToDateString(pubdate);
                            item.label = pubdate;
                            item.sectionName = section.getSectionName();
                            item.sectionLetter = section.getSectionLetter();
                            item.pages = section.getPages();
                            item.coverImagePath = ArchiveManager.getFullFilePath(getContext(), item.label, item.sectionLetter, section.getCoverImageName()).getPath();
                            item.height = section.getCoverImageHeight();
                            item.width = section.getCoverImageWidth();
                            item.lmt = section.getLmt() != null ? Long.parseLong(section.getLmt()) : 0;

                            //Need to set this info for use in PDFActivity. I wish there was a more efficient way to do it than n^2.
                            if (item.pages != null) {
                                for (PrintSectionPage page : item.pages) {
                                    page.setSectionLetter(item.sectionLetter);
                                    page.setSectionLmt(item.lmt);
                                }
                            }
                            adapter.archiveItems.add(item);
                        }

                        getArchiveManager().getSynchronizedPreviewArchiveForLabelObs(pubdate)
                                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
                            @Override
                            public void onCompleted() {}
                            @Override
                            public void onError(Throwable e) {
                                Logger.e(TAG, String.format("Error getting synchronized preview for %s.", pubdate), e);
                            }
                            @Override
                            public void onNext(Archive archive) {
                                previewDownloaded = archive != null && archive.getDownloadId() == null;
                                //Setup download of preview bundle.
                                if (previewDownloaded) {
                                    Logger.d(TAG, "Preview already downloaded.");
                                    previewArchive = archive;
                                    //If on startup, preview is already there, we can start downloading needed sections.
                                    getArchiveManager().downloadNeededSections(manifest);
                                } else {
                                    if (archive == null) {
                                        previewArchive = downloadPreviewForLabel(pubdate);
                                        Logger.d(TAG, "Preview not downloaded, starting now.");
                                    } else {
                                        previewArchive = archive;
                                        Logger.d(TAG, "Preview download already in progress.");
                                    }
                                    if (previewArchive != null) {
                                        previewDownloadId = previewArchive.getDownloadId();
                                    }
                                }
                                splashViewModel = new ViewModelProvider(requireActivity()).get(SplashViewModel.class);
                                splashViewModel.dismissSplashScreen();
                                setProgressVisibility(false);
                                setNoEditionFoundBarVisibility(false);
                                if (previewRecyclerView != null) {
                                    previewRecyclerView.setAdapter(adapter);
                                    archiveItems = adapter.archiveItems;
                                }
                                //Reset count of date errors if manifest was loaded successfully.
                                dateErrors = 0;
                                if (mHandler != null) mHandler.sendEmptyMessageDelayed(0, 1000);
                            }
                        });
                    }
                })
        );
    }

    private Archive downloadPreviewForLabel(long pubdate) {
        return getArchiveManager().downloadPreviewArchiveForLabel(pubdate);
    }

    @Override
    public void onSectionSelected(final ArchiveItem item) {
        //The tutorial section is treated differently.
        if (item.isTutorial()) {
            openTutorial(item);
            return;
        }

        final ArchiveManager am = getArchiveManager();
        //Add an attempt to read the section for auto-downloading smart logic.
        am.addSectionRead(item.sectionName);

        am.getSynchronizedArchiveObs(item.label, item.sectionLetter).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
            @Override
            public void onCompleted() {}

            @Override
            public void onError(Throwable e) {
                Logger.e(TAG, String.format("Error opening archive %s-%s in onSectionSelected", item.label, item.sectionLetter));
            }
            @Override
            public void onNext(Archive archive) {
                //Here, download the archive if its status is anything different from expected (not downloaded, etc.)
                if (archive == null || archive.getStatus() == Archive.Status.Canceled || archive.getStatus() == Archive.Status.Deleted) {
                    downloadArchive(item);
                    if (previewDownloaded && ArchiveManager.PREVIEW_ENABLED) {
                        showArchivePreview(item);
                    }
                } else if (archive.getDownloadId() != null) {
                    if (previewDownloaded && ArchiveManager.PREVIEW_ENABLED) {
                        item.downloadId = archive.getDownloadId();
                        showArchivePreview(item);
                    }
                } else {
                    //To get here, the archive must have already been downloaded.  Update the timestamp to show when it was most recently accessed.
                    archive.updateTimestamp();
                    am.updateArchive(archive);
                    showArchive(archive, item);
                }
            }
        });
    }

    private void showArchive(Archive archive, ArchiveItem item) {
        //TODO: Make configurable to a specific page.
        openArchive(archive, item, 0);
    }

    private void showArchivePreview(ArchiveItem item) {
        showArchive(previewArchive, item);
    }

    private void openTutorial(ArchiveItem item) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        Intent intent = new Intent(getActivity(), PdfActivity.class);
        int pagenum = 0;
        if (item != null) {
            EventTimerLog.startTimingEvent(EventTimerLog.ARCHIVE_OPEN_PDF, EventTimerLog.ARCHIVE_OPEN_PDF);
            PrintSectionPage pdfArchive = item.pages.get(pagenum);

            intent.putExtra(PdfActivity.ARCHIVE_LABEL, item.label);
            intent.putExtra(PdfActivity.ARCHIVE_SECTION_LETTER, item.sectionLetter);
            intent.putExtra(PdfActivity.PARAM_THUMBNAIL_PATH, ArchiveManager.getTutorialAssetFilePath(getContext(), pdfArchive.getThumbnailPath()));
            //TODO: Clean up uses of SectionNameParam
            intent.putExtra(TopBarFragment.SectionDisplayName, getActivity().getIntent().getStringExtra(TopBarFragment.SectionDisplayName));
            intent.putExtra(PdfActivity.PARAM_ARCHIVE_DATE_SECTION, Measurement.archiveDateSectionPageNum(item.label, item.sectionLetter, pdfArchive.getPageNumber()));

            File pdfFile = new File(ArchiveManager.getTutorialAssetFilePath(getContext(), pdfArchive.getHiResPdfPath()));
            if (!pdfFile.exists()) {
                Logger.w(TAG, "Tutorial does not exist.");
                return;
            }

            intent.putExtra(PdfActivity.PARAM_PDF, item.pages);
            intent.putExtra(PdfActivity.PARAM_PAGE_NUMBER, Integer.valueOf(pagenum));
            intent.setAction(Intent.ACTION_VIEW);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), R.string.open_archive_error, Toast.LENGTH_LONG).show();
        }
    }

    private final ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Measurement.setNavigationBehavior(NavigationBehavior.BACK_TO_FRONT);
                    Measurement.trackBottomTabNavigation(BottomTab.Print.getRoute());
                }
            }
    );

    private void openArchive(Archive archive, ArchiveItem item, int pagenum) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        /***
         * Android 5 > Redirecting to PDF2ACTIVITY
         * Android 4 to PDF Activity
         */
        Intent intent ;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(getActivity(), Pdf2Activity.class);
        } else {
            intent = new Intent(getActivity(), PdfActivity.class);
        }
        if (archive != null && item != null) {
            EventTimerLog.startTimingEvent(EventTimerLog.ARCHIVE_OPEN_PDF, EventTimerLog.ARCHIVE_OPEN_PDF);
            //Send an arraylist of all active download ids.
            intent.putExtra(PdfActivity.ARCHIVE_DOWNLOAD_ID, getActiveDownloadIds(item.label));
            PrintSectionPage pdfArchive = item.pages.get(pagenum);

            //Needs to be archive here so it looks in the right place for the preview files.
            File pdfFolder = ArchiveManager.getArchiveFolder(getContext(), archive.getDate(), archive.getSection());

            String thumbPath = pdfArchive.getHiResImagePath() == null ? pdfArchive.getThumbnailPath() : pdfArchive.getHiResImagePath();
            final File thumb = new File(pdfFolder, thumbPath);
            intent.putExtra(PdfActivity.ARCHIVE_LABEL, item.label);
            intent.putExtra(PdfActivity.ARCHIVE_SECTION_LETTER, item.sectionLetter);
            intent.putExtra(PdfActivity.PARAM_THUMBNAIL_PATH, thumb.getPath());
            //TODO: Clean up uses of SectionNameParam
            intent.putExtra(TopBarFragment.SectionDisplayName, getActivity().getIntent().getStringExtra(TopBarFragment.SectionDisplayName));
            intent.putExtra(PdfActivity.PARAM_ARCHIVE_DATE_SECTION, Measurement.archiveDateSectionPageNum(archive.getDate(), archive.getSection(), pdfArchive.getPageNumber()));

            File pdfFile = new File(pdfFolder, pdfArchive.getHiResPdfPath());
            if (!pdfFile.exists()) {
                Logger.w(TAG, "PDF File does not exist.");
                RemoteLog.d(getContext(), new EventLog.Builder()
                        .setMessage("Opening PDF archive. PDF File does not exist.")
                        .setModule(LogModules.PRINT)
                        .set("archive_path", archive.getPath())
                        .set("section", archive.getSection())
                        .set("res_pdf_path", pdfArchive.getHiResPdfPath()).build());
                return;
            }


            intent.putExtra(PdfActivity.PARAM_PDF, getArchivePages());
            intent.putExtra(PdfActivity.PARAM_PAGE_NUMBER, getPageNumberForSection(item.sectionLetter));
            intent.setAction(Intent.ACTION_VIEW);
            resultLauncher.launch(intent);
        } else {
            Toast.makeText(getActivity(), R.string.open_archive_error, Toast.LENGTH_LONG).show();
            RemoteLog.d(FlagshipApplication.getInstance(), new EventLog.Builder()
                    .setMessage("Opening PDF archive Error")
                    .setModule(LogModules.PRINT)
                    .set("archive_path", archive.getPath())
                    .set("section", archive.getSection())
                    .set("page_num", pagenum).build());
        }
    }

    private ArrayList<PrintSectionPage> getArchivePages() {
        ArrayList<PrintSectionPage> archivePages = new ArrayList<>();
        for (int i = 0; i < archiveItems.size(); i++) {
            ArchiveItem archiveItem = archiveItems.get(i);
            if (archiveItem != null && !archiveItem.isTutorial()) {
                archivePages.addAll(archiveItem.pages);
            }
        }
        return archivePages;
    }

    private int getPageNumberForSection(String sectionLetter) {
        int pageNumber = 0;
        for (int i = 0; i < archiveItems.size(); i++) {
            ArchiveItem archiveItem = archiveItems.get(i);
            if (archiveItem != null && !archiveItem.isTutorial()) {
                if (archiveItem.sectionLetter.equals(sectionLetter)) {
                    return pageNumber;
                } else {
                    pageNumber += archiveItem.pages.size();
                }
            }
        }
        return pageNumber;
    }

    private ArrayList<Long> getActiveDownloadIds(long label) {
        ArrayList<Long> activeDownloadIds = new ArrayList<>();
        List<Archive> archives = getArchiveManager().getArchivesByLabel(label);
        for (Archive archive : archives) {
            if (archive.getDownloadId() != null) {
                activeDownloadIds.add(archive.getDownloadId());
            }
        }
        return activeDownloadIds;
    }

    public void updateAdapterContent() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (previewRecyclerView != null && previewRecyclerView.getAdapter() instanceof ArchivesAdapter) {
                    previewRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    private void updateAdapterPositionForSectionLetter(final String sectionLetter) {
        if (previewRecyclerView != null && previewRecyclerView.getAdapter() instanceof ArchivesAdapter) {
            int position = ((ArchivesAdapter) previewRecyclerView.getAdapter()).findPositionForSectionLetter(sectionLetter);
            Logger.d(TAG, String.format("Updating section %s at position %s", sectionLetter, position));
            updateAdapterItemAtPosition(position);
        }
    }

    private void updateAdapterItemAtPosition(final int position) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (previewRecyclerView != null && previewRecyclerView.getAdapter() instanceof ArchivesAdapter) {
                    ArchivesAdapter adapter = (ArchivesAdapter) previewRecyclerView.getAdapter();
                    if (position >= 0 && position < adapter.archiveItems.size()) {
                        adapter.notifyItemChanged(position);
                    } else {
                        Logger.w(TAG, String.format("Update adapter for position %s failed. Refreshing entire data set.", position));
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    private void enableRecyclerViewScrolling(boolean isEnabled) {
        if (previewRecyclerView != null) {
            if (isEnabled) {
                previewRecyclerView.setHorizontalScrollBarEnabled(true);
                previewRecyclerView.setClickable(true);
                setProgressVisibility(false);
            } else {
                previewRecyclerView.setHorizontalScrollBarEnabled(false);
                previewRecyclerView.setClickable(false);
                setProgressVisibility(true);
            }
        }
    }

    @Override
    public void downloadArchive(ArchiveItem item) {
        if (item == null || item.downloadId != null || item.isTutorial()) {
            return;
        }
        try {
            Archive archive = getArchiveManager().scheduleFileDownload(item.label, item.sectionLetter, item.lmt);
            if (archive != null) {
                item.downloadId = archive.getDownloadId();
            }
        } catch (IOException e) {
            RemoteLog.e(FlagshipApplication.getInstance(), new EventLog.Builder()
                    .setMessage("Print Archive Error. Error downloading PDF section")
                    .setModule(LogModules.PRINT)
                    .setErrorMessage(e.getMessage())
                    .set("section_letter", item.sectionLetter)
                    .set("label", item.label).build());
            Logger.e(TAG, String.format("Error downloading %s section for %s", item.sectionLetter, item.label), e);
        }
    }

    @Override
    public void deleteArchive(final ArchiveItem item, final int adapterPosition) {
        if (item == null) {
            return;
        }
        if(item.isTutorial()) {
            ArchiveManager.setPrintTutorialEnabled(false);
            if (previewRecyclerView != null && previewRecyclerView.getAdapter() instanceof ArchivesAdapter) {
                ArchivesAdapter adapter = (ArchivesAdapter)previewRecyclerView.getAdapter();
                adapter.removeItemAtPosition(0);
                archiveItems = adapter.archiveItems;

                updateAdapterContent();
            }
            return;
        }
        final ArchiveManager am = getArchiveManager();
        final Archive archive = am.getArchiveByLabelAndSection(item.label, item.sectionLetter);
        if (archive == null) {
            return;
        }

        am.deleteArchiveAsync(archive, false).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Boolean>() {
            @Override
            public void onCompleted() {}

            @Override
            public void onError(Throwable e) {
                Logger.e(TAG, "Error deleting archive.", e);
            }

            @Override
            public void onNext(Boolean aBoolean) {
                if (aBoolean) {
                    item.downloadId = null;
                    updateAdapterItemAtPosition(adapterPosition);
                }
            }
        });
    }

    @Override
    public ArchiveManager getArchiveManager() {
        if (archivesManager == null) {
            archivesManager = FlagshipApplication.getInstance().getArchiveManager();
        }
        return archivesManager;
    }

    public MyHandler getmHandler() {
        return mHandler;
    }

    public final RecyclerView getPreviewRecyclerView() {
        return previewRecyclerView;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ArchivesFragment> mActivity;

        public MyHandler(ArchivesFragment activity) {
            mActivity = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(Message msg) {

            if (mActivity == null || mActivity.get() == null) return;

            for (ProgressBar downloadBar : mActivity.get().getDownloadBarList()) {
                if (downloadBar.getTag() instanceof Long) {
                    Download download = Download.get(mActivity.get().getContext(), (Long) downloadBar.getTag());
                    if (download != null) {
                        int status = download.getStatus();
                        boolean isDownloading = status == DownloadManager.STATUS_PAUSED || status == DownloadManager.STATUS_PENDING ||
                                status == DownloadManager.STATUS_RUNNING;
                        if (isDownloading) {
                            long downloaded = download.getDownloadedBytes();
                            long total = download.getTotalBytes();
                            float percentage = downloaded / (float) total * 100;
                            downloadBar.setProgress((int) percentage);
                            downloadBar.setVisibility(View.VISIBLE);
                        } else {
                            downloadBar.setVisibility(View.INVISIBLE);
                        }
                    }
                } else {
                    downloadBar.setVisibility(View.INVISIBLE);
                }
            }

            if (mActivity.get().getmHandler() != null)
                mActivity.get().getmHandler().sendEmptyMessageDelayed(0, 1000);
        }
    }

    static class ArchiveItem {
        Long downloadId;
        public long label;
        public Date date;
        String dateString;
        String sectionLetter;
        public String sectionName;
        public ArrayList<PrintSectionPage> pages;
        String coverImagePath;
        public int height;
        public int width;
        public long lmt;

        boolean isTutorial() {
            return this.sectionLetter.equals(ArchiveManager.TUTORIAL_SECTION_NAME);
        }
    }

    private static class ArchivesAdapter extends RecyclerView.Adapter<ArchiveItemVH>{
        private List<ArchiveItem> archiveItems;
        private Context context;
        private PrintSectionClickListener listener;

        ArchivesAdapter(List<ArchiveItem> archiveItems, Context context, PrintSectionClickListener listener) {
            this.archiveItems = archiveItems;
            this.context = context;
            this.listener = listener;
        }

        void removeItemAtPosition(int position) {
            if (archiveItems != null) {
                archiveItems.remove(position);
            }
        }

        int findPositionForSectionLetter(String sectionLetter) {
            for (int i = 0; i < archiveItems.size(); i++) {
                if (archiveItems.get(i).sectionLetter != null && archiveItems.get(i).sectionLetter.equals(sectionLetter)) {
                    return i;
                }
            }
            Logger.w(TAG, String.format("Unable to find position of sectionLetter %s", sectionLetter));
            return -1;
        }


        @Override
        public ArchiveItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View archiveView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_pdf_archive_item, parent, false);
            final ArchiveItemVH archiveItemVH = new ArchiveItemVH(archiveView);
            archiveItemVH.imageFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPosition = archiveItemVH.getAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION) {
                        return;
                    }
                    archiveItemVH.imageFrame.setClickable(false);
                    listener.onSectionSelected(archiveItems.get(adapterPosition));
                    notifyItemChanged(adapterPosition);
                    archiveItemVH.imageFrame.setClickable(true);
                }
            });
            archiveItemVH.imageFrame.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    archiveItemVH.showLongPressMenu(context);
                    return true;
                }
            });
            archiveItemVH.openButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPosition = archiveItemVH.getAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION) {
                        return;
                    }
                    archiveItemVH.imageFrame.setClickable(false);
                    archiveItemVH.hideLongPressMenu();
                    listener.onSectionSelected(archiveItems.get(adapterPosition));
                    archiveItemVH.imageFrame.setClickable(true);
                }
            });
            archiveItemVH.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPosition = archiveItemVH.getAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION) {
                        return;
                    }
                    if (archiveItemVH.isDownloaded) {
                        listener.deleteArchive(archiveItems.get(adapterPosition), adapterPosition);
                    } else {
                        listener.downloadArchive(archiveItems.get(adapterPosition));
                        notifyItemChanged(adapterPosition);
                    }
                    archiveItemVH.hideLongPressMenu();
                }
            });
            archiveItemVH.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    archiveItemVH.hideLongPressMenu();
                }
            });
            return archiveItemVH;
        }

        @Override
        public void onBindViewHolder(final ArchiveItemVH archiveItemVH, int position) {
            archiveItemVH.image.setImageDrawable(null);
            archiveItemVH.downloadStatusImage.setImageDrawable(null);
            archiveItemVH.hideLongPressMenu();

            archiveItemVH.downloadBar.setTag(null);
            archiveItemVH.downloadBar.setProgress(0);
            archiveItemVH.downloadBar.setVisibility(View.INVISIBLE);

            final ArchiveItem item = archiveItems.get(position);

            float ratio = ((float) item.width / item.height);
            archiveItemVH.imageFrame.setAspectRatio(ratio);

            archiveItemVH.sectionLabel.setText(item.sectionName);
            if (!item.isTutorial()) {
                if (!listener.getDownloadBarList().contains(archiveItemVH.downloadBar)) {
                    listener.getDownloadBarList().add(archiveItemVH.downloadBar);
                }
                archiveItemVH.subscription = listener.getArchiveManager().getSynchronizedArchiveObs(item.label, item.sectionLetter).take(1).flatMap(new Func1<Archive, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(Archive archive) {
                        if (archive != null) {
                            item.downloadId = archive.getDownloadId();
                        }
                        return listener.getDownloadStatusObs(archive);
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {}
                    @Override
                    public void onError(Throwable e) {
                        Logger.e(TAG, String.format("Error getting synchronized archive %s-%s in onBind", item.label, item.sectionLetter), e);
                    }
                    @Override
                    public void onNext(DownloadStatus downloadStatus) {
                        //Override for download status variables included if the archive is the tutorial.
                        if (item.isTutorial()) {
                            downloadStatus = DownloadStatus.DOWNLOADED;
                        }

                        if (downloadStatus == DownloadStatus.DOWNLOADING) {
                            archiveItemVH.downloadBar.setTag(item.downloadId);
                        }

                        if (!listener.getPreviewDownloaded() && !item.isTutorial()) {
                            Archive tempPreviewArchive = listener.getPreviewArchive();
                            archiveItemVH.downloadBar.setTag(tempPreviewArchive == null || tempPreviewArchive.getDownloadId() == null ? listener.getPreviewDownloadId() : tempPreviewArchive.getDownloadId());
                            archiveItemVH.downloadStatusImage.setBackgroundResource(R.drawable.blue_cloud);
                            archiveItemVH.downloadBar.setVisibility(View.VISIBLE);
                            archiveItemVH.isDownloaded = false;
                        } else if (downloadStatus == DownloadStatus.DOWNLOADING) {
                            archiveItemVH.downloadBar.setVisibility(View.VISIBLE);
                            archiveItemVH.downloadStatusImage.setBackgroundResource(R.drawable.blue_cloud);
                            archiveItemVH.isDownloaded = false;
                        } else if (downloadStatus == DownloadStatus.DOWNLOADED) {
                            archiveItemVH.downloadBar.setVisibility(View.INVISIBLE);
                            archiveItemVH.downloadStatusImage.setBackgroundResource(R.drawable.green_checkmark);
                            archiveItemVH.isDownloaded = true;
                        } else {
                            archiveItemVH.downloadBar.setVisibility(View.INVISIBLE);
                            archiveItemVH.downloadStatusImage.setBackgroundResource(R.drawable.blue_cloud);
                            archiveItemVH.isDownloaded = false;
                        }
                    }
                });
            } else {
                archiveItemVH.downloadBar.setVisibility(View.INVISIBLE);
                archiveItemVH.downloadStatusImage.setBackgroundResource(R.drawable.green_checkmark);
                archiveItemVH.isDownloaded = true;
            }


            Picasso.get().load(new File(item.coverImagePath)).placeholder(R.drawable.archives_placeholder).error(R.drawable.archives_placeholder).fit().into(archiveItemVH.image);
        }

        @Override
        public void onViewRecycled(final ArchiveItemVH archiveItemVH) {
            if (archiveItemVH.subscription != null) {
                archiveItemVH.subscription.unsubscribe();
            }
            super.onViewRecycled(archiveItemVH);
        }

        @Override
        public boolean onFailedToRecycleView(final ArchiveItemVH archiveItemVH) {
            if (archiveItemVH.subscription != null) {
                archiveItemVH.subscription.unsubscribe();
            }
            return super.onFailedToRecycleView(archiveItemVH);
        }

        @Override
        public int getItemCount() {
            return archiveItems.size();
        }
    }

    @Override
    public Observable<DownloadStatus> getDownloadStatusObs(final Archive archive){
        return Observable.fromCallable(new Callable<DownloadStatus>() {
            @Override
            public DownloadStatus call() throws Exception {
                return getDownloadStatus(archive);
            }
        });
    }

    private DownloadStatus getDownloadStatus(Archive archive) {
        DownloadStatus downloadStatus = DownloadStatus.UNDOWNLOADED;
        StringBuffer logMessage = new StringBuffer("Downloading PDF Archive Stats ");
        if (archive != null) {
            Long downloadId = archive.getDownloadId();
            if (downloadId == null) {
                if (archive.getPath() != null && Utils.exists(archive.getPath())) {
                    downloadStatus = DownloadStatus.DOWNLOADED;
                }
            } else {
                long start = System.currentTimeMillis();
                Download download = Download.get(getContext(), downloadId);
                Logger.d(TAG, String.format("Download get time: %s", System.currentTimeMillis() - start));
                if (download != null) {
                    int status = download.getStatus();
                    if (status == DownloadManager.STATUS_PAUSED || status == DownloadManager.STATUS_PENDING ||
                            status == DownloadManager.STATUS_RUNNING) {
                        downloadStatus = DownloadStatus.DOWNLOADING;
                    }
                    if(status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloadStatus = DownloadStatus.DOWNLOADED;
                    }
                } else if (archive.getPath() != null && Utils.exists(archive.getPath())) {
                        //This shouldn't be reachable.
                        downloadStatus = DownloadStatus.DOWNLOADED;
                }
                RemoteLog.d(FlagshipApplication.getInstance(), new EventLog.Builder()
                        .setMessage("Print Download Status")
                        .setModule(LogModules.PRINT)
                        .set("download_get_time", (System.currentTimeMillis() - start))
                        .set("path", archive.getPath())
                        .set("status", downloadStatus).build());
            }
        }
        return downloadStatus;
    }


    public enum DownloadStatus { UNDOWNLOADED, DOWNLOADING, DOWNLOADED; }
    private static class ArchiveItemVH extends RecyclerView.ViewHolder{
        public ImageView image;
        boolean isDownloaded = false;
        ImageView downloadStatusImage;
        TextView sectionLabel;
        ProgressBar downloadBar;
        ProportionalLayout imageFrame;
        Button openButton;
        Button downloadButton;
        Button cancelButton;
        Subscription subscription;

        ArchiveItemVH(View itemView) {
            super(itemView);
            imageFrame = (ProportionalLayout) itemView.findViewById(R.id.image_frame);
            image = (ImageView) itemView.findViewById(R.id.image);
            downloadStatusImage = (ImageView) itemView.findViewById(R.id.download_status);

            sectionLabel = (TextView) itemView.findViewById(R.id.sectionLabel);
            downloadBar = (ProgressBar) itemView.findViewById(R.id.download_progress);
            openButton = (Button) itemView.findViewById(R.id.openButton);
            downloadButton = (Button) itemView.findViewById(R.id.downloadButton);
            cancelButton = (Button) itemView.findViewById(R.id.cancelButton);
        }

        void showLongPressMenu(Context context) {
            if (isDownloaded) {
                downloadButton.setBackgroundColor(ContextCompat.getColor(context, R.color.print_edition_remove_button));
                downloadButton.setText(R.string.archive_remove_button);
            } else {
                downloadButton.setBackgroundColor(ContextCompat.getColor(context, R.color.print_edition_download_button));
                downloadButton.setText(R.string.archive_download_button);
            }

            downloadButton.setVisibility(View.VISIBLE);
            openButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);

            imageFrame.setEnabled(false);
            image.setImageAlpha(80);
        }

        void hideLongPressMenu() {
            openButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.INVISIBLE);
            downloadButton.setVisibility(View.INVISIBLE);

            imageFrame.setEnabled(true);
            image.setImageAlpha(255);
        }
    }

    @Override
    public List<ProgressBar> getDownloadBarList() {
        return downloadBarList;
    }

    @Override
    public Archive getPreviewArchive() {
        return previewArchive;
    }

    @Override
    public long getPreviewDownloadId() {
        return previewDownloadId;
    }

    @Override
    public boolean getPreviewDownloaded() {
        return previewDownloaded;
    }

    private void setupDownloadPromptButtons() {
        Button promptConfirmButton = (Button) _view.findViewById(R.id.archive_prompt_confirm);
        if (promptConfirmButton != null) {
            promptConfirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideDownloadPrompt();
                    getArchiveManager().setDailyDownloadOn(true);
                }
            });
        }

        Button promptCancelButton = (Button) _view.findViewById(R.id.archive_prompt_cancel);
        if (promptCancelButton != null) {
            promptCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideDownloadPrompt();
                    getArchiveManager().setDailyDownloadOn(false);
                }
            });
        }
    }

    private void setupPushPromptButtons() {
        Button pushPromptConfirmButton = (Button) _view.findViewById(R.id.push_prompt_confirm);
        if (pushPromptConfirmButton != null) {
            pushPromptConfirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hidePushPrompt();
                    getArchiveManager().enablePrintEditionPush();
                }
            });
        }

        Button pushPromptCancelButton = (Button) _view.findViewById(R.id.push_prompt_cancel);
        if (pushPromptCancelButton != null) {
            pushPromptCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hidePushPrompt();
                }
            });
        }
    }

    private void hideDownloadPrompt() {
        if (downloadPrompt != null) {
            ArchiveManager.setPrintEditionFirstRun(getContext(), false);
            //Start counting print edition runs once they've acknowledged the auto-download prompt.
            ArchiveManager.setPrintEditionRunCount(getContext(), 1);
            downloadPrompt.setVisibility(View.GONE);
            updateAdapterContent();
        }
    }

    private void hidePushPrompt() {
        if (pushPrompt != null) {
            ArchiveManager.setPrintEditionRunCount(getContext(), PUSH_PROMPT_THRESHOLD + 1);
            pushPrompt.setVisibility(View.GONE);
            updateAdapterContent();
        }
    }
}
