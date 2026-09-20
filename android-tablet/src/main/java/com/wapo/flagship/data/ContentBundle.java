package com.wapo.flagship.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ContentBundle {
    public static final String TableName = "ContentBundle";
    public static final String IdColumn = "_id";
    public static final String NameColumn = "name";
    public static final String UrlColumn = "url";
    public static final String ModifiedColumn = "modified";
    public static final String SizeColumn = "size";
    public static final String PathColumn = "path";
    public static final String StatusColumn = "status";
    public static final String TypeColumn = "type";
    public static final String JsonUrlColumn = "jsonUrl";
    public static final String IsFavoriteColumn = "isFavorite";
    public static final String DisplayNameColumn = "displayName";
    public static final String ZSyncModifiedColumn = "zsyncModified";
    public static final String ZSyncETagColumn = "zsyncEtag";
    public static final String ETagColumn = "etag";
    public static final String CheckTsColumn = "checkTs";
    public static final String FrontUrlColumn = "frontUrl";

    public static final String[] Columns = new String[] {
            IdColumn,
            NameColumn,
            UrlColumn,
            ModifiedColumn,
            SizeColumn,
            PathColumn,
            TypeColumn,
            StatusColumn,
            JsonUrlColumn,
            IsFavoriteColumn,
            DisplayNameColumn,
            ZSyncModifiedColumn,
            ZSyncETagColumn,
            ETagColumn,
            CheckTsColumn,
            FrontUrlColumn
    };
    public static final String[] ColumnsTypes = new String[] {
            "INTEGER PRIMARY KEY AUTOINCREMENT",
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
            "INTEGER NOT NULL DEFAULT 0",
            "TEXT",
            "TEXT",
            "INTEGER NOT NULL DEFAULT 0",
            "TEXT"
    };
    private static final String[] NO_SQL = new String[0];

    private long _id = -1;
    private String _name;
    private String _url;
    private long _modified;
    private long _zsyncModified;
    private long _size = 0;
    private String _path;
    private Status _status = Status.Pending;
    private Type _type;
    private String _jsonUrl;
    private boolean _isFavorite = false;
    private String _displayName;
    private String _zsyncETag;
    private String _etag;
    private long _checkTs;
    private String _frontUrl;

    public static ITableDescription getTableDescription() {
        return new ITableDescription() {
            @NonNull
            @Override
            public String getTableName() {
                return ContentBundle.TableName;
            }

            @Override
            public String[] getColumns() {
                return ContentBundle.Columns;
            }

            @Override
            public String[] getColumnsTypes() {
                return ContentBundle.ColumnsTypes;
            }

            @Override
            public String[] getKeys() {
                return NO_SQL;
            }

            @Override
            public String[] getPostCreationSql() {
                return new String[] {
                        String.format(Locale.US, "CREATE INDEX %1$s_%2$s_Index ON %1$s ( %2$s );", TableName, NameColumn)
                };
            }

            @Override
            public String[] getPreDeletionSql() {
                return new String[] {
                        String.format(Locale.US, "DROP INDEX IF EXISTS %s_%s_Index", TableName, NameColumn)
                };
            }
        };
    }

    public ContentBundle(String name) {
        _name = name;
    }

    public ContentBundle(String name, Type type) {
        _name = name;
        _type = type;
    }

    public ContentBundle(Cursor cursor) {
        for (int i = 0, cnt = cursor.getColumnCount(); i < cnt; i++) {
            String name = cursor.getColumnName(i);
            if (IdColumn.equals(name)) {
                _id = cursor.getInt(i);
            } else if (NameColumn.equals(name)) {
                _name = cursor.getString(i);
            } else if (UrlColumn.equals(name)) {
                _url = cursor.getString(i);
            } else if (ModifiedColumn.equals(name)) {
                _modified = cursor.isNull(i) ? 0 : cursor.getLong(i);
            } else if (SizeColumn.equals(name)) {
                _size = cursor.getInt(i);
            } else if(PathColumn.equals(name)) {
                _path = cursor.getString(i);
            } else if (TypeColumn.equals(name)) {
                _type = cursor.isNull(i) ? null : Type.parse(cursor.getInt(i));
            } else if (StatusColumn.equals(name)) {
                _status = cursor.isNull(i) ? Status.Complete : Status.parse(cursor.getInt(i));
            } else if (JsonUrlColumn.equals(name)) {
                _jsonUrl = cursor.getString(i);
            } else if (IsFavoriteColumn.equals(name)) {
                _isFavorite = !(cursor.isNull(i) || cursor.getInt(i) == 0);
            } else if (DisplayNameColumn.equals(name)) {
                _displayName = cursor.getString(i);
            } else if (ZSyncModifiedColumn.equals(name)) {
                _zsyncModified = cursor.isNull(i) ? 0 : cursor.getLong(i);
            } else if (ZSyncETagColumn.equals(name)) {
                _zsyncETag = cursor.getString(i);
            } else if (ETagColumn.equals(name)) {
                _etag = cursor.getString(i);
            } else if (CheckTsColumn.equals(name)) {
                _checkTs = cursor.getLong(i);
            } else if (FrontUrlColumn.equals(name)) {
                _frontUrl = cursor.getString(i);
            }
        }
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(NameColumn, _name);
        result.put(UrlColumn, _url);
        result.put(ModifiedColumn, _modified);
        result.put(SizeColumn, _size);
        result.put(PathColumn, _path);
        if (_type != null) {
            result.put(TypeColumn, _type.getValue());
        }
        result.put(StatusColumn, _status.getValue());
        result.put(JsonUrlColumn, _jsonUrl);
        result.put(IsFavoriteColumn, _isFavorite ? 1 : 0);
        result.put(DisplayNameColumn, _displayName);
        result.put(ZSyncModifiedColumn, _zsyncModified);
        result.put(ZSyncETagColumn, _zsyncETag);
        result.put(ETagColumn, _etag);
        result.put(CheckTsColumn, _checkTs);
        result.put(FrontUrlColumn, _frontUrl);
        return result;
    }

    public String getFrontUrl() {
        return _frontUrl == null ? "" : _frontUrl;
    }

    public void setFrontUrl(String _frontUrl) {
        this._frontUrl = _frontUrl;
    }

    public boolean isNew() {
        return this._id < 0;
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        this._url = url;
    }

    public Long getModified() {
        return _modified;
    }

    public void setModified(long modified) {
        _modified = modified;
    }

    public long getZsyncModified() {
        return _zsyncModified;
    }

    public void setZsyncModified(long zsyncModified) {
        this._zsyncModified = zsyncModified;
    }

    public long getSize() {
        return _size;
    }

    public void setSize(long size) {
        _size = size;
    }

    public Type getType() {
        return _type;
    }

    public void setType(Type type) {
        if (type == null)
            throw new IllegalArgumentException("ContentBundle DisplayType can not be null");

        _type = type;
    }

    public Status getStatus() {
        return _status;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("ContentBundle status can not be null");
        }
        _status = status;
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String path) {
        _path = path;
    }

    public String getJsonUrl() {
        return _jsonUrl;
    }

    public void setJsonUrl(String url) {
        _jsonUrl = url;
    }

    public boolean isFavorite() {
        return _isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        _isFavorite = isFavorite;
    }

    public String getDisplayName() {
        return _displayName;
    }

    public void setDisplayName(String displayName) {
        this._displayName = displayName;
    }

    public String getZsyncETag() {
        return _zsyncETag;
    }

    public void setZsyncETag(String zsyncETag) {
        this._zsyncETag = zsyncETag;
    }

    public String getETag() {
        return _etag;
    }

    public void setETag(String etag) {
        this._etag = etag;
    }

    public long getCheckTimestamp() {
        return _checkTs;
    }

    public void updateCheckTimestamp() {
        this._checkTs = System.currentTimeMillis();
    }




    public static enum Status {
        Pending(0),
        Downloading(1),
        Downloaded(2),
        Complete(3),
        CheckedNotModified(4);

        private static final HashMap<Integer, Status> valueToInstance;

        static {
            valueToInstance = new HashMap<Integer, Status>();
            valueToInstance.put(0, Pending);
            valueToInstance.put(1, Downloading);
            valueToInstance.put(2, Downloaded);
            valueToInstance.put(3, Complete);
            valueToInstance.put(4, CheckedNotModified);
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

        public boolean equal(Status status) {
            return _value == status._value;
        }

        public boolean  equal(int value) {
            return _value == value;
        }

        @Override
        public String toString() {
            return Integer.toString(_value);
        }
    }

    public static enum Type {
        SuperJson(0),
        Section(1),
        Blog(2),
        Global(3),
        Comics(4),
        Archive(5),
        Gallery(6);

        private final int _value;

        private static final HashMap<Integer, Type> valueToInstance;

        static {
            valueToInstance = new HashMap<Integer, Type>();
            valueToInstance.put(0, SuperJson);
            valueToInstance.put(1, Section);
            valueToInstance.put(2, Blog);
            valueToInstance.put(3, Global);
            valueToInstance.put(4, Comics);
            valueToInstance.put(5, Archive);
            valueToInstance.put(6, Gallery);
        }

        public static Type parse(int val) {
            if (!valueToInstance.containsKey(val))
                throw new IllegalArgumentException("Invalid value: " + val);

            return valueToInstance.get(val);
        }

        Type(int value) {
            _value = value;
        }

        public int getValue() {
            return _value;
        }

        public boolean equal(Type type) {
            return _value == type._value;
        }

        public boolean  equal(int value) {
            return _value == value;
        }

        @Override
        public String toString() {
            return Integer.toString(_value);
        }
    }
}
