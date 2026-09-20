package com.wapo.flagship.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;

public class FileMeta {
    private static final String TAG = FileMeta.class.getName();
    @Deprecated
    public static final String SAVED_URL_PREFIX = "favorites_article_prefix://";
    public static final long TTL = 12 * 3600000; //12 hours in milliseconds

    public static final String TableName = "FileMeta";
    public static final String IdColumn = "_id";
    public static final String BundleIdColumn = "bundleId";
    public static final String PathColumn = "path";
    public static final String ContentTypeColumn = "contentType";
    public static final String EncodingColumn = "encoding";
    public static final String ServerDateColumn = "serverDate";
    @Deprecated
    public static final String LockedColumn = "locked";
    public static final String UrlColumn = "url";
    public static final String StatusColumn = "status";
    public static final String TtlColumn = "ttl";
    public static final String TitleColumn = "title";
    public static final String ExpiredColumn = "expired";
    public static final String EntityTagColumn = "etag";
    public static final String ClientDateColumn = "clientDate";
    public static final String HashColumn = "hash";
    public static final String LastModifiedColumn = "lastModified";

    public static final String[] Columns = new String[] {
            IdColumn,
            BundleIdColumn,
            PathColumn,
            ContentTypeColumn,
            EncodingColumn,
            ServerDateColumn,
            LockedColumn,
            UrlColumn,
            StatusColumn,
            TtlColumn,
            TitleColumn,
            ExpiredColumn,
            EntityTagColumn,
            ClientDateColumn,
            HashColumn,
            LastModifiedColumn,
    };
    public static final String[] ColumnsTypes = new String[] {
            "INTEGER PRIMARY KEY AUTOINCREMENT",
            "INTEGER",
            "TEXT",
            "TEXT",
            "TEXT",
            "INTEGER",
            "INTEGER",
            "TEXT",
            "INTEGER",
            "INTEGER",
            "TEXT",
            "INTEGER",
            "TEXT",
            "INTEGER",
            "INTEGER NOT NULL UNIQUE ON CONFLICT IGNORE DEFAULT 0",
            "INTEGER",
    };
    private static final String[] NO_SQL = new String[0];

    private long _id = -1;
    private Long _bundleId;
    private String _path;
    private String _contentType;
    private String _encoding;
    private long _serverDate = 0;
    @Deprecated
    private boolean _locked = false;
    private String _url;
    private Status _status = Status.Complete;
    private long _ttl = 0;
    private String _title;
    private long _expired = 0;
    private String _eTag;
    private long _clientDate = 0;
    private long _hash = 0;
    private long _lastModified = 0;

    public static ITableDescription getTableDescription() {
        return new ITableDescription() {
            @NonNull
            @Override
            public String getTableName() {
                return FileMeta.TableName;
            }

            @Override
            public String[] getColumns() {
                return FileMeta.Columns;
            }

            @Override
            public String[] getColumnsTypes() {
                return FileMeta.ColumnsTypes;
            }

            @Override
            public String[] getKeys() {
                return NO_SQL;
            }

            @Override
            public String[] getPostCreationSql() {
                return new String[] {
                        String.format(Locale.US, "CREATE INDEX %1$s_%2$s_Index ON %1$s ( %2$s );", TableName, PathColumn),
                        String.format(Locale.US, "CREATE INDEX %1$s_%2$s_Index ON %1$s ( %2$s );", TableName, HashColumn)
                };
            }

            @Override
            public String[] getPreDeletionSql() {
                return new String[] {
                        String.format(Locale.US, "DROP INDEX IF EXISTS %s_%s_Index", TableName, PathColumn),
                        String.format(Locale.US, "DROP INDEX IF EXISTS %s_%s_Index;", TableName, HashColumn)
                };
            }
        };
    }

    public FileMeta(Long bundleId) {
        _bundleId = bundleId;
    }

    FileMeta(Cursor cursor) {
        _id = cursor.getInt(0);
        _bundleId = cursor.isNull(1) ? null : cursor.getLong(1);
        _path = cursor.getString(2);
        _contentType = cursor.getString(3);
        _encoding = cursor.getString(4);
        _serverDate = cursor.isNull(5) ? 0 : cursor.getLong(5);
        _locked = cursor.getInt(6) != 0;
        _url = cursor.getString(7);
        _status = cursor.isNull(8) ? Status.Complete : Status.parse(cursor.getInt(8));
        _ttl = cursor.isNull(9) ? 0 : cursor.getLong(9);
        _title = cursor.getString(10);
        _expired = cursor.isNull(11) ? 0 : cursor.getLong(11);
        _eTag = cursor.getString(12);
        _clientDate = cursor.isNull(13) ? 0 : cursor.getLong(13);
        _hash = cursor.getLong(14);
        _lastModified = cursor.isNull(15) ? 0 : cursor.getLong(15);
    }

    public FileMeta(Long bundleId, String path, String url, long hash, String contentType, String encoding, long serverDate, long lastModified) {
        _bundleId = bundleId;
        _path = path;
        _url = url;
        _contentType = contentType;
        _encoding = encoding;
        _serverDate = serverDate;
        _ttl = _serverDate + TTL;
        _hash = hash;
        _lastModified = lastModified;
    }

    public FileMeta(FileMeta fileMeta) {
        _bundleId = fileMeta.getBundleId();
        _path = fileMeta.getPath();
        _contentType = fileMeta.getContentType();
        _encoding = fileMeta.getEncoding();
        _serverDate = fileMeta.getServerDate();
        _locked = fileMeta.isLocked();
        _url = fileMeta.getUrl();
        _status = fileMeta.getStatus();
        _ttl = fileMeta.getTtl();
        _title = fileMeta.getTitle();
        _expired = fileMeta.getExpired();
        _eTag = fileMeta.getEntityTag();
        _clientDate = fileMeta.getClientDate();
        _lastModified = fileMeta.getLastModified();
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(BundleIdColumn, _bundleId);
        result.put(PathColumn, _path);
        result.put(ContentTypeColumn, _contentType);
        result.put(EncodingColumn, _encoding);
        result.put(ServerDateColumn, _serverDate);
        result.put(LockedColumn, _locked ? 1 : 0);
        result.put(UrlColumn, _url);
        result.put(StatusColumn, _status.getValue());
        result.put(TtlColumn, _ttl);
        result.put(TitleColumn, _title);
        result.put(ExpiredColumn, _expired);
        result.put(EntityTagColumn, _eTag);
        result.put(ClientDateColumn, _clientDate);
        result.put(HashColumn, _hash);
        result.put(LastModifiedColumn, _lastModified);
        return result;
    }

    public long getId() {
        return _id;
    }

    public Long getBundleId() {
        return _bundleId;
    }

    public void setBundleId(Long bundleId) {
        _bundleId = bundleId;
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String path) {
        _path = path;
    }

    public String getContentType() {
        return _contentType;
    }

    public void setContentType(String contentType) {
        this._contentType = contentType;
    }

    public String getEncoding() {
        return _encoding;
    }

    public void setEncoding(String encoding) {
        this._encoding = encoding;
    }

    public long getServerDate() {
        return _serverDate;
    }

    public void setServerDate(long serverDate) {
        _serverDate = serverDate;
        if (_ttl == 0) {
            _ttl = _serverDate + TTL;
        }
    }

    public boolean isNew() {
        return _id < 0;
    }

    @Deprecated
    public boolean isLocked() {
        return _locked;
    }

    @Deprecated
    public void setLocked(boolean locked) {
        _locked = locked;
    }

    public Status getStatus() {
        return _status;
    }

    public void setStatus(Status status) {
        _status = status;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = url;
    }

    public long getTtl() {
        return _ttl;
    }

    public void droptTtl() {
        _ttl = 0;
        _expired = 0;
    }

    public void updateTtl() {
        _ttl = System.currentTimeMillis() + TTL;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public long getExpired() {
        return _expired;
    }

    public void setExpired(long expired) {
        this._expired = expired;
    }
    //
    // should cache be revalidated
    public boolean shouldRefresh() {
        return _bundleId == null && _expired < System.currentTimeMillis();
    }

    public String getEntityTag() {
        return _eTag;
    }

    public void setEntityTag(String eTag) {
        _eTag = eTag;
    }

    public long getClientDate() {
        return _clientDate;
    }

    public void setClientDate(long _clientDate) {
        this._clientDate = _clientDate;
    }

    public long getHash() {
        return _hash;
    }

    public void setHash(long hash) {
        _hash = hash;
    }

    public long getLastModified() {
        return _lastModified;
    }

    public void setLastModified(long lastModified) {
        _lastModified = lastModified;
    }

    public static enum Status {
        UpdatePending(0),
        Downloading(1),
        Complete(2);

        private static final HashMap<Integer, Status> valueToInstance;

        static {
            valueToInstance = new HashMap<Integer, Status>();
            valueToInstance.put(0, UpdatePending);
            valueToInstance.put(1, Downloading);
            valueToInstance.put(2, Complete);
        }

        private final int _value;

        Status(int value) {
            _value = value;
        }

        public static Status parse(int val) {
            if (!valueToInstance.containsKey(val))
                throw new IllegalArgumentException("Invalid value: " + val);

            return valueToInstance.get(val);
        }

        public int getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return Integer.toString(_value);
        }
    }

}
