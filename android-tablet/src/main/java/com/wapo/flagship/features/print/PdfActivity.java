package com.wapo.flagship.features.print;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.radaee.pdf.Global;
import com.radaee.view.WapoPDFViewPager;
import com.squareup.picasso.Picasso;
import com.wapo.android.commons.engagement.EngagementTracker;
import com.wapo.android.commons.engagement.PageEngagementTrace;
import com.wapo.android.commons.util.Download;
import com.wapo.android.remotelog.logger.EventTimerLog;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.data.Archive;
import com.wapo.flagship.data.ArchiveManager;
import com.wapo.flagship.data.CacheManager;
import com.wapo.flagship.data.FileMeta;
import com.wapo.flagship.features.articles2.activities.ArticlesParcel;
import com.wapo.flagship.features.articles2.activities.ArticlesParcelKt;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.features.shared.fragments.TopBarFragment;
import com.wapo.flagship.model.PrintSectionPage;
import com.wapo.android.commons.util.Logger;
import com.wapo.flagship.util.ReachabilityUtil;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.WPUrlAnalyser;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.flagship.wrappers.CrashWrapper;
import com.wapo.view.ProportionalLayout;
import com.washingtonpost.android.R;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

@AndroidEntryPoint
public class PdfActivity extends BaseActivity implements WapoPDFViewPager.WapoReaderListener, PrintActivityInterface, ThumbnailClickListener {
    private static final String TAG = PdfActivity.class.getSimpleName();

    public static final String PARAM_THUMBNAIL_PATH = "thumb";
    public static final String PARAM_PAGE_NUMBER = "pagenum";
    public static final String PARAM_ARCHIVE_DATE_SECTION = "date_section_page_num";
    public static final String PARAM_PDF = "pdf";
    public static final String ARCHIVE_DOWNLOAD_ID = "downloadId";
    public static final String ARCHIVE_LABEL = "archiveLabel";
    public static final String ARCHIVE_SECTION_LETTER = "archiveSectionLetter";
    private static final String MENU_ITEM_FORMAT = "Page %s";
    private static final String PARAM_ARCHIVE_ID = "archiveDownloadId";
    private static final String PARAM_THUMBNAILS_VISIBLE = "thumbnailsVisible";
    private static final String CONTINUE_STRING = "continue:";

    private PdfHandler mHandler;
    private ArrayList<PrintSectionPage> printSectionPages;
    private String[] pdfLocations;
    private WapoPDFViewPager reader;
    private ImageView thumbView;
    private ProgressBar indicator;
    private ProgressBar downloadBar;
    private TextView pageNumber;
    private int pageNo;
    private BroadcastReceiver mReceiver;
    private ArrayList<Long> incomingDownloadIdList;
    private long label;
    private String sectionLetter;
    private RecyclerView pdfThumbnailRecyclerView;
    private View.OnClickListener thumbnailListener;
    private AnimatorListenerAdapter animatorListenerAdapter;
    private String defaultPdfPath;
    private final EngagementTracker engagementTracker = EngagementTracker.Companion.getInstance();
    private PageEngagementTrace engagementTrace;
    private String currentPageName;

    @Inject
    CacheManager cacheManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radaee_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        ActionBar actionBar =  getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        defaultPdfPath = ArchiveManager.getPlaceholderPDFFilePath(getApplicationContext());

        Global.Init(this);
        Global.render_mode = 0;
        reader = (WapoPDFViewPager) findViewById(R.id.pdf_pager);
        reader.setBackgroundResource(android.R.color.transparent);
        reader.setVisibility(View.INVISIBLE);
        thumbView = (ImageView) findViewById(R.id.thumbView);
        indicator = (ProgressBar) findViewById(R.id.pdf_indicator);
        downloadBar = (ProgressBar) findViewById(R.id.download_progress);
        pageNumber = (TextView) findViewById(R.id.page_number);
        Intent intent = getIntent();
        label = intent.getLongExtra(ARCHIVE_LABEL, -1);
        sectionLetter = intent.getStringExtra(ARCHIVE_SECTION_LETTER);
        printSectionPages = (ArrayList<PrintSectionPage>) intent.getSerializableExtra(PARAM_PDF);
        if (printSectionPages == null) {
            CrashWrapper.sendException(new Exception("Failed to open PdfActivity. printSectionPages is null."));
            finish();
            return;
        }
        File pdfFolder = ArchiveManager.getArchiveFolder(this, label, sectionLetter);
        pdfLocations = new String[printSectionPages.size()];
        for (int i = 0; i < printSectionPages.size(); i++) {
            pdfLocations[i] = (isTutorial()) ? ArchiveManager.getTutorialAssetFilePath(this, printSectionPages.get(i).getHiResPdfPath()) :
                    pdfFolder.getPath() + File.separator + printSectionPages.get(i).getHiResPdfPath();
        }
        ThumbnailAdapter adapter = new ThumbnailAdapter(printSectionPages, label, PdfActivity.this, PdfActivity.this);

        incomingDownloadIdList = (ArrayList<Long>) intent.getSerializableExtra(ARCHIVE_DOWNLOAD_ID);

        final String omnitureDateSectPageParam;
        boolean thumbnailVisibility = false;
        if (savedInstanceState != null) {
            pageNo = savedInstanceState.getInt(PARAM_PAGE_NUMBER, 0);
            incomingDownloadIdList = (ArrayList<Long>) savedInstanceState.getSerializable(PARAM_ARCHIVE_ID);
            omnitureDateSectPageParam = savedInstanceState.getString(PARAM_ARCHIVE_DATE_SECTION);
            thumbnailVisibility = savedInstanceState.getBoolean(PARAM_THUMBNAILS_VISIBLE, false);
        } else {
            pageNo = intent.getIntExtra(PARAM_PAGE_NUMBER, 0);
            omnitureDateSectPageParam = intent.getStringExtra(PARAM_ARCHIVE_DATE_SECTION);
            //This thumbnail stuff probably shouldn't be inside this else.
            String thumbPath = intent.getStringExtra(PARAM_THUMBNAIL_PATH);
            File thumb = TextUtils.isEmpty(thumbPath) ? null : new File(thumbPath);
            if (thumb != null && thumbView != null) {
                //Thumbnail behavior should differ between portrait and landscape?
                //if (UIUtil.isPortrait(this)) {
                //    thumbView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                //    Picasso.get().load(thumb).noPlaceholder().error(R.drawable.archives_placeholder).fit().centerInside().into(thumbView);
                //} else {
                thumbView.setScaleType(ImageView.ScaleType.MATRIX);
                Picasso.get().load(thumb).noPlaceholder().error(R.drawable.archives_placeholder).noFade().resize((UIUtil.displayMetrics(this).widthPixels), 0).into(thumbView);
                //}
            }
        }

        if (!isTutorial()) {
            currentPageName = (Measurement.PAGE_PDFFULL +
                    Measurement.archiveDateSectionWithPageNumReplaced(
                            getIntent().getStringExtra(PARAM_ARCHIVE_DATE_SECTION), pageNo
                    )).toLowerCase();
        }

        pdfThumbnailRecyclerView = (RecyclerView) findViewById(R.id.pdf_thumbnails);
        if (pdfThumbnailRecyclerView != null) {
            pdfThumbnailRecyclerView.setAdapter(adapter);
            pdfThumbnailRecyclerView.setVisibility(thumbnailVisibility ? View.VISIBLE : View.GONE);
        }

        if (incomingDownloadIdList == null) {
            incomingDownloadIdList = new ArrayList<>();
        }

        animatorListenerAdapter = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (thumbView != null) {
                    thumbView.setVisibility(View.GONE);
                }
                if (indicator != null) {
                    indicator.setVisibility(View.GONE);
                }
            }
        };

        pageNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pdfThumbnailRecyclerView != null) {
                    if (pdfThumbnailRecyclerView.getVisibility() != View.VISIBLE) {
                        pdfThumbnailRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        pdfThumbnailRecyclerView.setVisibility(View.GONE);
                    }
                    updateMenuItemTextColor();
                }
            }
        });
        updateMenuItemText();
        updateMenuItemTextColor();

        if(downloadPending()) {
            downloadBar.setVisibility(View.VISIBLE);
        } else {
            downloadBar.setVisibility(View.INVISIBLE);
        }

        loadDocument();

        if (!isTutorial()) {
            Measurement.trackPrint(null, Measurement.PAGE_PDFFULL, omnitureDateSectPageParam, false);
        } else {
            Measurement.trackPrintTutorial();
        }

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Logger.d(TAG, action);

                try {
                    final ArchiveManager am = FlagshipApplication.getInstance().getArchiveManager();
                    final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    final String[] sectionLetter = new String[1];
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        am.getArchiveByDownloadIdObs(downloadId).take(1).flatMap(new Func1<Archive, Observable<Archive>>() {
                            @Override
                            public Observable<Archive> call(Archive archive) {
                                if (archive != null) {
                                    sectionLetter[0] = archive.getSection();
                                    return am.getSynchronizedArchiveObs(archive.getDate(), archive.getSection());
                                } else {
                                    Logger.w(TAG, String.format("No archive found with downloadId: %s,", downloadId));
                                    return Observable.empty();
                                }
                            }
                        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Logger.e(TAG, String.format("Error processing download in PDFActivity for %s-%s with downloadId: %s", label, sectionLetter[0], downloadId), e);
                            }

                            @Override
                            public void onNext(Archive archive) {
                                if (archive != null && incomingDownloadIdList.contains(downloadId)) {
                                    Logger.d(TAG, String.format("Finishing download of %s-%s with downloadId: %s.", label, sectionLetter[0], downloadId));
                                    finishDownload(downloadId);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Logger.e(TAG, Utils.exceptionToString(e));
                }
            }
        };
    }

    private boolean isTutorial() {
        return sectionLetter != null && sectionLetter.equals(ArchiveManager.TUTORIAL_SECTION_NAME);
    }

    private void updateMenuItemText() {
        if (pageNo >= 0 && printSectionPages != null && pageNo < printSectionPages.size() && pageNumber != null) {
            pageNumber.setText(String.format(Locale.US, MENU_ITEM_FORMAT, printSectionPages.get(pageNo).getPageName()));
        }
    }

    private void updateMenuItemTextColor() {
        if (pdfThumbnailRecyclerView == null || pageNumber == null) {
            return;
        }
        boolean thumbnailsHidden = pdfThumbnailRecyclerView.getVisibility() != View.VISIBLE;
        pageNumber.setTextColor(ContextCompat.getColor(this, (thumbnailsHidden ? R.color.print_edition_pdf_thumbnail_text : R.color.print_edition_calendar_selected_background)));
        pageNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, thumbnailsHidden ? R.drawable.icon_pagepicker : R.drawable.icon_pagepicker_blue, 0);
    }

    private void loadDocument() {
        Logger.d(TAG, "loadDocument");
        onDocumentOpen();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_OK);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mHandler == null) {
            mHandler = new PdfHandler(this);
        }
        mHandler.sendEmptyMessageDelayed(0, 500);
        startEngagementTrace();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * When a user opens ArticlesActivity right before getting paywalled on this screen,
         * the user will continue to see the paywall on this screen even if they were to successfully login from
         * the paywall in ArticlesActivity. So, will hide the paywall in the onStart.
         */

        if(PaywallService.getInstance().isPremiumUser() && isWallShowing()) {
            dismissWall();
        }
        else if(!this.downloadPending() && !isTutorial() && !isWallShowing()) {
            paywallTrackPageView(pageNo);
        }

        Measurement.resumeCollection(this);

        if (indicator != null) {
            indicator.setVisibility(View.GONE);
        }

        if (downloadPending()) {
            Long[] incomingDownloadArray = new Long[incomingDownloadIdList.size()];
            incomingDownloadIdList.toArray(incomingDownloadArray);
            for (final Long incomingDownloadId : incomingDownloadArray) {
                Download download = Download.get(this, incomingDownloadId);
                if (download == null || download.getStatus() == DownloadManager.STATUS_SUCCESSFUL) {
                    final ArchiveManager am = FlagshipApplication.getInstance().getArchiveManager();

                    am.getArchiveByDownloadIdObs(incomingDownloadId).take(1).flatMap(new Func1<Archive, Observable<Archive>>() {
                        @Override
                        public Observable<Archive> call(Archive archive) {
                            if (archive != null) {
                                return am.getSynchronizedArchiveObs(archive.getDate(), archive.getSection());
                            } else {
                                Logger.w(TAG, String.format("No archive found with downloadId: %s,", incomingDownloadId));
                                return Observable.empty();
                            }
                        }
                    }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            Logger.e(TAG, "Error performing auto-reload from onResume.", e);
                        }

                        @Override
                        public void onNext(Archive archive) {
                            if (archive != null) {
                                finishDownload(incomingDownloadId);
                            }
                        }
                    });
                }
            }
        }

        ContextCompat.registerReceiver(this, mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        EventTimerLog.stopTimingEventAndLog(EventTimerLog.ARCHIVE_OPEN_PDF, EventTimerLog.ARCHIVE_OPEN_PDF, getApplicationContext(), false);

        invalidateOptionsMenu();
        CrashWrapper.logExtras("PdfActivity onResume ");
    }

    private void finishDownload(long incomingDownloadId) {
        if (incomingDownloadIdList != null) {
            //The archive you're currently viewing just finished downloading.
            incomingDownloadIdList.remove(incomingDownloadId);
            if (incomingDownloadIdList.isEmpty()) {
                downloadBar.setVisibility(View.INVISIBLE);
            }
        } else {
            downloadBar.setVisibility(View.INVISIBLE);
        }
        //Reload the document, and the newly processed PDFs should be there.
        long start = System.currentTimeMillis();
        loadDocument();
        Logger.d(TAG, String.format("Loading document for downloadID: %s took %s ms.", incomingDownloadId, (System.currentTimeMillis() - start)));
        updateAdapterContent();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    private void paywallTrackPageView(int pageNo) {
        if(pageNo>0 && PaywallService.initialized()){
            int reason = PaywallConstants.getWallReason(PaywallConstants.WallType.METERED_PAYWALL);
            showWallDialog(PaywallService.getInstance().isWpUserLoggedIn(), reason, PaywallConstants.WallType.METERED_PAYWALL);
        }
    }

    @Override
    protected void onStop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        stopAndTrackEngagementTrace();
        super.onStop();
    }

    private void startEngagementTrace() {
        if (!isTutorial() && engagementTrace == null) {
            engagementTrace = new PageEngagementTrace(UUID.randomUUID().toString(), null, null);
            engagementTrace.startTrace();
        }
    }

    private void stopAndTrackEngagementTrace() {
        if (engagementTrace != null && currentPageName != null) {
            engagementTrace.stopTrace();
            engagementTrace.updateTrackingInfo(currentPageName, Measurement.CONTENT_TYPE_MAIN, "print");
            engagementTracker.trackEngagement(engagementTrace);
            engagementTrace = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (reader != null) {
            reader.PDFClose();
            reader = null;
        }
        if (thumbView != null) {
            thumbView.setImageDrawable(null);
        }
        mReceiver = null;
        thumbView = null;
        Global.RemoveTmp();
        pdfThumbnailRecyclerView = null;
        super.onDestroy();
    }

    @Override
    public void onPageChanged(int pageno) {
        int oldPage = pageNo;
        pageNo = pageno;
        if (!isTutorial()) {
            currentPageName = (Measurement.PAGE_PDFFULL +
                    Measurement.archiveDateSectionWithPageNumReplaced(
                            getIntent().getStringExtra(PARAM_ARCHIVE_DATE_SECTION), pageno
                    )).toLowerCase();
        }
        if (printSectionPages == null) {
            return;
        }
        if (!isTutorial()) {
            Measurement.trackPrint(null, Measurement.PAGE_PDFFULL,
                    Measurement.archiveDateSectionWithPageNumReplaced(getIntent().getStringExtra(PARAM_ARCHIVE_DATE_SECTION), pageno), false);
        }
        updateMenuItemText();
        //Check to see if the section was changed.  If it was, it may mean a new download is necessary.
        PrintSectionPage printSectionPage = printSectionPages.get(pageno);
        String newSectionLetter = printSectionPage.getSectionLetter();
        final long sectionLmt = printSectionPage.getSectionLmt() != null ? printSectionPage.getSectionLmt() : 0;
        if (!sectionLetter.equals(newSectionLetter)){
            sectionLetter = newSectionLetter;
            final ArchiveManager am = FlagshipApplication.getInstance().getArchiveManager();
            am.getSynchronizedArchiveObs(label, sectionLetter).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Archive>() {
                @Override
                public void onCompleted() {}
                @Override
                public void onError(Throwable e) {
                    Logger.e(TAG, String.format("Error getting synchronized archive in PDFActivity onPageChanged for archive %s-%s", label, sectionLetter));
                }
                @Override
                public void onNext(Archive archive) {
                    if (archive == null || archive.getStatus() == Archive.Status.Canceled || archive.getStatus() == Archive.Status.Deleted) {
                        //download
                        try {
                            archive = am.scheduleFileDownload(label, sectionLetter, sectionLmt);
                        } catch (IOException e) {
                            Logger.e(TAG, String.format("Error scheduling file download in PDFActivity for archive %s-%s", label, sectionLetter));
                        }
                    }
                    //If this section was somehow mid-download but not being tracked, add it to the download list.
                    if (archive != null && archive.getDownloadId() != null && incomingDownloadIdList != null && !incomingDownloadIdList.contains(archive.getDownloadId())) {
                        incomingDownloadIdList.add(archive.getDownloadId());
                    }
                }
            });
        }


        if (pdfThumbnailRecyclerView != null) {
            pdfThumbnailRecyclerView.getLayoutManager().scrollToPosition(pageNo);
            pdfThumbnailRecyclerView.getAdapter().notifyItemChanged(oldPage);
            pdfThumbnailRecyclerView.getAdapter().notifyItemChanged(pageNo);
        }
        //Only check paywall status if there's no download left to complete.
        //This prevents the download BroadcastReceiver from being interrupted by the paywall.
        if (!this.downloadPending() && !isTutorial()) {
            paywallTrackPageView(pageNo);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PARAM_PAGE_NUMBER, pageNo);
        outState.putSerializable(PARAM_ARCHIVE_ID, incomingDownloadIdList);
        if (pdfThumbnailRecyclerView != null) {
            outState.putBoolean(PARAM_THUMBNAILS_VISIBLE, pdfThumbnailRecyclerView.getVisibility() == View.VISIBLE);
        }
    }

    @Override
    public void onOpenURI(final String uri, final boolean firstAttempt) {
        if (uri.startsWith(CONTINUE_STRING)){
            String re0="(continue:)";
            String re1="([a-z]+)";
            String re2="(\\d+)";

            Pattern p = Pattern.compile(re0+re1+re2,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(uri);
            if (m.find())
            {
                String section=m.group(2);
                String pageNumber=m.group(3);
                StringBuilder sb = new StringBuilder();
                sb.append(section);
                if (pageNumber.length() == 1) {
                    sb.append(0);
                }
                sb.append(pageNumber);
                String pageName = sb.toString();

                for (int i = 0; i < printSectionPages.size(); i++) {
                    PrintSectionPage page = printSectionPages.get(i);
                    if (page.getPageName().equalsIgnoreCase(pageName) && reader != null) {
                        reader.PDFGotoPage(i, true);
                        return;
                    }
                }
            }

            return;
        }
        FileMeta fileMeta = cacheManager.getFileMetaByUrl(uri);
        if (fileMeta == null) {
            indicator.setVisibility(View.VISIBLE);
            //If the user has an internet connection, then it's not a problem to get the article from the internet.
            //Also try this if caching has failed once, as we don't want an endless loop here.
            if (ReachabilityUtil.isConnected(getApplicationContext()) || !firstAttempt) {
                WPUrlAnalyser analyser = WPUrlAnalyser.getWPUrlAnalyser();
                analyser.setAnalysedUrlListener(new WPUrlAnalyser.AnalysedUrlListener() {
                    @Override
                    public void onCancelLoader() {
                        indicator.setVisibility(View.GONE);
                    }

                    @Override
                    public void onModifyLaunchIntent(@NonNull Intent intent) {
                        intent.putExtra(ArticlesParcelKt.CURRENT_TAB_NAME, getString(R.string.tab_print_edition));
                        intent.putExtra(TopBarFragment.SectionDisplayName, getIntent().getStringExtra(TopBarFragment.SectionDisplayName));
                        intent.putExtra(ArticlesParcelKt.PRINT_ORIGINATED, true);
                    }
                });
                analyser.analyseAndStartIntent(this, uri, "");
            } else {
                //If the user isn't online, then we need to try to re-add the articles to cache.
                ArchiveManager am = FlagshipApplication.getInstance().getArchiveManager();
                Logger.w(TAG, String.format("Unable to find story %s in cache for archive %s-%s.  Attempting to refill cache for archive.", uri, label, sectionLetter));
                Archive archive = am.getArchiveByLabelAndSection(label, sectionLetter);
                if (archive == null) {
                    archive = am.getPreviewArchiveForLabel(label);
                }
                am.getProcessArticlesObs(archive).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {}
                    @Override
                    public void onError(Throwable e) {
                        Logger.e(TAG, String.format("Failed to reload cache for archive %s-%s", label, sectionLetter), e);
                        indicator.setVisibility(View.GONE);
                        onOpenURI(uri, false);
                    }
                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (!aBoolean) {
                            Logger.e(TAG, String.format("Failed to reload cache for archive %s-%s", label, sectionLetter));
                        }
                        indicator.setVisibility(View.GONE);
                        onOpenURI(uri, false);
                    }
                });
            }
        } else {
            Intent intent = ArticlesParcel.builder()
                    .setArticleSingleUrl(fileMeta.getUrl())
                    .setTabName(getString(R.string.tab_print_edition))
                    .setSectionDisplayName(getIntent().getStringExtra(TopBarFragment.SectionDisplayName))
                    .printOriginated(true)
                    .buildIntent(this);
            startActivity(intent);
        }
    }

    public void onDocumentOpen() {
        Logger.d(TAG, "onDocumentOpen");
        if (reader == null) {
            return; // ignore callback after onDestroy was called. Radaee has race conditions
        }
        if (reader.isOpen()) {
            reader.PDFReload(pageNo);
        } else {
            reader.PDFOpen(pdfLocations, 1, PdfActivity.this, pageNo, defaultPdfPath);
            reader.setVisibility(View.VISIBLE);

            if (thumbView != null) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(thumbView, "translationX",
                        thumbView.getAlpha(), 0);
                animator.addListener(animatorListenerAdapter);
                animator.setDuration(500);
                animator.start();
            }
        }
        Logger.d(TAG, "onDocumentOpen complete");
    }

    private boolean downloadPending() {
        return !incomingDownloadIdList.isEmpty();
    }

    protected boolean showOverlay() {
        return true;
    }

    protected int getOverlayLayoutId() {
        return R.layout.activity_pdf_overlay;
    }

    public void updateAdapterContent() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ThumbnailAdapter adapter = (ThumbnailAdapter) pdfThumbnailRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public View.OnClickListener getThumbnailClickListener() {
        if (thumbnailListener == null) {
            thumbnailListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reader.PDFGotoPage((Integer) v.getTag(), true);
                    if (pdfThumbnailRecyclerView != null) {
                        pdfThumbnailRecyclerView.setVisibility(View.GONE);
                        updateMenuItemTextColor();
                    }
                }
            };
        }
        return thumbnailListener;
    }

    @Override
    public int getPageNo() {
        return pageNo;
    }

    private static class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailVH>{

        private List<PrintSectionPage> pages;
        private long label;
        private Context context;
        private ThumbnailClickListener listener;

        ThumbnailAdapter(ArrayList<PrintSectionPage> pages, long label, Context context, ThumbnailClickListener listener) {
            this.pages = pages;
            this.label = label;
            this.context = context;
            this.listener = listener;
        }

        @Override
        public ThumbnailVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View thumbnailView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_pdf_thumbnail_item, parent, false);
            ThumbnailVH thumbnailVH = new ThumbnailVH(thumbnailView);
            thumbnailVH.thumbnail.setOnClickListener(listener.getThumbnailClickListener());

            return thumbnailVH;
        }

        @Override
        public void onBindViewHolder(final ThumbnailVH holder, int position) {

            holder.thumbnail.setImageDrawable(null);
            final PrintSectionPage page = pages.get(position);

            String sectionLetter = page.getSectionLetter();
            File imageFile = ArchiveManager.TUTORIAL_SECTION_NAME.equals(sectionLetter) ? new File(ArchiveManager.getTutorialAssetFilePath(context, page.getThumbnailPath())):
                    ArchiveManager.getFullFilePath(context, label, sectionLetter, page.getThumbnailPath());

            float aspectRatio = (float) page.getPageWidth() / page.getPageHeight();
            holder.imageFrame.setAspectRatio(aspectRatio);

            Picasso.get().load(imageFile).placeholder(R.drawable.archives_placeholder).noFade()
                    .error(R.drawable.archives_placeholder).fit().centerInside().into(holder.thumbnail);

            holder.thumbnail.setTag(holder.getAdapterPosition());

            holder.pageName.setText(page.getPageName());

            holder.itemView.setSelected(position == listener.getPageNo());

        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    private static class ThumbnailVH extends RecyclerView.ViewHolder {

        public ImageView thumbnail;
        public TextView pageName;
        public ProportionalLayout imageFrame;

        ThumbnailVH(View itemView){
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.pdfThumbnail);
            pageName = (TextView) itemView.findViewById(R.id.pageName);
            imageFrame = (ProportionalLayout) itemView.findViewById(R.id.image_frame);
        }

    }

    private static class PdfHandler extends Handler {
        private final WeakReference<PdfActivity> mActivity;

        public PdfHandler(PdfActivity activity) {
            mActivity = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(Message msg) {

            if (mActivity == null || mActivity.get() == null || mActivity.get().isFinishing()) return;

            if (mActivity.get().incomingDownloadIdList != null && !mActivity.get().incomingDownloadIdList.isEmpty()) {
                ProgressBar downloadBar = mActivity.get().downloadBar;
                long downloadId = mActivity.get().incomingDownloadIdList.get(0);
                Download download = null;
                try {
                    download = Download.get(mActivity.get(), downloadId);
                } catch (Exception e) {
                    Logger.e(TAG, "handleMessage ", e);
                }
                if (download != null) {
                    int status = download.getStatus();
                    boolean isDownloading = status == DownloadManager.STATUS_PAUSED || status == DownloadManager.STATUS_PENDING ||
                            status == DownloadManager.STATUS_RUNNING;
                    if (downloadBar != null) {
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
                    mActivity.get().finishDownload(downloadId);
                }
            }
            if (mActivity.get().getmHandler() != null) {
                mActivity.get().getmHandler().sendEmptyMessageDelayed(0, 1000);
            }
        }
    }

    public PdfHandler getmHandler() {
        return mHandler;
    }
}
