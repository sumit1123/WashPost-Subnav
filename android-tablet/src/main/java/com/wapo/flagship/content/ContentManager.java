package com.wapo.flagship.content;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.wapo.android.commons.util.AppContextUtils;
import com.wapo.android.commons.util.Logger;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.wapo.android.commons.config.ConfigManager;
import com.wapo.android.commons.config.Constants;
import com.wapo.android.commons.exceptions.LoggableError;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.domain.repository.RemoteLogRepo;
import com.wapo.android.remotelog.logger.EventTimerLog;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.ConfigSyncObservableFactory;
import com.wapo.flagship.Utils;
import com.wapo.flagship.common.TrackingOperator;
import com.wapo.flagship.common.errors.SectionParseError;
import com.wapo.flagship.config.SiteServiceConfig;
import com.wapo.flagship.config.SiteServiceConfigUtils;
import com.wapo.flagship.content.notifications.NotificationData;
import com.wapo.flagship.content.notifications.NotificationModel;
import com.wapo.flagship.data.CacheManager;
import com.wapo.flagship.data.FileMeta;
import com.wapo.flagship.external.WidgetUtils;
import com.wapo.flagship.features.articles.recirculation.model.MostReadFeed;
import com.wapo.flagship.features.articles2.interfaces.SubNavElectionHandler;
import com.wapo.flagship.features.articles2.models.Article2;
import com.wapo.flagship.features.articles2.repo.Articles2Repository;
import com.wapo.flagship.features.ask.models.CategoryItem;
import com.wapo.flagship.features.ask.models.QuestionItem;
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo;
import com.wapo.flagship.features.fusion.FusionContentUtils;
import com.wapo.flagship.features.grid.FusionMapper;
import com.wapo.flagship.features.grid.GridDebugUtils;
import com.wapo.flagship.features.grid.GridEntity;
import com.wapo.flagship.features.grid.views.vote.VoteGuide;
import com.wapo.flagship.features.grid.views.vote.VoteGuideApi;
import com.wapo.flagship.features.grid.views.vote.VoteGuideService;
import com.wapo.flagship.features.notification.AlertManager;
import com.wapo.flagship.features.sections.PageLayout;
import com.wapo.flagship.features.sections.PageManager;
import com.wapo.flagship.features.sections.model.Section;
import com.wapo.flagship.json.MenuSection;
import com.wapo.flagship.model.ArticleMeta;
import com.wapo.flagship.model.Status;
import com.wapo.flagship.network.request.MostReadFeedRequest;
import com.wapo.flagship.network.request.VoteRequest;
import com.wapo.flagship.push.PushUpdaterCallback;
import com.wapo.flagship.querypolicies.BypassCacheQueryPolicy;
import com.wapo.flagship.querypolicies.CacheOnlyQueryPolicy;
import com.wapo.flagship.querypolicies.LMTQueryPolicy;
import com.wapo.flagship.querypolicies.Query;
import com.wapo.flagship.services.data.ITaskStatusListener;
import com.wapo.flagship.services.data.Task;
import com.wapo.flagship.sync.ProgressTaskListener;
import com.wapo.flagship.sync.UpdateSfTaskListener;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.rx.RxUtils;
import com.wapo.flagship.wrappers.CrashWrapper;
import com.washingtonpost.android.BuildConfig;
import com.washingtonpost.android.R;
import com.washingtonpost.android.comics.ComicsService;
import com.washingtonpost.android.comics.model.ComicStrip;
import com.washingtonpost.android.config.domain.models.config.AskThePostCategory;
import com.washingtonpost.android.config.domain.models.config.AskThePostQuestion;
import com.washingtonpost.android.config.domain.models.config.Config;
import com.washingtonpost.android.config.domain.models.config.RecipesConfig;
import com.washingtonpost.android.config.domain.models.config.Search2Config;
import com.washingtonpost.android.volley.RequestQueue;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.wapocontent.Priority;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class ContentManager implements Handler.Callback, PageManager, AlertManager, VoteGuideService, SubNavElectionHandler {

    private Articles2Repository articles2Repository;
    private static final boolean D = BuildConfig.DEBUG;
    private static final String DTAG = "[d][cm]";

    private final static Long DEFAULT_NOTIFICATION_TTL = 1000L * 60L * 60L * 48L;
    private final PageImageLoader pageImageLoader;
    private final Context context;
    private final CacheManager cacheManager;
    private final Observable<Config> configObs = com.washingtonpost.android.config.domain.manager.ConfigManager.Companion.getInstance().getConfigObs();
    private final BehaviorSubject<MostReadFeed> mostReadFeedSubj = BehaviorSubject.create();
    private final BehaviorSubject<ComicsService> comicsServiceSubj = BehaviorSubject.create();
    private final BehaviorSubject<List<Section>> sectionsSubject = BehaviorSubject.create();
    private Integer retrySections = 0;
    private final RequestQueue requestQueue;
    private final PublishSubject<PageInfo> pagesSubject = PublishSubject.create();
    private final WapoConfigManager wapoConfigManager;
    private final ContentUpdateRulesManager contentUpdateRulesManager;
    private static final int MSG_CALLBACK_STATUS = 3;

    private static final int MSG_CALLBACK_PROGRESS = 4;
    private static final String COMICS_TRACKING_TAG = "update-comics-list";

    private HashMap<ITaskStatusListener, ListenerWrapper> callbacksMap = new HashMap<>();
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper(), this);

    protected BehaviorSubject<List<NotificationData>> notificationsSubject = BehaviorSubject.create();
    private static final String ELECTION_TIME = "election_time";
    private Boolean isLowDataModeEnable = false;

    private final BehaviorSubject<List<ComicStrip>> comicsSubject = BehaviorSubject.create((List<ComicStrip>) null);

    private RemoteLogRepo remoteLogRepo;

    private Config getCurrentConfig() {
        return com.washingtonpost.android.config.domain.manager.ConfigManager.Companion.getInstance().getConfig();
    }

    private volatile long workingThreadId = -1;
    protected final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(r, "th-contentMngr") {
                        @Override
                        public void run() {
                            workingThreadId = this.getId();
                            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                            super.run();
                        }
                    };
                }
            }
    );
    private Scheduler scheduler = Schedulers.from(executor);

    private TrackingOperator.TrackListener trackListener = new TrackingOperator.TrackListener() {
        @Override
        public void onStart(String tag) {
            startTracking(tag);
        }

        @Override
        public void onFinish(String tag) {
            stopTracking(tag);
        }

        private void startTracking(String tag) {
            if (TextUtils.isEmpty(tag)) return;
            if (isTrackingEnabled(tag)) {
                EventTimerLog.startTimingEvent(tag, EventTimerLog.DATA_RENDER_LOAD);
            }
        }

        private void stopTracking(String tag) {
            if (TextUtils.isEmpty(tag)) return;
            if (isTrackingEnabled(tag)) {
                HashMap additionalFields = new HashMap<String, String>();
                additionalFields.put("tag", tag);
                EventTimerLog.stopTimingEventAndLog(tag, EventTimerLog.DATA_RENDER_LOAD, context,
                        true, additionalFields, "Data Load Metrics");
            }
        }

        private boolean isTrackingEnabled(String tag) {
            return COMICS_TRACKING_TAG.equals(tag);
        }
    };

    public ContentManager(
            @NonNull final Context context,
            @NonNull final CacheManager cacheManager,
            @NonNull RequestQueue requestQueue,
            PageImageLoader pageImageLoader,
            ContentUpdateRulesManager contentUpdateRulesManager,
            Articles2Repository repo,
            AskQuestionsRepo askQuestionsRepo,
            RemoteLogRepo remoteLogRepo
    ) {
        this.context = context;
        this.cacheManager = cacheManager;
        this.requestQueue = requestQueue;
        this.pageImageLoader = pageImageLoader;
        this.remoteLogRepo = remoteLogRepo;
        this.wapoConfigManager = new WapoConfigManager(context, remoteLogRepo);
        this.contentUpdateRulesManager = contentUpdateRulesManager;
        this.articles2Repository = repo;

        this.configObs
                .observeOn(scheduler)
                .map(config -> {
                    askQuestionsRepo.setDefaultQuestions(getAskThePostQuestionsFromConfig(config.getSearch2Config()));
                    askQuestionsRepo.setDefaultCategories(getAskThePostCategoriesFromConfig(config.getSearch2Config()));
                    return config.getComicsConfigStub().getEndpointURL();
                })
                .distinctUntilChanged()
                .map(new Func1<String, ComicsService>() {
                    @Override
                    public ComicsService call(String url) {
                        return new ComicsService(
                                url,
                                UIUtil.sizeInDp(context),
                                context
                        );
                    }
                })
                .onErrorResumeNext(Observable.<ComicsService>empty())
                .subscribe(comicsServiceSubj);

        notificationsSubject.onNext(cacheManager.getNotifications());
    }

    private Observable<ComicsService> getComicsServiceObs() {
        return comicsServiceSubj.take(1);
    }

    public Observable<Void> markPushArticleAsFavorite(final ArticleMeta articleMeta) {
        return null;
    }

    /**
     * Depending on parameters downloads/check for an section update
     *
     * @param pageName
     * @param forceUpdate
     * @return Observable which
     * - closes without issuing an item the page hasn't change
     * - issue a new version of the page and completes
     */
    @Override
    public Observable<PageLayout> updatePage(final String pageName, final boolean forceUpdate) {
        return updatePage(pageName, forceUpdate, Priority.Group.FOREGROUND)
                .onErrorResumeNext(new Func1<Throwable, Observable<PageLayout>>() {
                    @Override
                    public Observable<PageLayout> call(Throwable throwable) {
                        return Observable.error(
                                throwable instanceof FourFifteenError ?
                                        new PageManagerFourFifteenException(((FourFifteenError) throwable).getContentUrl()) :
                                        throwable
                        );
                    }
                });
    }

    @Override
    public void onPageStop(String pageName) {
        contentUpdateRulesManager.setTime(ContentUpdateRulesManagerImpl.TimeType.PAGE_STOP_TIME, fixPageName(pageName));
    }

    @Override
    public boolean shouldClearPage(String pageName) {
        final String correctPageName = fixPageName(pageName);
        return contentUpdateRulesManager.refreshNeeded(correctPageName)
                && !contentUpdateRulesManager.shouldConsiderCache(correctPageName);
    }

    @Override
    public boolean shouldUpdatePage(String pageName) {
        return true;
    }

    @Override
    public void onLowDataMode(Boolean enable) {
        isLowDataModeEnable = enable;
    }

    public Observable<PageLayout> updatePage(
            @NonNull final String pageName,
            final boolean forceUpdate,
            @NonNull final Priority.Group priorityGroup
    ) {
        final String correctPageName = fixPageName(pageName);
        Observable<String> urlObs = getPageUrlObs(correctPageName);
        if (forceUpdate) {
            urlObs = urlObs
                    .observeOn(scheduler)
                    .doOnNext(new Action1<String>() {
                        @Override
                        public void call(String url) {
                            cacheManager.dropFileMetaSoftTtl(url);
                        }
                    });
        }

        return urlObs
            .flatMap(new Func1<String, Observable<PageLayout>>() {
                @Override
                public Observable<PageLayout> call(final String url) {
                    cacheManager.dropFileMetaSoftTtl(url);
                    Observable<PageLayout> pageLayoutObservable;
                    switch (getSectionSubtype(correctPageName)){
                        case FUSION:
                            pageLayoutObservable = createFusionPageObservable(url, correctPageName, priorityGroup, forceUpdate);
                            break;
                        default:
                            pageLayoutObservable = Observable.empty();
                    }

                    pageLayoutObservable
                            .map(pageLayout -> {
                                executor.execute(() -> pagesSubject.onNext(new PageInfo(pageName, url, pageLayout)));
                                return pageLayout;
                            })
                            .doOnError(throwable -> {
                                if (throwable instanceof FourFifteenError) {
                                    cacheManager.dropFileMeta(url);
                                }
                            });

                    return pageLayoutObservable;
                }
            })
            .doOnError(throwable -> {
                if (throwable instanceof LoggableError) {
                    EventLog.Builder eventLogBuilder = new EventLog.Builder()
                            .setMessage("ContentManager Section Load Error")
                            .setModule(LogModules.SECTIONS)
                            .set("section_name", correctPageName)
                            .setErrorMessage(throwable.getMessage());
                    if (throwable instanceof SectionParseError) {
                        eventLogBuilder.set("original_json", ((SectionParseError) throwable).getOriginalJson());
                    }
                    RemoteLog.e(context, eventLogBuilder.build());
                }
            })
            .lift(new TrackingOperator<>("update-page/" + pageName, trackListener));
    }

    private Observable<PageLayout> createFusionPageObservable(String url, String pageName, Priority.Group priorityGroup, Boolean forceUpdate) {
        Observable<GridEntity> fusionObservable = null;
        if (BuildConfig.DEBUG) {
            com.wapo.flagship.config.Section section = SiteServiceConfigUtils.findSectionByPath(pageName, SiteServiceConfigUtils.createFusionLocalSiteService());
            if (section != null) {
                int raw = context.getResources().getIdentifier(section.getSectionId(), "raw", context.getPackageName());
                if (raw > 0) {
                    GridEntity gridEntity = GridDebugUtils.parseGridJson(context, raw);
                    fusionObservable = Observable.just(gridEntity);
                }
            }
        }

        if (fusionObservable == null) {
            fusionObservable = UpdateFusionPageObservable.create(
                    url,
                    getRequestQueue(),
                    new Priority(priorityGroup, System.currentTimeMillis()), forceUpdate
            );
        }

        return fusionObservable
                .doOnNext(gridEntity -> {
                    boolean isHomepage = pageName.equals("/.");

                    // 2. Fall back to a safe tracking check if the pageName doesn't catch it
                    if (!isHomepage && gridEntity != null && gridEntity.getTracking() != null) {
                        String trackingSection = gridEntity.getTracking().getSection();
                        if (trackingSection != null && trackingSection.contains("homepage")) {
                            isHomepage = true;
                        }
                    }

                    // Execute prefetch if either check passes
                    if (isHomepage) {
                        List<ArticleMeta> articleMetas = FusionContentUtils.getArticleUrls(gridEntity);
                        List<Query<Article2>> queryList = new ArrayList<>();
                        for (ArticleMeta articleMeta : articleMetas) {
                            queryList.add(new Query<>(articleMeta.id, new LMTQueryPolicy(articleMeta.lastModified)));
                        }
                        articles2Repository.prefetchArticle(queryList);
                    }
                })
                .map(gridEntity -> new PageLayout(gridEntity));
    }
    private static String fixPageName(@NonNull String pageName) {
        // Using pageName as-is in debug as pages can have a .json as an extension in stage.
        if (AppContextUtils.INSTANCE.isDebuggableBuild()) {
            return pageName;
        } else {
            return pageName.replace(".json", "");
        }
    }

    public Observable<PageInfo> listenPageUpdates() {
        return pagesSubject
                .subscribeOn(scheduler)
                .observeOn(scheduler);
    }

    /**
     * @param pageName
     * @return an observable which issues cached value (null, if non) and then all subsequent page updates
     */
    @Override
    public Observable<PageLayout> listenToPage(@NonNull final String pageName, boolean forceRefresh) {
        if (D) {
            Logger.d(DTAG, "Listening for page: " + pageName);
        }

        final String correctPageName = fixPageName(pageName);
        SectionSubtype sectionSubtype = getSectionSubtype(correctPageName);
        return getPageUrlObs(correctPageName)
                .flatMap((Func1<String, Observable<PageLayout>>) pageUrl -> {

                    Observable<PageLayout> pageUpdates = pagesSubject.asObservable()
                            .filter((PageInfo pageInfo) -> pageUrl.equals(pageInfo.pageUrl))
                            .map((PageInfo pageInfo) -> {
                                if (D) {
                                    Logger.d(DTAG, "an update for the page " + pageInfo.pageName + ": (" + (pageInfo.page == null ? "null" : "<data>") + ")");
                                }
                                return pageInfo.page;
                            });

                    Observable<PageLayout> deferredPageObservable = Observable.defer(() -> {

                        FileMeta fileMeta = cacheManager.getFileMetaByUrl(pageUrl);
                        String cacheContent = readFileMeta(fileMeta);
                        final PageLayout pageFromCache;
                        if (cacheContent != null) {
                            switch (sectionSubtype){
                                case FUSION:
                                    GridEntity gridEntity = FusionMapper.INSTANCE.getGson().fromJson(cacheContent, GridEntity.class);
                                    pageFromCache = new PageLayout(gridEntity);
                                    break;
                                default: pageFromCache = null;
                            }
                        } else {
                            pageFromCache = null;
                        }

                        final boolean refreshNeeded = contentUpdateRulesManager.refreshNeeded(correctPageName);
                        final boolean shouldConsiderCache = contentUpdateRulesManager.shouldConsiderCache(correctPageName);

                        final boolean doesContentNeedRefresh = (fileMeta != null) && contentUpdateRulesManager.doesContentNeedRefresh(fileMeta.getServerDate());

                        final boolean doWarmUpdate = (refreshNeeded || forceRefresh) && pageFromCache != null && shouldConsiderCache && !doesContentNeedRefresh;
                        //final boolean doColdUpdate = refreshNeeded && (pageFromCache == null || !shouldConsiderCache || doesContentNeedRefresh);
                        final boolean doColdUpdate = pageFromCache == null || ((refreshNeeded || forceRefresh) && (!shouldConsiderCache || doesContentNeedRefresh));

                        if (D) {
                            Logger.d(DTAG, "SyncRules: doColdUpdate: " + doColdUpdate +
                                    ", doWarmUpdate: " + doWarmUpdate +
                                    ", rn/sCC/dCNR/fR: " + refreshNeeded + "/" + shouldConsiderCache + "/" + doesContentNeedRefresh + "/" + forceRefresh +
                                    ", pN: " + correctPageName);
                        }

                        // Logging page cache state
                        EventLog.Builder eventLogBuilder = new EventLog.Builder()
                                .setModule(LogModules.SECTIONS)
                                .set("section_name", correctPageName)
                                .set("section_subtype", sectionSubtype)
                                .set("force_refresh", forceRefresh);
                        if (pageFromCache == null) {
                            eventLogBuilder
                                    .setMessage("Cache miss");
                        } else if (doWarmUpdate || doColdUpdate) {
                            eventLogBuilder
                                    .setMessage("Cache refresh")
                                    .set("refresh_needed", refreshNeeded)
                                    .set("consider_cache", shouldConsiderCache)
                                    .set("need_refresh", doesContentNeedRefresh)
                                    .set("file_ttl", fileMeta != null ? fileMeta.getTtl() : -1)
                                    .set("current_time", System.currentTimeMillis())
                                    .set("warm_update", doWarmUpdate)
                                    .set("cold_update", doColdUpdate);
                        }
                        RemoteLog.d(context, eventLogBuilder.build());

                        if (doWarmUpdate) {
                            updatePage(correctPageName, true, Priority.Group.FOREGROUND)
                                    .onErrorResumeNext(Observable.empty())
                                    .subscribe();
                            return Observable.just(pageFromCache);
                        } else if (doColdUpdate) {
                            boolean isTtlExpired = fileMeta == null || fileMeta.getTtl() < System.currentTimeMillis();
                            Observable<PageLayout> onErrorObservable = pageFromCache != null && !isTtlExpired
                                    ? Observable.just(pageFromCache)
                                    : Observable.error(new InterruptedIOException("ui timeout"));
                            long uiTimeout = getCurrentConfig().getContentUpdateRulesConfig().getSectionsUiRequestsTimeout();
                            return updatePage(correctPageName, true)
                                    .first()
                                    .timeout(uiTimeout, TimeUnit.MILLISECONDS)
                                    .onErrorResumeNext(onErrorObservable);
                        } else {
                            return Observable.just(pageFromCache);
                        }
                    });

                    return deferredPageObservable
                            .lift(new TrackingOperator<>("page-listen/" + pageName, trackListener))
                            .concatWith(pageUpdates)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread());
                })
                .onErrorResumeNext((Func1<Throwable, Observable<PageLayout>>) throwable -> Observable.error(
                        throwable instanceof FourFifteenError ?
                                new PageManagerFourFifteenException(((FourFifteenError) throwable).getContentUrl()) :
                                throwable
                ));
    }

    private String readFileMeta(FileMeta fileMeta) {
        if (fileMeta != null && fileMeta.getTtl() >= System.currentTimeMillis()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(fileMeta.getPath());
                return Utils.inputStreamToString(inputStream);
            } catch (Exception e) {
                if (D) {
                    Logger.e(DTAG, "Error reading file meta", e);
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return null;
    }

    @NonNull
    public Observable<List<NotificationData>> addNotification(@NonNull final NotificationData notification, @NonNull PushUpdaterCallback pushUpdaterCallback) {
        return Observable.fromCallable(new Callable<NotificationData>() {
                    @Override
                    public NotificationData call() throws Exception {
                        getCacheManager().addNotification(notification);
                        List<NotificationData> notifications = getCacheManager().getNotifications();
                        notificationsSubject.onNext(notifications);
                        return notification;

                    }
                })
                .subscribeOn(scheduler)
                .doOnNext(new Action1<NotificationData>() {
                    @Override
                    public void call(NotificationData notificationData) {
                        fetchNotificationDataAsync(notificationData, pushUpdaterCallback);
                    }
                })
                .map(new Func1<NotificationData, List<NotificationData>>() {
                    @Override
                    public List<NotificationData> call(NotificationData notificationData) {
                        List<NotificationData> notifications = getCacheManager().getNotifications();
                        return getRecent(notifications);
                    }
                })
                .lift(new TrackingOperator<List<NotificationData>>("add-notif", trackListener));
    }

    private List<NotificationData> getRecent(List<NotificationData> notificationDatas) {
        List<NotificationData> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (NotificationData notificationData : notificationDatas) {
            if (now - formatTimeStamp(notificationData.getTimestamp()) <= DEFAULT_NOTIFICATION_TTL) {
                filtered.add(notificationData);
            }
        }
        return filtered;
    }

    @NonNull
    @Override
    public Observable<List<NotificationData>> getRecentNotifications() {
        return notificationsSubject
                .map(new Func1<List<NotificationData>, List<NotificationData>>() {
                    @Override
                    public List<NotificationData> call(List<NotificationData> notificationDatas) {
                        return getRecent(notificationDatas);
                    }
                })
                .lift(new TrackingOperator<List<NotificationData>>("get-recent-notif", trackListener));
    }

    @NonNull
    public Observable<Void> readNotification(final int id){
        return Observable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getCacheManager().readNotification(id);
                notificationsSubject.onNext(getCacheManager().getNotifications());
                return null;
            }
        }).onErrorResumeNext(Observable.<Void>empty()).subscribeOn(scheduler);
    }

    @NonNull
    @Override
    public Observable<Void> readNotifications(@NonNull final List<NotificationData> notifications){
        return Observable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (NotificationData notification : notifications) {
                    notification.setRead(true);
                }
                getCacheManager().updateNotifications(notifications);
                notificationsSubject.onNext(getCacheManager().getNotifications());
                return null;
            }
        }).subscribeOn(scheduler)
                .lift(new TrackingOperator<Void>("read-notif", trackListener));
    }

    @NonNull
    @Override
    public Observable<Void> clearAllNotifications() {
        return Observable.fromCallable(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        getCacheManager().deleteNotifications(getCacheManager().getNotifications());
                        notificationsSubject.onNext(getCacheManager().getNotifications());
                        return null;
                    }
                })
                .subscribeOn(scheduler)
                .lift(new TrackingOperator<Void>("clear-all-notif", trackListener));
    }

    @NonNull
    public Observable<Void> deleteOldNotification() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        List<NotificationData> notifications = getCacheManager().getNotifications();
                        long now = System.currentTimeMillis();
                        ArrayList<NotificationData> old = new ArrayList<>();
                        for (NotificationData notification : notifications) {
                            if (now - formatTimeStamp(notification.getTimestamp()) > DEFAULT_NOTIFICATION_TTL) {
                                old.add(notification);
                            }
                        }

                        getCacheManager().deleteNotifications(old);
                        notificationsSubject.onNext(getCacheManager().getNotifications());

                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(scheduler)
                .lift(new TrackingOperator<Void>("del-old-notif", trackListener));
    }

    @Override
    public Observable<Void> deleteNotification(final NotificationData notification) {
        return Observable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ArrayList<NotificationData> deleted = new ArrayList<>();
                deleted.add(notification);
                getCacheManager().deleteNotifications(deleted);
                notificationsSubject.onNext(getCacheManager().getNotifications());
                return null;
            }
        })
                .subscribeOn(scheduler)
                .lift(new TrackingOperator<Void>("del-notif", trackListener));
    }

    @Override
    public Observable<NotificationModel> getNotificationModel(final NotificationData notificationData) {
        return RxUtils.observeLiveData(() -> articles2Repository.fetchData(
                new Query<>(notificationData.getStoryUrl(), new CacheOnlyQueryPolicy<>()),
                null,
                null))
                .map(status -> {
                    if (status instanceof Status.Network) {
                        Article2 content = ((Status.Network<? extends Article2>) status).getData();
                        notificationData.setImageUrl(content.getSocialImage());
                    }
                    if (status instanceof Status.Cache) {
                        Article2 content = ((Status.Cache<? extends Article2>) status).getData();
                        notificationData.setImageUrl(content.getSocialImage());
                    }
                    return notificationData;
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(scheduler)
                .flatMap(new Func1<NotificationData, Observable<NotificationModel>>() {
                    @Override
                    public Observable<NotificationModel> call(NotificationData notificationData) {
                        return Observable.just(new NotificationModel(
                                notificationData.getHeadline() == null ? "" : notificationData.getHeadline(),
                                new Date(formatTimeStamp(notificationData.getTimestamp())),
                                notificationData.getStoryUrl() == null ? "" : notificationData.getStoryUrl(),
                                notificationData,
                                notificationData.getImageUrl() == null ? "" : notificationData.getImageUrl()
                        ));
                    }
                })
                .onErrorReturn(new Func1<Throwable, NotificationModel>() {
                    @Override
                    public NotificationModel call(Throwable throwable) {
                        CrashWrapper.sendException(throwable);
                        return null;
                    }
                })
                .lift(new TrackingOperator<NotificationModel>("get-notif-model", trackListener));
    }

    /**
     * This method will always make a fresh network request get the notification article
     * The Request object used has a high timeout of 10 seconds
     * @param url
     * @return
     */
    private Observable<Article2> getNotificationArticleByUrl(final String url) {
       return RxUtils.observeLiveData(() -> {
           BypassCacheQueryPolicy<Article2> queryPolicy = new BypassCacheQueryPolicy<>();
           queryPolicy.setTimeOut(5000);
           return articles2Repository.fetchData(
                   new Query<>(url, queryPolicy),
                   null,
                   null);
       })
               .flatMap((Func1<Status<? extends Article2>, Observable<Article2>>) RxUtils::flatten)
               .subscribeOn(AndroidSchedulers.mainThread());
    }

    protected Observable<FileMeta> getFileMetaByUrl(final String url) {
        return Observable.create(new Observable.OnSubscribe<FileMeta>() {
                    @Override
                    public void call(final Subscriber<? super FileMeta> subscriber) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }

                        if (!isWorkingThread()) {
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    call(subscriber);
                                }
                            });
                            return;
                        }


                        subscriber.onNext(getCacheManager().getFileMetaByUrl(url));
                        subscriber.onCompleted();
                    }
                });
    }

    private void execute(Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg == null) {
            return false;
        }

        switch (msg.what) {

            case MSG_CALLBACK_STATUS: {
                processCallbackStatus((ITaskStatusListener) msg.obj, msg.arg1, msg.arg2);
                return true;
            }

            case MSG_CALLBACK_PROGRESS: {
                processCallbackProgress((ProgressTaskListener) msg.obj, msg.arg1);
                return true;
            }
            default:
                return false;
        }
    }

    private void processCallbackSectionComplete(UpdateSfTaskListener listener, String section) {
        if (!callbacksMap.containsKey(listener)) {
            return;
        }

        listener.onSectionComplete(section);
    }

    private void processCallbackProgress(ProgressTaskListener listener, int progress) {
        if (!callbacksMap.containsKey(listener)) {
            return;
        }

        listener.onProgress(progress);
    }

    private void processCallbackError(ITaskStatusListener listener, Throwable e) {
        if (!callbacksMap.containsKey(listener)) {
            return;
        }

        listener.onTaskError(e);

        callbacksMap.remove(listener);
    }

    private void processCallbackStatus(ITaskStatusListener listener, int status, int arg) {
        if (!callbacksMap.containsKey(listener)) {
            return;
        }

        listener.onTaskStatusChanged(status);

        if (status == Task.STATUS_COMPLETE) {
            callbacksMap.remove(listener);
        }
    }

    private boolean isWorkingThread() {
        return Thread.currentThread().getId() == workingThreadId;
    }

    private void fetchNotificationDataAsync(final NotificationData notificationData, PushUpdaterCallback pushUpdaterCallback) {
        getNotificationArticleByUrl(notificationData.getStoryUrl())
                .observeOn(scheduler)
                .onErrorReturn(new Func1<Throwable, Article2>() {
                    @Override
                    public Article2 call(Throwable throwable) {
                        return null;
                    }
                })
                .take(1)
                .lift(new TrackingOperator<Article2>("fetch-notif-data", trackListener))
                .subscribe(new Subscriber<Article2>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (notificationData != null && notificationData.getStoryUrl() != null) {
                            pushUpdaterCallback.onArticleLoadError(notificationData.getStoryUrl(), e);
                        }
                    }

                    @Override
                    public void onNext(Article2 nativeContent) {
                        if (nativeContent == null || nativeContent.getItems() == null) {
                            // nativeContent will be null in 415 and error cases.
                            pushUpdaterCallback.onArticleLoadError(notificationData.getStoryUrl(), null);
                        }
                    }
                });
    }

    private final ConfigManager.ConfigFailureListener configFailureListener = (type, e) -> {
        remoteLogRepo.e(new EventLog.Builder()
                .setModule(LogModules.CONFIG)
                .setMessage("Failed to load remote config: " + type.name())
                .setErrorMessage(e.getMessage())
                .build()
        );
    };

    public Observable<ConfigSyncOpInfo> updateConfigs() {
        return Observable.zip(
                ConfigSyncObservableFactory.INSTANCE.createConfigSyncObservable(context, Constants.ConfigType.SECTIONS_BAR_CONFIG, ConfigManager.instance(), configFailureListener),
                ConfigSyncObservableFactory.INSTANCE.createConfigSyncObservable(context, Constants.ConfigType.SECTIONS_FEATURED_CONFIG, ConfigManager.instance(), configFailureListener),
                ConfigSyncObservableFactory.INSTANCE.createConfigSyncObservable(context, Constants.ConfigType.SECTIONS_AZ_CONFIG, ConfigManager.instance(), configFailureListener),
                (configSyncOpInfo, configSyncOpInfo1, configSyncOpInfo2) -> new ConfigSyncOpInfo() // combine into a single Rx event
        )
                .doOnNext(configSyncOpInfo -> fetchSections());
    }

    public Observable<SyncOpInfo> performPagesSync(final List<String> sections) {
        Observable<ConfigSyncOpInfo> configUpdate = updateConfigs();
        //
        // turn list of names into observable
        Observable<List<String>> sectionNames;
        if (sections == null) {
            sectionNames = getDefaultPagesToSync();
        } else {
            sectionNames = Observable.just(sections);
        }
        //
        // log list of pages to sync, if needed
        if (D) {
            sectionNames = sectionNames.doOnNext(new Action1<List<String>>() {
                @Override
                public void call(List<String> names) {
                    Logger.d(DTAG, "updating sections: " + Arrays.toString(names.toArray()));
                }
            });
        }

        //
        // turn list of pages into combined page sync result observable
        Observable<SyncOpInfo> getPagesObservable = sectionNames.flatMap(new Func1<List<String>, Observable<SyncOpInfo>>() {
                    @Override
                    public Observable<SyncOpInfo> call(List<String> sectionNames) {
                        List<Observable<SyncOpInfo>> obs = new ArrayList<>(sectionNames.size());
                        for (String section : sectionNames) {
                            obs.add(
                                    updatePage(section, false, Priority.Group.BACKGROUND)
                                            .map(new Func1<PageLayout, SyncOpInfo>() {
                                                @Override
                                                public SyncOpInfo call(PageLayout page) {
                                                    return new SectionUpdateOpInfo(page);
                                                }
                                            })
                                            .onErrorResumeNext(Observable.<SyncOpInfo>empty())
                            );
                        }

                        return Observable.merge(obs);
                    }
                })
                .onErrorResumeNext(Observable.<SyncOpInfo>empty());

        Observable<SyncOpInfo> mostReadFeed = getMostReadFeed()
                .map((Func1<MostReadFeed, SyncOpInfo>) mostReadFeed1 -> new MostReadFeedSyncOpInfo())
                .onErrorResumeNext(Observable.empty());

        return Observable.concat(configUpdate, getPagesObservable, cleanupObservable(), updateAppWidgets(), mostReadFeed);
    }

    @NotNull
    private Observable<List<String>> getDefaultPagesToSync() {
        return getPages().take(1).map(sl -> {
            List<String> sectionNames = new ArrayList<>();
            if (sl.size() > 0) {
                sectionNames.add(sl.get(0).getBundleName());
            }
            return sectionNames;
        });
    }

    public Observable<SyncOpInfo> cleanupObservable() {

        return Observable.create(new Observable.OnSubscribe<SyncOpInfo>() {
            @Override
            public void call(final Subscriber<? super SyncOpInfo> subscriber) {
                if (!isWorkingThread()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            call(subscriber);
                        }
                    });
                    return;
                }

                try {
                    cacheManager.cleanUp();
                    if (articles2Repository != null)
                        articles2Repository.cleanUp();
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(new CleanupOpInfo());
                        subscriber.onCompleted();
                    }
                } catch (Throwable t) {
                    subscriber.onError(t);
                }


            }
        });
    }


    public Observable<SyncOpInfo> updateAppWidgets() {
        return Observable.create(subscriber -> {
            // Notify AppWidgets to refresh content.
            WidgetUtils.notifyAppWidgets(context);

            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(new AppWidgetsSyncOpInfo());
                subscriber.onCompleted();
            }
        });
    }

    private Long formatTimeStamp(String timeStamp) {
        if (timeStamp != null) {
            return Long.parseLong(timeStamp) * 1000;
        }
        System.out.println("notification timestamp failed.");
        return System.currentTimeMillis();
    }

    public void cancelListener(ITaskStatusListener listener) {
        callbacksMap.remove(listener);
    }

    protected ListenerWrapper getWrapper(ITaskStatusListener listener) {
        ListenerWrapper listenerWrapper = callbacksMap.get(listener);
        if (listenerWrapper == null) {
            listenerWrapper = new ListenerWrapper(listener);
            callbacksMap.put(listener, listenerWrapper);
        }

        return listenerWrapper;
    }

    private boolean isPhone() {
        return getContext().getResources().getBoolean(R.bool.is_phone);
    }

    public boolean isFirstSection(String bundleName) {
        Section firstSection = getFirstSection();
        return firstSection != null && firstSection.getBundleName().equals(bundleName);
    }

    @Nullable
    public Section getFirstSection() {
        if (sectionsSubject.hasValue()) {
            List<Section> sections = sectionsSubject.getValue();
            if (sections != null && sections.size() > 0) {
                return sections.get(0);
            }
        }
        return null;
    }

    /**
     * @return ab observable which provides a list of sections. onNext will be called on a background thread
     */
    public Observable<List<Section>> getPages() {
        fetchSections();
        return sectionsSubject;
    }

    @Override
    public Observable<List<Section>> getPages(@Nullable String pageId) {
        if (!sectionsSubject.hasValue()) {
            fetchSections();
        }

        return sectionsSubject
                .subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<List<Section>, Observable<? extends List<Section>>>() {
                    @Override
                    public Observable<? extends List<Section>> call(List<Section> sections) {
                        boolean pageFound = false;
                        List<Section> list = new ArrayList<>();
                        if (sections != null && !sections.isEmpty()) {
                            for (Section section : sections) {
                                if (TextUtils.isEmpty(pageId) || pageId.equals(section.getId())) {
                                    Logger.d(DTAG, "Found page section " + pageId);
                                    list.add(section);
                                    list.addAll(section.getChildSections());
                                    pageFound = true;
                                    break;
                                }
                            }
                            if (!pageFound) {
                                // pageName can be in children then.
                                for (Section section : sections) {
                                    for (Section childSection : section.getChildSections()) {
                                        if (pageId.equals(childSection.getId())) {
                                            list.add(childSection);
                                            pageFound = true;
                                            retrySections = 0;
                                            break;
                                        }
                                    }
                                    if (pageFound) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (!pageFound) {
                            Logger.d(DTAG, "page section not found!" + pageId);
                            if (retrySections < 2) {
                                retrySections += 1;
                                fetchSections();
                            }
                        }
                        return Observable.just(list);
                    }
                });
    }

    public void fetchSections() {
        Observable.create(new Observable.OnSubscribe<List<Section>>() {
            @Override
            public void call(final Subscriber<? super List<Section>> subscriber) {
                if (!isWorkingThread()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            call(subscriber);
                        }
                    });
                    return;
                }

                try {
                    List<Section> sections = SiteServiceConfigUtils.getSections(context, wapoConfigManager);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(sections);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }

            }
        })
                .subscribe(new Subscriber<List<Section>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<Section> sections) {
                        sectionsSubject.onNext(sections);
                    }
                });
    }

    private CacheManager getCacheManager() {
        return cacheManager;
    }

    private RequestQueue getRequestQueue() {
        return requestQueue;
    }

    private Context getContext() {
        return context;
    }

    public Observable<String> getPageUrlObs(final String correctPageName) {
        return configObs
                .take(1)
                .map(config -> {
                    if (AppContextUtils.INSTANCE.isDebuggableBuild()) {
                        // for both pb and fusion fronts.
                        if (URLUtil.isValidUrl(correctPageName)) {
                            //to allow us to use a url as a path to test any page from any jsonapp environment
                            return correctPageName;
                        }
                    }

                    // Allow custom recipe or bottom tab URL
                    if (isRecipePage(correctPageName) || isBottomTabPage(correctPageName)) {
                        return correctPageName;
                    }

                    switch (getSectionSubtype(correctPageName)) {
                        case FUSION:
                            String fusionBaseUrl;
                            Boolean isHomepage = correctPageName.equals("/.");
                            if (isLowDataModeEnable && isHomepage) {
                                fusionBaseUrl = config.getLowDataModeUrl();
                            } else {
                                fusionBaseUrl = config.getFusionUrl(correctPageName);
                            }
                            return fusionBaseUrl;
                        default: return null;
                    }
                });
    }

    public SectionSubtype getSectionSubtype(String pageName) {
        if (isRecipePage(pageName) || isListenBottomTabPage(pageName) || isGamesBottomTabPage(pageName)) {
            // Recipes, Listen, and Games are special cases: they are fusion fronts but are not in the site service config
            return SectionSubtype.FUSION;
        }
        return SectionSubtype.FUSION;
    }

    @NotNull
    @Override
    public Observable<VoteGuide> getVoteGuide() {
        return configObs
                .asObservable()
                .take(1)
                .map(Config::getVoterGuideUrl)
                .flatMap((Func1<String, Observable<VoteGuide>>) url -> Observable.create(subscriber -> {
                    Response.Listener<VoteGuide> listener = response -> {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    };
                    Response.ErrorListener errorListener = subscriber::onError;
                    VoteRequest voteRequest = new VoteRequest(url, listener, errorListener);
                    requestQueue.add(voteRequest);
                }))
                .onErrorResumeNext(Observable.fromCallable(new Callable<VoteGuide>() {
                    @Override
                    public VoteGuide call() throws Exception {
                        return VoteGuideApi.fromResources(context, R.raw.voter_guide, new Gson());
                    }
                }));
    }

    @Override
    public void setSubNavElectionLoadTime() {
        contentUpdateRulesManager.setTime(ContentUpdateRulesManager.TimeType.ELECTION_LOAD_TIME,ELECTION_TIME , System.currentTimeMillis());
    }

    @Override
    public long getSubNavElectionLoadTime() {
        return contentUpdateRulesManager.getTime(ContentUpdateRulesManager.TimeType.ELECTION_LOAD_TIME,ELECTION_TIME);
    }

    protected class ListenerWrapper implements UpdateSfTaskListener {
        private ITaskStatusListener mListener;

        public ListenerWrapper(ITaskStatusListener mListener) {
            this.mListener = mListener;
        }

        @Override
        public void onSectionComplete(final String section) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    processCallbackSectionComplete((UpdateSfTaskListener) mListener, section);
                }
            });
        }

        @Override
        public void onProgress(int progress) {
            mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(MSG_CALLBACK_PROGRESS, progress, -1, mListener));
        }

        @Override
        public void onTaskStatusChanged(int status) {
            mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(MSG_CALLBACK_STATUS, status, -1, mListener));
        }

        @Override
        public void onTaskError(final Throwable e) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    processCallbackError(mListener, e);
                }
            });
        }
    }

    public Observable<Map<Date, ComicStrip>> getSpecComicsListObservable(final String comicID) {

        return getComicsServiceObs()
                .flatMap(new Func1<ComicsService, Observable<Map<Date, ComicStrip>>>() {
                    @Override
                    public Observable<Map<Date, ComicStrip>> call(ComicsService comicsService) {
                        Calendar today = Calendar.getInstance();
                        Calendar aWeekAgo = Calendar.getInstance();
                        aWeekAgo.add(Calendar.DATE, -7);
                        return comicsService.getSpecificComic(comicID, aWeekAgo.getTime(), today.getTime());
                    }
                });
    }

    public Observable<List<MenuSection>> getNavSections() {
        return Observable.create(new Observable.OnSubscribe<List<MenuSection>>() {
            @Override
            public void call(final Subscriber<? super List<MenuSection>> subscriber) {
                if (!isWorkingThread()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            call(subscriber);
                        }
                    });
                    return;
                }

                try {
                    SiteServiceConfig siteServiceConfig = wapoConfigManager.getSectionsBarConfig();
                    List<MenuSection> menuSections = siteServiceConfig.getSectionsAsMenuSections(siteServiceConfig.getSections(), UIUtil.isPhone(context), true, false);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(menuSections);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    public Observable<List<MenuSection>> getFeaturedSections() {
        return Observable.create(new Observable.OnSubscribe<List<MenuSection>>() {
            @Override
            public void call(final Subscriber<? super List<MenuSection>> subscriber) {
                if (!isWorkingThread()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            call(subscriber);
                        }
                    });
                    return;
                }

                try {
                    SiteServiceConfig siteServiceConfig = wapoConfigManager.getSectionsFeaturedConfig();
                    List<MenuSection> menuSections = siteServiceConfig.getSectionsAsMenuSections(siteServiceConfig.getSections(), UIUtil.isPhone(context), true, false);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(menuSections);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    public Observable<List<MenuSection>> getAZSections() {
        return Observable.create(new Observable.OnSubscribe<List<MenuSection>>() {
            @Override
            public void call(final Subscriber<? super List<MenuSection>> subscriber) {
                if (!isWorkingThread()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            call(subscriber);
                        }
                    });
                    return;
                }

                try {
                    SiteServiceConfig siteServiceConfig = wapoConfigManager.getSectionsAZConfig();
                    List<MenuSection> menuSections = siteServiceConfig.getSectionsAsMenuSections(siteServiceConfig.getSections(), UIUtil.isPhone(context), true, false);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(menuSections);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    public Observable<List<MenuSection>>
    getAllMenuSections() {
        return Observable.create(new Observable.OnSubscribe<List<MenuSection>>() {
            @Override
            public void call(final Subscriber<? super List<MenuSection>> subscriber) {
                if (!isWorkingThread()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            call(subscriber);
                        }
                    });
                    return;
                }

                try {
                    SiteServiceConfig barSiteServiceConfig = wapoConfigManager.getSectionsBarConfig();
                    List<MenuSection> menuSections = barSiteServiceConfig.getSectionsAsMenuSections(barSiteServiceConfig.getSections(), UIUtil.isPhone(context), true, false);

                    SiteServiceConfig featuredSiteServiceConfig = wapoConfigManager.getSectionsFeaturedConfig();
                    menuSections.addAll(featuredSiteServiceConfig.getSectionsAsMenuSections(featuredSiteServiceConfig.getSections(), UIUtil.isPhone(context), true, false));

                    SiteServiceConfig azSiteServiceConfig = wapoConfigManager.getSectionsAZConfig();
                    menuSections.addAll(azSiteServiceConfig.getSectionsAsMenuSections(azSiteServiceConfig.getSections(), UIUtil.isPhone(context), true, false));

                    SiteServiceConfig unlistedSiteServiceConfig = wapoConfigManager.getSectionsUnlistedConfig();
                    if (unlistedSiteServiceConfig != null) {
                        List<MenuSection> unlistedSections = unlistedSiteServiceConfig.getSectionsAsMenuSections(unlistedSiteServiceConfig.getSections(), UIUtil.isPhone(context), true, true);
                        for (MenuSection section : unlistedSections) {
                            if (menuSections.contains(section)) {
                                //section appears in listed and unlisted config, remove from unlisted sections
                                section.setUnlisted(false);
                            }
                        }
                        menuSections.addAll(unlistedSections);
                    }
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(menuSections);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }

            }
        });
    }

    public SiteServiceConfig getSectionsBarConfig() {
        return wapoConfigManager.getSectionsBarConfig();
    }

    @NonNull
    public Observable<List<ComicStrip>> getComicsList() {
        updateComicsList();
        return comicsSubject.asObservable();
    }

    public static List<MenuSection> mapComicsToSection(String displayName, String loadingMessage, List<ComicStrip> comics) {
        List<MenuSection> list = new ArrayList<>();
        if (comics == null || comics.isEmpty()) {
            createLoadingStubSection(list, displayName, loadingMessage);
            return list;
        }

        List<MenuSection> comicsMenuList = new ArrayList<>(comics.size());
        for (ComicStrip comicStrip : comics) {
            MenuSection menuSection = new MenuSection(
                    comicStrip.getName(),
                    comicStrip.getName(),
                    MenuSection.COMICS_TYPE,
                    "",
                    "",
                    null,
                    3,
                    null,
                    false);
            comicsMenuList.add(menuSection);
        }

        Collections.sort(comicsMenuList, new Comparator<MenuSection>() {
            final String PREFIX = "The ";

            @Override
            public int compare(MenuSection ms1, MenuSection ms2) {
                String left = ms1.getDisplayName();
                String right = ms2.getDisplayName();
                if (left.startsWith(PREFIX)) {
                    left = left.substring(PREFIX.length());
                }
                if (right.startsWith(PREFIX)) {
                    right = right.substring(PREFIX.length());
                }
                return left.compareTo(right);
            }
        });

        MenuSection[] comicsMenuArray = comicsMenuList.toArray(new MenuSection[comicsMenuList.size()]);
        MenuSection menuSection = new MenuSection(
                displayName,
                displayName,
                MenuSection.LABEL_TYPE,
                null,
                null,
                comicsMenuArray,
                3,
                null,
                false);
        list.add(menuSection);
        return list;
    }

    private static void createLoadingStubSection(List<MenuSection> list, String displayName, String loadingMessage) {
        MenuSection menuSection = new MenuSection(
                displayName,
                displayName,
                MenuSection.LABEL_TYPE,
                null,
                null,
                new MenuSection[]{
                        new MenuSection(
                                "",
                                loadingMessage,
                                MenuSection.LABEL_TYPE,
                                null,
                                null,
                                null,
                                3,
                                null,
                                false)
                },
                3,
                null,
                false);
        list.add(menuSection);
    }

    private void updateComicsList() {
        getComicsServiceObs()
                .flatMap(new Func1<ComicsService, Observable<Map<Date, List<ComicStrip>>>>() {
                    @Override
                    public Observable<Map<Date, List<ComicStrip>>> call(ComicsService comicsService) {
                        return comicsService.getTodayComicsObservable();
                    }
                })
                .take(1)
                .map(new Func1<Map<Date, List<ComicStrip>>, List<ComicStrip>>() {
                    @Override
                    public List<ComicStrip> call(Map<Date, List<ComicStrip>> dateListMap) {
                        List<ComicStrip> comicStrips = null;
                        Date maxDate = new Date(0);
                        for (Map.Entry<Date, List<ComicStrip>> entry: dateListMap.entrySet()) {
                            Date date = entry.getKey();
                            if (date.after(maxDate)) {
                                maxDate = date;
                                comicStrips = entry.getValue();
                            }
                            EventLog eventLog = new EventLog.Builder()
                                    .setMessage("Comics List Loaded")
                                    .setModule(LogModules.COMICS)
                                    .set("date", date)
                                    .set("items_size", (entry.getValue() != null) ? entry.getValue().size() : null)
                                    .build();
                            if (entry.getValue() == null || entry.getValue().isEmpty())
                                RemoteLog.e(context, eventLog);
                            else
                                RemoteLog.d(context, eventLog);
                        }
                        return comicStrips;
                    }
                })
                .doOnNext(new Action1<List<ComicStrip>>() {
                    @Override
                    public void call(List<ComicStrip> comicStrips) {
                        comicsSubject.onNext(comicStrips);
                    }
                })
                .onErrorReturn(new Func1<Throwable, List<ComicStrip>>() {
                    @Override
                    public List<ComicStrip> call(Throwable throwable) {
                        if (D) { Logger.d(DTAG, "Error while getting today comics", throwable); }
                        RemoteLog.e(context, new EventLog.Builder()
                                .setMessage("Comics List Load Error")
                                .setModule(LogModules.COMICS)
                                .setErrorMessage(throwable.getMessage())
                                .set("cause", throwable.getCause())
                                .build());
                        return new ArrayList<>();
                    }
                })
                .lift(new TrackingOperator<List<ComicStrip>>(COMICS_TRACKING_TAG, trackListener))
                .subscribe();
    }


    public BehaviorSubject<MostReadFeed> getMostReadFeedSubj() {
        return mostReadFeedSubj;
    }

    public Observable<MostReadFeed> getMostReadFeed() {
        return configObs
                .take(1)
                .flatMap((Func1<Config, Observable<MostReadFeed>>) config ->
                        Observable.create(subscriber -> {
                            Response.Listener<MostReadFeed> listener = response -> {
                                subscriber.onNext(response);
                                subscriber.onCompleted();
                                mostReadFeedSubj.onNext(response);
                            };
                            Response.ErrorListener errorListener = throwable -> {
                                subscriber.onError(throwable);
                                mostReadFeedSubj.onError(throwable);
                            };
                            MostReadFeedRequest request = new MostReadFeedRequest(config.getMostReadFeedURL(), listener, errorListener);
                            getRequestQueue().add(request);
                        }));
    }


    /**
     * This observable can be used to wait until initial sections are loaded.
     * Mainly to skip loading anything before loading top stories.
     * @return Observable<Boolean>
     */
    public Observable<Boolean> canMakeApiCalls() {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed()) {
                return;
            }
            sectionsSubject
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .take(1)
                    .subscribe(
                            sections -> {
                                if (!sections.isEmpty()) {
                                    listenToPage(sections.get(0).getBundleName(), false)
                                            .take(1)
                                            .subscribe(
                                                    pageLayout -> {
                                                        subscriber.onNext(true);
                                                        subscriber.onCompleted();
                                                    },
                                                    throwable -> {
                                                        subscriber.onNext(true);
                                                        subscriber.onCompleted();
                                                    });
                                } else {
                                    subscriber.onNext(true);
                                    subscriber.onCompleted();
                                }
                            },
                            throwable -> {
                                subscriber.onNext(true);
                                subscriber.onCompleted();
                            });
        });
    }

    public WapoConfigManager getWapoConfigManager() {
        return wapoConfigManager;
    }

    private boolean isRecipePage(String pageName) {
        RecipesConfig recipesConfig = getCurrentConfig().getRecipesConfig();
        return recipesConfig != null &&
                pageName != null &&
                pageName.equals(recipesConfig.getLandingPage());
    }

    private boolean isBottomTabPage(String pageName) {
        return isListenBottomTabPage(pageName) || isGamesBottomTabPage(pageName);
    }

    private boolean isListenBottomTabPage(String pageName) {
        return pageName != null && pageName.equals(getCurrentConfig().getListenContentUrl());
    }

    private boolean isGamesBottomTabPage(String pageName) {
        return pageName != null && pageName.equals(getCurrentConfig().getGamesContentUrl());
    }

    private List<QuestionItem> getAskThePostQuestionsFromConfig(Search2Config search2Config) {
        ArrayList<QuestionItem> questionItems = new ArrayList<>();
        if (search2Config != null && search2Config.getAskThePost() != null && search2Config.getAskThePost().getQuestions() != null) {
            List<AskThePostQuestion> questions = search2Config.getAskThePost().getQuestions();
            for (int i = 0; i < questions.size(); i++) {
                questionItems.add(new QuestionItem(questions.get(i).getText(), questions.get(i).getUuid(), questions.get(i).getTopicId()));
            }
        }
        return questionItems;
    }

    private List<CategoryItem> getAskThePostCategoriesFromConfig(Search2Config search2Config) {
        ArrayList<CategoryItem> categoryItems = new ArrayList<>();
        if (search2Config != null && search2Config.getAskThePost() != null && search2Config.getAskThePost().getCategories() != null) {
            List<AskThePostCategory> categories = search2Config.getAskThePost().getCategories();
            for (int i = 0; i < categories.size(); i++) {
                categoryItems.add(new CategoryItem(categories.get(i).getId(), categories.get(i).getName(), getAskThePostQuestionsFromCategory(categories.get(i).getQuestions())));
            }
        }
        return categoryItems;
    }

    private List<QuestionItem> getAskThePostQuestionsFromCategory(List<AskThePostQuestion> questions){
        ArrayList<QuestionItem> questionItems = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            questionItems.add(new QuestionItem(questions.get(i).getText(),questions.get(i).getUuid(), questions.get(i).getTopicId()));
        }
        return questionItems;
    }

    public static abstract class SyncOpInfo {
    }

    public static class SectionUpdateOpInfo extends SyncOpInfo {
        private final PageLayout page;

        private SectionUpdateOpInfo(PageLayout page) {
            this.page = page;
        }

        public PageLayout getPage() {
            return page;
        }
    }

    public static class CleanupOpInfo extends SyncOpInfo {
        private CleanupOpInfo() {
        }
    }

    public static class MostReadFeedSyncOpInfo extends SyncOpInfo { }

    public static class ConfigSyncOpInfo extends SyncOpInfo { }

    public static class AppWidgetsSyncOpInfo extends SyncOpInfo { }

    public class PageInfo {
        private final String pageName;
        private final String pageUrl;
        private final PageLayout page;

        PageInfo(String pageName, String pageUrl, PageLayout page) {
            this.pageName = pageName;
            this.pageUrl = pageUrl;
            this.page = page;
        }

        public String getPageName() {
            return pageName;
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public PageLayout getPage() {
            return page;
        }
    }

    private static class PageManagerFourFifteenException extends FourFifteenException {
        private final String contentUrl;

        public PageManagerFourFifteenException(String contentUrl) {
            this.contentUrl = contentUrl;
        }

        @Override
        public String getContentUrl() {
            return contentUrl;
        }
    }

    private enum Platform {
        TABLET("tablet"),
        PHONE("phone");

        private final String value;

        Platform(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
