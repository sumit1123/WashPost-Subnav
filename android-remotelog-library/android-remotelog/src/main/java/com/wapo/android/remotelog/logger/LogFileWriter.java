/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */


package com.wapo.android.remotelog.logger;

import static com.wapo.android.commons.util.Utils.isConnectedOrConnecting;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.util.Compress;
import com.wapo.android.commons.util.DeviceUtils;
import com.wapo.android.commons.util.FileUtils;
import com.wapo.android.commons.util.ReachabilityUtil;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.Config;
import com.washingtonpost.android.config.domain.models.config.LoggerConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class LogFileWriter {

    private final static String COMPRESSED_FILE = "wp-log.zip";
    private final static String TAG = "LogFileWriter";

    private static final String INFO_OS = "os";
    private static final String INFO_APP_VERSION = "app_version";
    private static final String INFO_OS_VERSION = "os_version";
    private static final String INFO_DEVICE = "device";
    private static final String INFO_APP = "app";
    private static final String INFO_NETWORK = "network";
    private static final String ANDROID = "ANDROID";
    private static final String DISK_USED = "disk_usage";
    private static final String ARCHIVE_SIZE = "archive_size";
    public static final String INFO_TABLET = "tablet";
    private static final String INFO_LOGGING_ID = "logging_id";
    private static final String INFO_SERIAL_ID = "device_id";
    private static final String INFO_VPN_STATUS = "vpn";
    private static final String INFO_JUC_ID = "juc_id";

    protected final static String LOG_FILE_NAME = "wp-log.log";
    public static final String INFO_LEVEL = "level";
    public static final String LOG_MESSAGE = "LOG_MESSAGE";
    public static final String PROCESS = "process";
    public static final String PAYWALL_INFO = "paywallInfo";
    public static final String IS_FORCE_UPLOAD = "force_upload";
    public static final String RAINBOW_MIGRATION_INFO = "rainbow_migration_info";
    public static final String AMAZON_CLASSIC_MIGRATION_INFO = "amazon_classic_migration_info";
    public static final String CONFIG_VERSION = "config_version";


    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    private Config getAppConfig() {
        return ConfigManager.Companion.getInstance().getConfig();
    }

    private LoggerConfig getConfig() {
        return RemoteLog.getInstance().config;
    }

    protected void process(Bundle bundle) {
        if (bundle != null) {
            String messageToLog = bundle.getString(LOG_MESSAGE);
            Object object = bundle.getSerializable(INFO_LEVEL);
            String process = bundle.getString(PROCESS);
            String paywallInfo = bundle.getString(PAYWALL_INFO);
            boolean forceUpload = bundle.getBoolean(IS_FORCE_UPLOAD, false);
            String rainbowMigrationInfo = bundle.getString(RAINBOW_MIGRATION_INFO);
            String amazonClassicMigrationInfo = bundle.getString(AMAZON_CLASSIC_MIGRATION_INFO);

            // Setting a default value so that a message is always logged
            Level level = Level.DEBUG;
            if (object != null && (object instanceof Level)) {
                level = (Level) object;
            }

            this.writeToFile(messageToLog, level, process, paywallInfo, forceUpload, rainbowMigrationInfo, amazonClassicMigrationInfo);
        }
    }

    private void writeToFile(String logMessage, Level logLevel, String process, String paywallInfo, boolean forceUpload, String rainbowMigrationInfo, String amazonClassicMigrationInfo) {

        String filesDirectory = getConfig().getFilesDirectory();

        File logFile = new File(filesDirectory, RemoteLog.LOGGER_FOLDER + "/" + LogFileWriter.LOG_FILE_NAME);
        if (!logFile.isFile()) {
            try {
                logFile.createNewFile();
            } catch (Exception ex) {
                Logger.e(TAG, "Could not create log file!!");
                return;
            }
        }

        try {
            writeLineToFile(logFile, logMessage, logLevel, process, paywallInfo, rainbowMigrationInfo, amazonClassicMigrationInfo);

            if (logFile.length() >= getConfig().getFileSizeThreshold() || forceUpload) {
                Logger.d(TAG, "exceeded limit: " + logFile.length());
                writeDiskUsageAndCompress(logFile, process, paywallInfo, rainbowMigrationInfo, amazonClassicMigrationInfo);

                Bundle bundle = new Bundle();
                bundle.putBoolean(LogFileWriter.IS_FORCE_UPLOAD, forceUpload);
                RemoteLog.uploadLogFiles(getAppContext(), bundle);
            } else {
                Logger.d(TAG, "not yet: " + logFile.length());
            }
        } catch (Exception ex) {
            // not critical that we log every message. ok to drop if we get an exception.
            Logger.e(TAG, "Could not write to wp-log: " + ex.getMessage());
        }

        return;
    }

    private void writeDiskUsageAndCompress(File logFile, String process, String paywallInfo, String rainbowMigrationInfo, String amazonClassicMigrationInfo) {
        long bytes = FileUtils.dirSize(DeviceUtils.getAppDirectory(getAppContext()));
        double diskUsageInMb = bytes / (1024D * 1024D);
        String diskUsedToLog = LogFileWriter.DISK_USED + "=" + String.format("%.2f", diskUsageInMb) + ";";
        this.writeLineToFile(logFile, diskUsedToLog, Level.WARNING, process, paywallInfo, rainbowMigrationInfo, amazonClassicMigrationInfo);

        getConfig().getArchivesDirectory();
        double archiveBytes = FileUtils.getPrintEditionSizeInDisk(getAppContext(), getConfig().getArchivesDirectory());
        String archiveSize = LogFileWriter.ARCHIVE_SIZE + "=" + String.format("%.2f", archiveBytes) + ";";
        this.writeLineToFile(logFile, archiveSize, Level.WARNING, "disk-usage", paywallInfo, rainbowMigrationInfo, amazonClassicMigrationInfo);

        String dir = getConfig().getFilesDirectory() + File.separator + RemoteLog.LOGGER_FOLDER + File.separator + RemoteLog.COMPRESSED_LOG_FOLDER;
        String zipFilePath = dir + File.separator + Build.DEVICE + "_" + System.nanoTime() + COMPRESSED_FILE;
        try {
            Logger.d(TAG, "compressed: " + logFile.length());
            new Compress().gZipFile(logFile.getCanonicalPath(), zipFilePath);

            File zipFile = new File(zipFilePath);
            logFile.delete();
            logFile.createNewFile();
            Logger.d(TAG, "after compression: " + zipFile.length());
        } catch (IOException ex) {
            // that's fine. we will try next time.
            Logger.e(TAG, "Could not compress log file.  Log will keep growing in the meantime.");
        }
    }

    private void writeLineToFile(File file, String line, Level level, String process, String paywallInfo, String rainbowMigrationInfo, String amazonClassicMigrationInfo) {
        StringBuilder builder = new StringBuilder(getDate())
                .append(this.getDeviceInfoString())
                .append(getNetworkInfoLog()).append(getLevelInfo(level))
                .append(getProcessInfo(process))
                .append(paywallInfo == null ? "" : paywallInfo)
                .append(rainbowMigrationInfo)
                .append(amazonClassicMigrationInfo == null ? "" : amazonClassicMigrationInfo)
                .append(line);
        //RemoteLog should end with semicolon
        builder.append(";");
        String logMessage = builder.toString();
        Logger.d(TAG, logMessage);
        try {
            FileWriter fw = new FileWriter(file, true);
            fw.write(logMessage + "\n");
            fw.close();
        } catch (IOException ex) {
            Logger.e(TAG, "could not write line.");
            ex.printStackTrace();
            // we dropped one line, not a big deal
        }
    }

    /* Default Log entries - OS, OS Version, APP, APP Version, Device */
    private String getDeviceInfoString() {
        StringBuilder deviceInfoLog = new StringBuilder();
        String jUcid = RemoteLog.getInstance().getRemoteLogProvider().getJUcid();

        String appVersion = "Unknown";
        try {
            PackageInfo pInfo = getAppContext().getPackageManager().getPackageInfo(getAppContext().getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (Exception ex) {
            // ignore.  We will set to unknown
        }

        deviceInfoLog.append(", ").append(LogFileWriter.INFO_OS).append("=").append("\"" + LogFileWriter.ANDROID + "\"").append(", ").append(LogFileWriter.INFO_APP_VERSION).
                append("=").append("\"" + appVersion + "\"").append(", ").append(LogFileWriter.INFO_OS_VERSION).append("=").append("\"" + Build.VERSION.RELEASE + "\"").
                append(", ").append(LogFileWriter.INFO_DEVICE).append("=").append("\"" + Build.MODEL + "\"").append(", ").append(LogFileWriter.INFO_APP).append("=").
                append("\"" + getConfig().getAppName() + "\"").append(", ").append(INFO_TABLET).append("=").append("\"" + DeviceUtils.isTablet(getAppContext()) + "\"")
                .append(", ").append(LogFileWriter.INFO_LOGGING_ID).append("=").append("\"" + DeviceUtils.getUniqueDeviceId(getAppContext()) + "\"")
                .append(", ").append(LogFileWriter.INFO_SERIAL_ID).append("=").append("\"" + DeviceUtils.getDeviceSerialId(getAppContext()) + "\"")
                .append(", ").append(LogFileWriter.INFO_VPN_STATUS).append("=").append("\"" + ReachabilityUtil.isOnVPN(getAppContext()) + "\"")
                .append(", ").append(LogFileWriter.CONFIG_VERSION).append("=").append("\"" + getAppConfig().getVersion() + "\"")
                .append(", ").append(LogFileWriter.INFO_JUC_ID).append("=").append("\"").append(jUcid).append("\"");
        return deviceInfoLog.toString();
    }

    private String getProcessInfo(String process) {
        if (process != null) {
            return new StringBuilder(PROCESS).append("=").append("\"" + process + "\"").append(", ").toString();
        } else {
            return "";
        }
    }

    private String getDate() {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ZZZZZ", Locale.getDefault());
        return s.format(new Date());
    }

    private String getNetworkInfoLog() {
        return new StringBuilder(", ").append(LogFileWriter.INFO_NETWORK).append("=").append("\"" + this.getNetworkInfo() + "\"").toString();
    }

    private String getNetworkInfo() {
        if (null == getAppContext()) {
            return "Unknown";
        }

        String network = "No Connectivity";
        if (isConnectedOrConnecting(getAppContext())) {
            if (ReachabilityUtil.isOnWiFi(getAppContext())) {
                network = "WiFi";
            } else {
                network = "Cellular";
            }
        }
        return network;
    }

    private String getLevelInfo(Level level) {
        return new StringBuilder(", ").append(LogFileWriter.INFO_LEVEL).append("=").append("\"" + level.toString() + "\"").append(", ").toString();
    }

    private Context getAppContext() {
        return context;
    }
}