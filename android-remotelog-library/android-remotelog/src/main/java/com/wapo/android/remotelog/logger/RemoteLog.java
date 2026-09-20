/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.StringDef;

import android.preference.PreferenceManager;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.util.Utils;
import com.wapo.android.domain.repository.RemoteLogRepo;
import com.washingtonpost.android.config.domain.models.config.LoggerConfig;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RemoteLog {
    private static final String TAG = "RemoteLog";

    protected static final String LOGGER_FOLDER = "wp-remote-logger";
    protected static final String COMPRESSED_LOG_FOLDER = "compressed";

    private static RemoteLog instance = null;
    private static volatile RemoteLogRepo activeRepo = null;
    // for DI injected RemoteLogRepo
    public static final RemoteLogRepo repo = new RemoteLogRepo() {
        @Override
        public void d(EventLog eventLog) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.d(eventLog);
            }
        }

        @Override
        public void d(EventLog eventLog, String process) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.d(eventLog, process);
            }
        }

        @Override
        public void d(EventLog eventLog, String process, String paywallInfo) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.d(eventLog, process, paywallInfo);
            }
        }

        @Override
        public void e(EventLog eventLog) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.e(eventLog);
            }
        }

        @Override
        public void e(EventLog eventLog, String process) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.e(eventLog, process);
            }
        }

        @Override
        public void e(EventLog eventLog, String process, String paywallInfo) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.e(eventLog, process, paywallInfo);
            }
        }

        @Override
        public void w(EventLog eventLog) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.w(eventLog);
            }
        }

        @Override
        public void p(EventLog eventLog) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.p(eventLog);
            }
        }

        @Override
        public void p(EventLog eventLog, String process) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.p(eventLog, process);
            }
        }

        @Override
        public void p(EventLog eventLog, String process, String paywallInfo) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.p(eventLog, process, paywallInfo);
            }
        }

        @Override
        public void v(EventLog eventLog) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.v(eventLog);
            }
        }

        @Override
        public void v(EventLog eventLog, String process) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.v(eventLog, process);
            }
        }

        @Override
        public void v(EventLog eventLog, String process, String paywallInfo) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.v(eventLog, process, paywallInfo);
            }
        }

        @Override
        public void m(EventLog eventLog) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.m(eventLog);
            }
        }

        @Override
        public void m(EventLog eventLog, String process) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.m(eventLog, process);
            }
        }

        @Override
        public void m(EventLog eventLog, String process, String paywallInfo) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.m(eventLog, process, paywallInfo);
            }
        }

        @Override
        public void uploadLogFiles(Bundle bundle) {
            RemoteLogRepo remoteLogRepo = activeRepo;
            if (remoteLogRepo != null) {
                remoteLogRepo.uploadLogFiles(bundle);
            }
        }

    };
    private LogFileUploader.UploadHelper uploadHelper;
    protected LoggerConfig config;
    private LogThreadHandler logThreadHandler;
    private RemoteLogProvider remoteLogProvider = new DefaultRemoteLogProviderImpl();
    private static int VERSION_CODES_O = 26;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SYNCER, PUSH, PAYWALL_ERROR, PAYWALL_WARNING, PAYWALL_DEBUG, PAYWALL_SHOWN,
            APP_ERROR, BUNDLE_PROCESSING, ARTICLE_RENDER, FIRST_LAUNCH, CAROUSEL_UPDATE, FILE_STATS, METRICS, TEXT_TO_SPEECH, CONTACT_US, BACKEND_ERROR})

    public @interface Process {
    }

    public static final String SYNCER = "syncer";
    public static final String PUSH = "push";
    public static final String PAYWALL_ERROR = "paywall_error";
    public static final String PAYWALL_WARNING = "paywall_warning";
    public static final String PAYWALL_DEBUG = "paywall_debug";
    public static final String PAYWALL_SHOWN = "paywall_shown";
    public static final String APP_ERROR = "app_error";
    public static final String BUNDLE_PROCESSING = "bundle_processing";  //This is not used anymore (Old syncer)
    public static final String ARTICLE_RENDER = "article_render";
    public static final String FIRST_LAUNCH = "first_launch";
    public static final String CAROUSEL_UPDATE = "carousel_update";
    public static final String FILE_STATS = "file_stats";
    public static final String METRICS = "metrics";
    public static final String TEXT_TO_SPEECH = "text_to_speech";
    public static final String CONTACT_US = "contact_us";
    public static final String BACKEND_ERROR = "backend_error";
    private static final String PREF_HAS_MIGRATED_FROM_RAINBOW = "pref.HAS_MIGRATED_FROM_RAINBOW";
    private static final String PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW = "pref.PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW";
    private static final String PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW = "pref.PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW";
    private static String PREF_HAS_MIGRATED_FROM_AMAZON_CLASSIC = "pref.has_migrated_from_amazon_classic";
    private static final String PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC = "pref.PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC";
    private static final String PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC = "pref.PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC";
    private static final String PREF_MIGRATED_AMAZON_USER_HAS_DUPLICATE_SUB = "migrated_amazon_user_has_duplicate_sub";
    private static final String PREF_MIGRATED_AMAZON_USER_APPSTORE_MIGRATION_COMPLETE = "migrated_amazon_user_appstore_migration_complete";
    /**
     * For any generic non-fatal errors should have their own enums.
     */
    public enum NonFatalErrors{
        NonFatalNPE,
        InvalidState,
        ParseError
    }


    protected synchronized static RemoteLog getInstance() {
        if (instance == null) {
            instance = new RemoteLog();
        }
        return instance;
    }

    private RemoteLog() {
    }


    public static void initialize(LoggerConfig config, LogFileUploader.UploadHelper uploadHelper) {
        RemoteLog.getInstance().config = config;
        RemoteLog.getInstance().uploadHelper = uploadHelper;
        initFiles();
    }

    public static void initialize(LoggerConfig config, LogFileUploader.UploadHelper uploadHelper, RemoteLogProvider remoteLogProvider) {
        RemoteLog.getInstance().config = config;
        RemoteLog.getInstance().uploadHelper = uploadHelper;
        RemoteLog.getInstance().remoteLogProvider = remoteLogProvider;
        initFiles();
    }

    public static synchronized void initialize(
            Context appContext,
            LoggerConfig config,
            LogFileUploader.UploadHelper uploadHelper,
            RemoteLogProvider remoteLogProvider,
            String uploaderUrl
    ) {
        RemoteLog.getInstance().config = config;
        RemoteLog.getInstance().uploadHelper = uploadHelper;
        RemoteLog.getInstance().remoteLogProvider = remoteLogProvider;
        initFiles();

        // Repository selection is owned by KMP's LoggerRegistrar. Android's
        // legacy facade must not select a logger from uploaderUrl.
    }

    private static void initFiles() {
        File dir = new File(getConfig().getFilesDirectory(), LOGGER_FOLDER);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        File compressedDir = new File(dir, COMPRESSED_LOG_FOLDER);
        if (!compressedDir.isDirectory()) {
            compressedDir.mkdirs();
        }
    }

    public static LoggerConfig getConfig() {
        return RemoteLog.getInstance().config;
    }

    public static void setRepo(RemoteLogRepo remoteLogRepo) {
        activeRepo = remoteLogRepo;
    }

    /**
     * Write a {@link com.wapo.android.commons.logger.Level#DEBUG} log message in to the remote log file. Method will write log in
     * to the file only if the config properties are initialized properly and when the remote logging is active.
     *
     * @param eventLog     The message you would like logged.
     * @param context Application context
     */
    public static void d(Context context, EventLog eventLog) {
        d(context, eventLog, null);
    }

    //rainbow specific
    public static void d(Context context, EventLog eventLog, @Process String process) {
        d(context, eventLog, process, null);
    }

    public static void d(Context context, EventLog eventLog, @Process String process, String paywallInfo) {
        RemoteLogRepo remoteLogRepo = activeRepo;
        if (remoteLogRepo != null) {
            remoteLogRepo.d(eventLog, process, paywallInfo);
        } else {
            LoggerConfig config = getConfig();
            if (eventLog == null
                    || config == null
                    || !config.isLoggable()
                    || !config.isDebugLoggingActive()) {
                return;
            }
            writeMessageToFile(context, Level.DEBUG, eventLog, process, paywallInfo);
        }
    }

    /**
     * Write a {@link com.wapo.android.commons.logger.Level#ERROR} log message in to the remote log file. Method will write log
     * in to the file only if the config properties are initialized properly and when the remote logging is active.
     *
     * @param eventLog     The message you would like logged.
     * @param context Application context
     */
    public static void e(Context context, EventLog eventLog) {
        e(context, eventLog, null);
    }

    //rainbow specific
    public static void e(Context context, EventLog eventLog, @Process String process) {
        e(context, eventLog, process, null);
    }

    public static void e(Context context, EventLog eventLog, @Process String process, String paywallInfo) {
        RemoteLogRepo remoteLogRepo = activeRepo;
        if (remoteLogRepo != null) {
            remoteLogRepo.e(eventLog, process, paywallInfo);
        } else {
            LoggerConfig config = getConfig();
            if (eventLog == null
                    || config == null
                    || !config.isLoggable()
                    || !config.isErrorLoggingActive()) {
                return;
            }
            writeMessageToFile(context, Level.ERROR, eventLog, process, paywallInfo);
        }
    }


    /**
     * Write a {@link com.wapo.android.commons.logger.Level#WARNING} log message in to the remote log file. Method will write log
     * in to the file only if the config properties are initialized properly and when the remote logging is active.
     *
     * @param eventLog The message you would like logged.
     * @param context  Application context
     */
    public static void w(Context context, EventLog eventLog) {
        RemoteLogRepo remoteLogRepo = activeRepo;
        if (remoteLogRepo != null) {
            remoteLogRepo.w(eventLog);
        } else {
            LoggerConfig config = getConfig();
            if (eventLog == null
                    || config == null
                    || !config.isLoggable()) {
                return;
            }
            writeMessageToFile(context, Level.WARNING, eventLog, null, null);
        }
    }

    //Paywall logging
    public static void p(Context context, EventLog eventLog) {
        p(context, eventLog, null);
    }

    public static void p(Context context, EventLog eventLog, @Process String process) {
        p(context, eventLog, process, null);
    }

    public static void p(Context context, EventLog eventLog, @Process String process, String paywallInfo) {
        RemoteLogRepo remoteLogRepo = activeRepo;
        if (remoteLogRepo != null) {
            remoteLogRepo.p(eventLog, process, paywallInfo);
        } else {
            LoggerConfig config = getConfig();
            if (eventLog == null
                    || config == null
                    || !config.isLoggable()
                    || !config.isPaywallLoggingActive()) {
                return;
            }
            writeMessageToFile(context, Level.PAYWALL, eventLog, process, paywallInfo);
        }
    }

    //Verbose Logging
    public static void v(Context context, EventLog eventLog) {
        v(context, eventLog, null);
    }

    public static void v(Context context, EventLog eventLog, @Process String process) {
        v(context, eventLog, process, null);
    }

    public static void v(Context context, EventLog eventLog, @Process String process, String paywallInfo) {
        RemoteLogRepo remoteLogRepo = activeRepo;
        if (remoteLogRepo != null) {
            remoteLogRepo.v(eventLog, process, paywallInfo);
        } else {
            LoggerConfig config = getConfig();
            if (eventLog == null
                    || config == null
                    || !config.isLoggable()
                    || !config.isVerboseLoggingActive()) {
                return;
            }
            writeMessageToFile(context, Level.VERBOSE, eventLog, process, paywallInfo);
        }
    }

    //Metrics Logging
    public static void m(Context context, EventLog eventLog) {
        m(context, eventLog, null);
    }

    public static void m(Context context, EventLog eventLog, @Process String process) {
        m(context, eventLog, process, null);
    }

    public static void m(Context context, EventLog eventLog, @Process String process, String paywallInfo) {
        RemoteLogRepo remoteLogRepo = activeRepo;
        if (remoteLogRepo != null) {
            remoteLogRepo.m(eventLog, process, paywallInfo);
        } else {
            LoggerConfig config = getConfig();
            if (eventLog == null
                    || config == null
                    || !config.isLoggable()
                    || !config.isMetricsLoggingActive()
                    || !config.isSampledForMetrics()) {
                return;
            }
            writeMessageToFile(context, Level.METRICS, eventLog, process, paywallInfo);
        }
    }


    public synchronized static void writeMessageToFile(Context context, Level level, EventLog eventLog, String process, String paywallInfo) {
        Bundle bundle = new Bundle();

        bundle.putString(LogFileWriter.LOG_MESSAGE, eventLog.getDataString());
        bundle.putSerializable(LogFileWriter.INFO_LEVEL, level);
        bundle.putString(LogFileWriter.PROCESS, process);
        bundle.putString(LogFileWriter.PAYWALL_INFO, paywallInfo);
        bundle.putString(LogFileWriter.RAINBOW_MIGRATION_INFO, getRainbowMigrationInfoString(context));
        if (Utils.INSTANCE.isAmazonBuild()) {
            bundle.putString(LogFileWriter.AMAZON_CLASSIC_MIGRATION_INFO, getAmazonClassicMigrationString(context));
        }

        if (eventLog.isForceUpload()) {
            bundle.putBoolean(LogFileWriter.IS_FORCE_UPLOAD, true);
        }

        if (getInstance().logThreadHandler == null) {
            getInstance().logThreadHandler = new LogThreadHandler(context);
        }
        getInstance().logThreadHandler.sendWriteMessage(bundle);
    }

    /**
     * Creates and returns a string that holds relevant Rainbow migration information to log
     * @return a string that includes the following information regarding the user's migration status:
     * if the user has migrated from Rainbow,
     * if the user has migrated their account from Rainbow,
     * if the user has migrated an in app purchase subscription from Rainbow
     */
    private static String getRainbowMigrationInfoString(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String hasMigrated = sharedPreferences.getString(PREF_HAS_MIGRATED_FROM_RAINBOW, "na");
        String hasMigratedAccount = String.valueOf(sharedPreferences.getBoolean(PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW, false));
        String hasMigratedInAppSub = String.valueOf(sharedPreferences.getBoolean(PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW, false));
        return "has_migrated=\"" + hasMigrated + "\", has_migrated_account=\"" + hasMigratedAccount + "\", has_migrated_in_app_sub=\"" + hasMigratedInAppSub + "\", ";
    }

    /**
     * Creates and returns a string that holds relevant Amazon Classic migration information to log
     * @return a string that includes the following information regarding the user's migration status:
     * if the user has migrated from Amazon Classic,
     * if the user has migrated their account from Amazon Classic,
     * if the user has migrated an in app purchase subscription from Amazon Classic
     */
    private static String getAmazonClassicMigrationString(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences paywallSharedPreferences = context.getSharedPreferences("pw_prefs_name", Context.MODE_PRIVATE);
        String hasMigrated = sharedPreferences.getString(PREF_HAS_MIGRATED_FROM_AMAZON_CLASSIC, "na");
        String hasMigratedAccount = String.valueOf(sharedPreferences.getBoolean(PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC, false));
        String hasMigratedInAppSub = String.valueOf(sharedPreferences.getBoolean(PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC, false));
        String hasAppStoreMigratedInAppSub = String.valueOf(paywallSharedPreferences.getBoolean(PREF_MIGRATED_AMAZON_USER_APPSTORE_MIGRATION_COMPLETE, false));
        String hasDuplicateSub = String.valueOf(paywallSharedPreferences.getBoolean(PREF_MIGRATED_AMAZON_USER_HAS_DUPLICATE_SUB, false));
        return "has_migrated_amazon_classic=\"" + hasMigrated
                + "\", has_migrated_amazon_classic_account=\"" + hasMigratedAccount
                + "\", has_migrated_amazon_classic_in_app_sub=\"" + hasMigratedInAppSub
                + "\", has_appstore_migrated_in_app_sub=\"" + hasAppStoreMigratedInAppSub
                + "\", has_duplicate_sub=\"" + hasDuplicateSub
                + "\", ";
    }

    /**
     * Upload pending log files
     */
    public static void uploadLogFiles(Context context, Bundle bundle) {
        if (getInstance().logThreadHandler == null) {
            getInstance().logThreadHandler = new LogThreadHandler(context);
        }
        getInstance().logThreadHandler.sendUploadMessage(bundle);
    }

    public LogFileUploader.UploadHelper getUploadHelper() {
        return uploadHelper;
    }

    public RemoteLogProvider getRemoteLogProvider() {
        return remoteLogProvider;
    }

    public interface RemoteLogProvider {

        boolean isApplicationInForeground();

        String getJUcid();
    }

    public class DefaultRemoteLogProviderImpl implements RemoteLogProvider {

        @Override
        public boolean isApplicationInForeground() {
            return true;
        }

        @Override
        public String getJUcid() {
            return "";
        }
    }
}
