package com.wapo.android.commons.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class Download {

    private static String TAG = Download.class.getName();
    private static DownloadManager _downloadManager = null;
    private Context _context;
    private long _id;
    private String _path;
    private int _status;
    private long _downloadedBytes;
    private long _totalBytes;
    private String _title;
    private String _description;
    private int _reason;
    private Uri _url;
    private String _mimeType;

    private Download() {
    }

    public Download(Context context, Uri url, String path) {
        _url = url;
        _path = path;
        _context = context;
    }

    public static long enqueue(Download download) {
        DownloadManager.Request request = new DownloadManager.Request(download.getUri());
        request.setDestinationUri(Uri.parse("file://" + download.getPath()));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle(download.getTitle());
        request.setDescription(download.getDescription());
        request.setMimeType(download.getMimeType());

        return getDownloadManager(download._context).enqueue(request);
    }

    public static Download get(Context context, long id) {
        if(context == null) {
            return null;
        }
        context = context.getApplicationContext();
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor cursor = null;
        try {
            cursor = getDownloadManager(context).query(query);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            Download result = new Download();
            result._context = context;
            result._id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
            String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            if (localUri != null && !localUri.isEmpty()) {
                result._path = Uri.parse(localUri).getPath();
            }
            result._status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            result._downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            result._totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            result._title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
            result._description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
            result._reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static int remove(Context context, long id) {
        int deleted = -1;
        if(ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_ALL_DOWNLOADS") == PackageManager.PERMISSION_GRANTED) {
            try {
                deleted = getDownloadManager(context).remove(id);
            } catch (Exception exception) {
                Logger.e(TAG, "Cannot download file with id: " + id, exception);
            }
        } else {
            Logger.e(TAG, "Cannot download file with id: " + id);
        }
        return deleted;
    }

    @Nullable
    private static DownloadManager getDownloadManager(Context context) {
        if (_downloadManager == null) {
            _downloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        }
        return _downloadManager;
    }

    public long getId() {
        return _id;
    }

    public String getPath() {
        return _path;
    }

    public int getStatus() {
        return _status;
    }

    public long getDownloadedBytes() {
        return _downloadedBytes;
    }

    public long getTotalBytes() {
        return _totalBytes;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String _title) {
        this._title = _title;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }

    public int getReason() {
        return _reason;
    }

    public Uri getUri() {
        return _url;
    }

    public String getMimeType() {
        return _mimeType;
    }

    public void setMimeType(String mimeType) {
        _mimeType = mimeType;
    }
}
