package com.wapo.flagship.data;

import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;

import java.util.HashMap;

public class Archive {
    public static final String TableName = "Archive";
    public static final String IdColumn = "_id";
    public static final String DownloadIdColumn = "DownloadId";
    public static final String PathColumn = "Path";
    public static final String DateColumn = "Date";//yyyyMMdd
    public static final String SectionColumn = "Section";
    public static final String StatusColumn = "Status";
    public static final String TimestampColumn = "Timestamp";
    public static final String LmtColumn = "Lmt";
    public static final String [] Columns = new String[] {IdColumn, DownloadIdColumn, PathColumn, DateColumn, SectionColumn, StatusColumn, TimestampColumn, LmtColumn};
    public static final String [] ColumnTypes = new String[] {
            "INTEGER PRIMARY KEY AUTOINCREMENT",
            "INTEGER",
            "TEXT",
            "INTEGER",
            "TEXT",
            "INTEGER",
            "INTEGER NOT NULL DEFAULT 0",
            "INTEGER NOT NULL DEFAULT 0"
    };
    private static final String[] NO_SQL = new String[0];

    private long _id;
    private Long _downloadId;
    private String _path;
    private long _date;
    private String _section;
    private Status _status = Status.None;
    private long _timestamp = 0;
    private long _lmt = 0;

    public static ITableDescription getTableDescription() {
        return new ITableDescription() {
            @NonNull
            @Override
            public String getTableName() {
                return TableName;
            }

            @Override
            public String[] getColumns() {
                return Columns;
            }

            @Override
            public String[] getColumnsTypes() {
                return ColumnTypes;
            }

            @Override
            public String[] getKeys() {
                return NO_SQL;
            }

            @Override
            public String[] getPostCreationSql() {
                return NO_SQL;
            }

            @Override
            public String[] getPreDeletionSql() {
                return NO_SQL;
            }
        };
    }

    public Archive(long date, String sectionLetter) {
        _date = date;
        _section = sectionLetter;
        _timestamp = System.currentTimeMillis();
    }

    Archive(Cursor cursor) {
        _id = cursor.getInt(0);
        _downloadId = cursor.isNull(1) ? null : cursor.getLong(1);
        _path = cursor.getString(2);
        _date = cursor.getInt(3);
        _section = cursor.getString(4);
        _status = Status.parse(cursor.getInt(5));
        _timestamp = cursor.getLong(6);
        _lmt = cursor.getLong(7);
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(DownloadIdColumn, _downloadId);
        result.put(PathColumn, _path);
        result.put(DateColumn, _date);
        result.put(SectionColumn, _section);
        result.put(StatusColumn, _status.getValue());
        result.put(TimestampColumn, _timestamp);
        result.put(LmtColumn, _lmt);
        return result;
    }


    public long getId() {
        return _id;
    }

    public Long getDownloadId() {
        return _downloadId;
    }

    public void setDownloadId(Long downloadId) {
        _downloadId = downloadId;
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String path) {
        _path = path;
    }

    public long getDate() {
        return _date;
    }

    public String getSection() { return _section; }

    public Status getStatus() {
        return _status;
    }

    public void setStatus(Status _status) {
        this._status = _status;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public void updateTimestamp() {
        _timestamp = System.currentTimeMillis();
    }

    public long getLmt() { return _lmt; }

    public void setLmt(Long lmt) { this._lmt = lmt; }

    public static enum Status {
        None(0),
        Canceled(1),
        Deleted(2);

        private static final HashMap<Integer, Status> valueToInstance;

        static {
            valueToInstance = new HashMap<Integer, Status>();
            valueToInstance.put(None.getValue(), None);
            valueToInstance.put(Canceled.getValue(), Canceled);
            valueToInstance.put(Deleted.getValue(), Deleted);
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
}
