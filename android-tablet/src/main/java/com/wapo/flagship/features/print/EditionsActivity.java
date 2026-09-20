package com.wapo.flagship.features.print;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.data.ArchiveManager;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.model.PrintManifestResponse;
import com.wapo.android.commons.util.Logger;
import com.wapo.flagship.util.UIUtil;
import com.washingtonpost.android.R;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.PrintConfigStub;

import org.threeten.bp.LocalDate;

import java.io.File;
import java.util.Date;

import dagger.hilt.android.AndroidEntryPoint;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by curacamalitod on 12/23/16.
 */
@AndroidEntryPoint
public class EditionsActivity extends BaseActivity implements PrintActivityInterface {

    public static final String TAG = EditionsActivity.class.getName();
    public final static String DateParam = EditionsActivity.class.getSimpleName() + ".Date";
    private final static String FIRST_SECTION_LETTER = "A";

    private CompositeSubscription compositeSubscription;
    private MaterialCalendarView calendarView;
    private ProgressBar frontPageLoadingProgressBar;
    private Date selectedDate;
    private int minTileSizePx;
    private ImageView frontPageImageView;
    private ArchiveManager archiveManager;
    private static CharSequence[] weekdayLabels = new CharSequence[]{"M", "T", "W", "T", "F", "S", "S"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editions);

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
            actionBar.setDisplayShowTitleEnabled(true);
        }

        //Issues in the MaterialCalendarView class means you have to refresh layout on rotation.
        DisplayMetrics dm = UIUtil.displayMetrics(this);
        //The height of the toolbar needs to be factored in.
        int toolbarHeight = ((int) getResources().getDimension(R.dimen.phone_bottom_bar_height));
        minTileSizePx = (Math.min(dm.widthPixels, dm.heightPixels) - toolbarHeight) / 8;

        calendarView = (MaterialCalendarView) findViewById(R.id.editionsCalendar);
        frontPageImageView = (ImageView) findViewById(R.id.frontPageImageView);
        frontPageLoadingProgressBar = (ProgressBar) findViewById(R.id.frontPageLoadingProgress);

        frontPageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Date currentDate = ArchiveManager.getCurrentDate();

        selectedDate = ArchiveManager.getPrintEditionDate(this);

        DrawableCompat.setTint(calendarView.getLeftArrow(), ContextCompat.getColor(getApplicationContext(), R.color.white));
        DrawableCompat.setTint(calendarView.getRightArrow(), ContextCompat.getColor(getApplicationContext(), R.color.white));

        LocalDate maxLocalDate = Utils.getLocalDate(currentDate);
        LocalDate minLocaldate = Utils.getLocalDate(new Date(currentDate.getTime() - (1000 * 60 * 60 * 24 * 13)));
        calendarView.state().edit()
                .setMinimumDate(minLocaldate)
                .setMaximumDate(maxLocalDate).commit();

        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_OUT_OF_RANGE);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                CalendarDay selectedCalendarDay = getCalendarDay(selectedDate);
                if(selectedCalendarDay.getDay() != date.getDay()) {
                    selectedDate = Utils.getDate(date.getDate());
                    widget.setDateSelected(date, selected);
                    ArchiveManager.setPrintEditionDate(EditionsActivity.this, selectedDate.getTime());
                    getManifestForDate(selectedDate);
                }
            }
        });

        calendarView.setWeekDayLabels(weekdayLabels);
        CalendarDay selectedCalendarDay = getCalendarDay(selectedDate);
        calendarView.setDateSelected(selectedCalendarDay, true);
        //This is needed to make sure that the month the user is currently on is shown if 14 day split is over two months.
        calendarView.setCurrentDate(selectedCalendarDay);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeSubscription = new CompositeSubscription();

        getManifestForDate(selectedDate);
    }

    @Override
    public void onResume() {
        super.onResume();
        int defaultTileSize = (int) getResources().getDimension(R.dimen.editions_calendar_tile_size);
        //This should catch cases on small devices where the default tile size is too large to fit on the phone.
        calendarView.setTileSize(Math.min(minTileSizePx, defaultTileSize));
    }

    @Override
    protected void onDestroy() {
        if (frontPageImageView != null) {
            frontPageImageView.setImageDrawable(null);
            frontPageImageView = null;
        }
        if (calendarView != null) {
            calendarView.setOnDateChangedListener(null);
            calendarView.removeAllViews();
            calendarView = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DateParam, selectedDate.getTime());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
        compositeSubscription = null;
        Picasso.get().cancelRequest(frontPageImageView);
    }

    /**
     * Gets the manifest for a specific date from ArchiveManager, and uses the manifest response to
     * populate the front page image for the given date.  Attempts to load the image from disk
     * if the page has been downloaded already, and loads it from the internet if it hasn't been downloaded
     * as part of a bundle.  If both fail, this displays a placeholder image.
     * @param date the specific date to get the manifest for.
     */
    private void getManifestForDate(final Date date) {
        if (frontPageImageView != null && compositeSubscription != null && date != null) {
            frontPageImageView.setImageDrawable(null);
            compositeSubscription.clear();
            final long dateLabel = Utils.dateToEDTLabel(date);
            Logger.d(TAG, String.format("Requesting manifest for %s", dateLabel));
            compositeSubscription.add(getArchiveManager().getPrintManifest(date).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<PrintManifestResponse>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            Logger.e(TAG, String.format("Unable to load manifest for date=%s", dateLabel), e);
                            setFrontPageImagePlaceholder();
                        }

                        @Override
                        public void onNext(PrintManifestResponse printManifestResponse) {
                            if (printManifestResponse != null && printManifestResponse.getIssue().getFrontPageImageName() != null) {
                                File frontPageFile = ArchiveManager.getFullFilePath(EditionsActivity.this, Utils.dateToEDTLabel(selectedDate), FIRST_SECTION_LETTER, printManifestResponse.getIssue().getFrontPageImageName());
                                if (frontPageFile.exists() && !frontPageFile.isDirectory()) {
                                    loadFrontPageImageFromDisk(frontPageFile, date);
                                } else {
                                    loadFrontPageImageFromInternet(getFrontPageImageUrl(date, printManifestResponse.getIssue().getFrontPageImageName()));
                                }
                            } else {
                                setFrontPageImagePlaceholder();
                            }
                        }
                    })
            );
        } else {
            Logger.w(TAG, "Expected value not found.");
            setFrontPageImagePlaceholder();
        }
    }

    /**
     * Loads a front page image into an ImageView using a URL and Picasso.
     * If this fails, load a placeholder image instead.
     * @param frontPageUrl url where the image can be found
     */
    private void loadFrontPageImageFromInternet(final String frontPageUrl) {
        if (frontPageImageView != null) {
            if (frontPageLoadingProgressBar != null) {
                frontPageLoadingProgressBar.setVisibility(View.VISIBLE);
            }

            Callback callback = new Callback() {
                @Override
                public void onSuccess() {
                    if (frontPageLoadingProgressBar != null) {
                        frontPageLoadingProgressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Logger.e(TAG, "Error loading front page image from internet at URL " + frontPageUrl, e);
                    setFrontPageImagePlaceholder();
                }
            };

            int frontPageImageViewWidth = frontPageImageView.getWidth();

            if (frontPageImageViewWidth > 0) {
                Picasso.get().load(frontPageUrl).resize(frontPageImageViewWidth, 0).into(frontPageImageView, callback);
            }
        }
    }

    /**
     * Loads a front page image into an ImageView using a file location and Picasso.
     * If this fails, attempt to load the image from the internet.
     * @param frontPageFile url where the image can be found
     */
    private void loadFrontPageImageFromDisk(final File frontPageFile, final Date date) {
        if (frontPageImageView != null) {
            if (frontPageLoadingProgressBar != null) {
                frontPageLoadingProgressBar.setVisibility(View.VISIBLE);
            }

            Callback callback = new Callback() {
                @Override
                public void onSuccess() {
                    if (frontPageLoadingProgressBar != null) {
                        frontPageLoadingProgressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Logger.e(TAG, "Error loading front page image from disk at path " + frontPageFile.getPath(), e);
                    loadFrontPageImageFromInternet(getFrontPageImageUrl(date, frontPageFile.getName()));
                }
            };

            int frontPageImageViewWidth = frontPageImageView.getWidth();

            if (frontPageImageViewWidth > 0) {
                Picasso.get().load(frontPageFile).resize(frontPageImageView.getWidth(), 0).into(frontPageImageView, callback);
            }
        }
    }

    /**
     * Load a placeholder image into frontPageImageView.
     */
    private void setFrontPageImagePlaceholder() {
        if (frontPageImageView != null) {
            if (frontPageLoadingProgressBar != null) {
                frontPageLoadingProgressBar.setVisibility(View.VISIBLE);
            }

            Callback callback = new Callback() {
                @Override
                public void onSuccess() {
                    if (frontPageLoadingProgressBar != null) {
                        frontPageLoadingProgressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Logger.e(TAG, "Error loading front page image.", e);
                    if (frontPageLoadingProgressBar != null) {
                        frontPageLoadingProgressBar.setVisibility(View.GONE);
                    }
                }
            };

            int frontPageImageViewWidth = frontPageImageView.getWidth();

            if (frontPageImageViewWidth > 0) {
                Picasso.get().load(R.drawable.archives_placeholder).resize(frontPageImageView.getWidth(), 0).into(frontPageImageView, callback);
            }
        }
    }

    /**
     * Returns a URL for the front page image of the current date.
     * @param date the date of the front page image
     * @param coverImagePath filename of the cover image
     * @return complete URL of front page image
     */
    private String getFrontPageImageUrl(Date date, String coverImagePath) {
        PrintConfigStub printConfig = ConfigManager.Companion.getInstance().getConfig().getPrintConfig();
        return printConfig.getNewsstandBaseURL() + String.format(
                printConfig.getFrontPageImageURLTemplate(), Utils.dateToEDTLabel(date), coverImagePath);
    }

    /**
     * Inner method to access singleton of ArchiveManager.
     * @return local reference to the archive manager.
     */
    private ArchiveManager getArchiveManager() {
        if (archiveManager == null) {
            archiveManager = FlagshipApplication.getInstance().getArchiveManager();
        }
        return archiveManager;
    }

    private static CalendarDay getCalendarDay(@NonNull Date date) {
        CalendarDay calendarDay = CalendarDay.from(Utils.getLocalDate(date));
        return calendarDay;
    }
}
