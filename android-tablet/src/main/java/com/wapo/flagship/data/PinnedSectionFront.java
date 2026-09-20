package com.wapo.flagship.data;

import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;

public class PinnedSectionFront {

    public static final String TableName = "PinnedSectionFront";
    public static final String IdColumn = "id";
    public static final String BundleNameColumn = "BundleName";
    public static final String NameColumn = "Name";
    public static final String TypeColumn = "Type";
    public static final String PositionColumn = "Position";
    public static final String RemovableColumn = "Removable";//yyyyMMdd
    public static final String [] Columns = new String[] {IdColumn, BundleNameColumn, NameColumn, TypeColumn, PositionColumn, RemovableColumn};
    public static final String [] ColumnTypes = new String[] {
            "INTEGER PRIMARY KEY AUTOINCREMENT",
            "TEXT",
            "TEXT",
            "TEXT",
            "INTEGER",
            "BYTE"
    };
    private static final String[] NO_SQL = new String[0];
    private static final int UPST_NONE = -1;
    private static final int UPST_INSERT = 1;
    private static final int UPST_UPDATE = 2;
    private static final int UPST_DELETE = 3;
    public static final int POSITION_NONE = -1;


    private int id = -1;
    private String name;
    private String bundleName;
    private String type;
    private int position;
    private boolean removable;

    private int updateStatus = UPST_NONE;

    public PinnedSectionFront() {
    }

    public PinnedSectionFront(String bundleName, String name, int position, boolean removable, String type) {
        this.bundleName = bundleName;
        this.name = name;
        this.position = position;
        this.removable = removable;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public void setUpdateStatusInsert() {
        updateStatus = UPST_INSERT;
    }

    public void setUpdateStatusUpdate() {
        updateStatus = UPST_UPDATE;
    }

    public void setUpdateStatusDelete() {
        updateStatus = UPST_DELETE;
    }

    public boolean isInsertUpdateStatus() {
        return updateStatus == UPST_INSERT;
    }

    public boolean isUpdateUpdateStatus() {
        return updateStatus == UPST_UPDATE;
    }

    public boolean isDeleteUpdateStatus() {
        return updateStatus == UPST_DELETE;
    }

    public PinnedSectionFront(Cursor cursor) {
        id = cursor.getInt(0);
        bundleName = cursor.getString(1);
        name = cursor.getString(2);
        type = cursor.getString(3);
        position = cursor.getInt(4);
        removable = cursor.getInt(5) == 1;
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(BundleNameColumn, bundleName);
        result.put(NameColumn, name);
        result.put(TypeColumn, type);
        result.put(PositionColumn, position);
        result.put(RemovableColumn, removable ? 1 : 0);
        return result;
    }

    public static ITableDescription getTableDescription() {
        return new ITableDescription() {
            @NonNull
            @Override
            public String getTableName() {
                return PinnedSectionFront.TableName;
            }

            @Override
            public String[] getColumns() {
                return PinnedSectionFront.Columns;
            }

            @Override
            public String[] getColumnsTypes() {
                return PinnedSectionFront.ColumnTypes;
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

    public int getHashCode() {
        return (bundleName + name + position + removable).hashCode();
    }

    public int getBundleHashCode() {
        return (bundleName + name).hashCode();
    }
}
