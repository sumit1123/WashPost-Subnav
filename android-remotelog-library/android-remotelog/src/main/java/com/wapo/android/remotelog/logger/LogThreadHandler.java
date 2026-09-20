/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import com.wapo.android.commons.util.Logger;

public class LogThreadHandler {

    private final static String TAG = "[d][Com][LogTH]";
    private final static String LOG_WRITER_THREAD = "writerHandlerThread";
    private final static String LOG_UPLOADER_THREAD = "uploaderHandlerThread";
    public final static int FILE_WRITER_MSG = 1;
    public final static int FILE_UPLOADER_MSG = 2;

    private Looper logWriterLooper, logUploaderLooper;
    private LogHandler logWriterHandler, logUploaderHandler;
    private Context context;

    private final class LogHandler extends Handler {

        private LogFileWriter logFileWriter;
        private LogFileUploader logFileUploader;

        public LogHandler(Looper looper) {
            super(looper);
            if (LOG_WRITER_THREAD.equalsIgnoreCase(looper.getThread().getName())) {
                logFileWriter = new LogFileWriter();
                logFileWriter.setContext(context);
            } else if (LOG_UPLOADER_THREAD.equalsIgnoreCase(looper.getThread().getName())) {
                logFileUploader = new LogFileUploader();
                logFileUploader.setContext(context);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case FILE_WRITER_MSG:
                        if (logFileWriter != null && msg.obj instanceof Bundle) {
                            logFileWriter.process((Bundle) msg.obj);
                        }
                        break;
                    case FILE_UPLOADER_MSG:
                        if (logFileUploader != null) {
                            logFileUploader.process(msg.obj instanceof Bundle ? (Bundle) msg.obj : null);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                Logger.e(TAG, "remoteLog - error in handleMessage - " + e.getMessage());
            }
        }
    }

    LogThreadHandler(Context context) {
        this.context = context;
        // Log Writer
        HandlerThread logWriterThread = new HandlerThread(LOG_WRITER_THREAD,
                Process.THREAD_PRIORITY_BACKGROUND);
        logWriterThread.start();

        logWriterLooper = logWriterThread.getLooper();
        logWriterHandler = new LogHandler(logWriterLooper);

        // Log Uploader
        HandlerThread logUploaderThread = new HandlerThread(LOG_UPLOADER_THREAD,
                Process.THREAD_PRIORITY_BACKGROUND);
        logUploaderThread.start();

        logUploaderLooper = logUploaderThread.getLooper();
        logUploaderHandler = new LogHandler(logUploaderLooper);
    }

    public void sendWriteMessage(Bundle intent) {
        Message msg = logWriterHandler.obtainMessage();
        msg.what = FILE_WRITER_MSG;
        msg.obj = intent;
        logWriterHandler.sendMessage(msg);
    }

    public void sendUploadMessage(Bundle intent) {
        if (!logUploaderHandler.hasMessages(FILE_UPLOADER_MSG)) {
            Message msg = logUploaderHandler.obtainMessage();
            msg.what = FILE_UPLOADER_MSG;
            msg.obj = intent;
            logUploaderHandler.sendMessage(msg);
        }
    }
}
