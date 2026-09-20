package com.wapo.flagship.data;

import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;

/**
 * Created by Sharvani on 7/30/16.
 */
public class RecentSection {
    public static final String TableName = "RecentSections";
    public static final String IdColumn = "id";
    public static final String MenuItemId = "MenuItemId";
    public static final String BundleNameColumn = "BundleName";
    public static final String NameColumn = "Name";
    public static final String TypeColumn = "Type";
    public static final String SectionTypeColumn = "SectionType";
    public static final String[] Columns = new String[]{IdColumn, MenuItemId, BundleNameColumn, NameColumn, TypeColumn, SectionTypeColumn};
    public static final String[] ColumnTypes = new String[]{
            "INTEGER PRIMARY KEY AUTOINCREMENT",
            "TEXT",
            "TEXT",
            "TEXT",
            "INTEGER",
            "TEXT"
    };

    private int id;
    private String menuItemId;
    private String name;
    private String bundleName;
    private int type;
    private String sectionType;
    private static final int UPST_NONE = -1;
    private static final int UPST_INSERT = 1;
    private static final int UPST_DELETE = 2;
    private int updateStatus = UPST_NONE;
    private static final String[] NO_SQL = new String[0];


    public RecentSection( String menuItemId, String name, String bundleName, int type, String sectionType) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.type = type;
        this.bundleName = bundleName;
        this.sectionType = sectionType;
    }

    public RecentSection(Cursor cursor) {
        id = cursor.getInt(0);
        menuItemId = cursor.getString(1);
        bundleName = cursor.getString(2);
        name = cursor.getString(3);
        type = cursor.getInt(4);
        sectionType = cursor.getString(5);
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(MenuItemId, menuItemId);
        result.put(BundleNameColumn, bundleName);
        result.put(NameColumn, name);
        result.put(TypeColumn, type);
        result.put(SectionTypeColumn, sectionType);
        return result;
    }

    public int getId() {
        return id;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public String getName() {
        return name;
    }

    public String getBundleName() {
        return bundleName;
    }

    public int getType() {
        return type;
    }

    public String getSectionType() {
        return sectionType;
    }

    public boolean isInsertUpdateStatus() {
        return updateStatus == UPST_INSERT;
    }


    public boolean isDeleteUpdateStatus() {
        return updateStatus == UPST_DELETE;
    }
    public void setUpdateStatusInsert() {
        updateStatus = UPST_INSERT;
    }

    public void setUpdateStatusDelete() {
        updateStatus = UPST_DELETE;
    }

    public static ITableDescription getTableDescription() {
        return new ITableDescription() {
            @NonNull
            @Override
            public String getTableName() {
                return RecentSection.TableName;
            }

            @Override
            public String[] getColumns() {
                return RecentSection.Columns;
            }

            @Override
            public String[] getColumnsTypes() {
                return RecentSection.ColumnTypes;
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
}
