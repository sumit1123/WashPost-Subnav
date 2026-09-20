/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

import static com.wapo.android.commons.util.Utils.isConnectedOrConnecting;

import android.content.Context;
import android.os.Bundle;

import com.wapo.android.commons.util.ReachabilityUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class LogFileUploader {

    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    protected void process(Bundle bundle) {
        boolean forceUpload = bundle != null && bundle.getBoolean(LogFileWriter.IS_FORCE_UPLOAD, false);
        File compressedFileDir = new File(getAppContext().getFilesDir() + File.separator + RemoteLog.LOGGER_FOLDER + File.separator + RemoteLog.COMPRESSED_LOG_FOLDER);

        if ((RemoteLog.getInstance().getRemoteLogProvider().isApplicationInForeground() || forceUpload) &&
                isConnectedOrConnecting(getAppContext()) && ReachabilityUtil.isOnWiFi(getAppContext())) {
            if (compressedFileDir.list() == null || compressedFileDir.list().length < 1) {
                return;
            }

            List<File> filesToBePurged = new LinkedList<>();
            for (File fileToUpload : compressedFileDir.listFiles()) {
                if (RemoteLog.getInstance().getUploadHelper().upload_gZipCompressedFile(fileToUpload)) {
                    //purge file if upload is successful
                    filesToBePurged.add(fileToUpload);
                }
            }

            this.purgeFiles(filesToBePurged);
        }
    }

    private void purgeFiles(List<File> files) {
        if (files != null && files.size() > 0) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private void purgeFilesIfNeeded(File compressedFileDir) {
        File[] files = compressedFileDir.listFiles();

        int fileCountCap = 4;

        if (files == null || files.length < fileCountCap) {
            return;
        }

        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return (int) (f1.lastModified() - f2.lastModified());
            }
        });

        int fileCount = files.length;

        for (int i = 0; fileCount > fileCountCap; i++) {
            files[i].delete();
            fileCount--;
        }
    }

    private Context getAppContext() {
        return context;
    }

    public interface UploadHelper {
        //return whether file upload was successful or not
        boolean upload_gZipCompressedFile(File file);
    }
}