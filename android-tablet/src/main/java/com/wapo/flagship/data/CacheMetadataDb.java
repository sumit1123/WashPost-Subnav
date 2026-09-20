package com.wapo.flagship.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import com.wapo.android.commons.util.Logger;
import android.util.Log;
import androidx.annotation.NonNull;

import com.wapo.flagship.Utils;
import com.wapo.flagship.content.notifications.NotificationTable;
import com.wapo.flagship.wrappers.CrashWrapper;

import java.util.Locale;

public class CacheMetadataDb extends SQLiteOpenHelper {
    public static final String Name = "CacheMetadataDb";
    private final Context _context;

    private ITableDescription[] _tables;

    private CacheManager _cacheManager;

    public CacheMetadataDb(Context context, int version, ITableDescription[] tables, CacheManager cacheManager) {
        this(context, Name, version, tables, cacheManager);
    }

    public CacheMetadataDb(Context context, String dbName, int version, ITableDescription[] tables, CacheManager cacheManager) {
        super(context, dbName, null, version);
        _tables = tables.clone();
        _context = context;
        _cacheManager = cacheManager;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (ITableDescription table : _tables) {
            createTable(db, table);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int currentVersion, int newVersion) {
        if (currentVersion < 17) {
            //Nuke current archives and cache if older then native articles
            Utils.deleteFileOrFolder(_cacheManager.getTargetDir());
            Utils.deleteFileOrFolder(ArchiveManager.getPdfRootFolder(_context));

            for(ITableDescription table : new ITableDescription[] {ContentBundle.getTableDescription(), Archive.getTableDescription(), FileMeta.getTableDescription()}){
                dropTable(db, table);
                createTable(db, table);
            }
        }else if (currentVersion < 18) {
            try {
                ITableDescription tableDesc = ContentBundle.getTableDescription();
                //
                // rename ContetnBundleTable
                final String tempTableName = "ContentBundleDel";
                for (String sql: tableDesc.getPreDeletionSql()) {
                    db.execSQL(sql);
                }
                db.execSQL(String.format("ALTER TABLE ContentBundle RENAME TO %s", tempTableName));
                //
                // recreate content bundle table
                createTable(db, ContentBundle.getTableDescription());
                //
                // move data from temp table into the new ContentBundle table
                String copyQuery = String.format(
                        "INSERT INTO %s SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 0 AS %s, NULL AS %s, NULL AS %s, 0 as %s, NULL AS %s FROM %s",
                        ContentBundle.TableName,
                        ContentBundle.IdColumn,
                        ContentBundle.NameColumn,
                        ContentBundle.UrlColumn,
                        ContentBundle.ModifiedColumn,
                        ContentBundle.SizeColumn,
                        ContentBundle.PathColumn,
                        ContentBundle.TypeColumn,
                        ContentBundle.StatusColumn,
                        ContentBundle.JsonUrlColumn,
                        ContentBundle.IsFavoriteColumn,
                        ContentBundle.DisplayNameColumn,
                        ContentBundle.ZSyncModifiedColumn,
                        ContentBundle.ZSyncETagColumn,
                        ContentBundle.ETagColumn,
                        ContentBundle.CheckTsColumn,
                        ContentBundle.FrontUrlColumn,
                        tempTableName
                );
                db.execSQL(copyQuery);
                //
                // drop temp table
                db.execSQL("DROP TABLE " + tempTableName);
            } catch (Exception e) {
                Logger.e(CacheMetadataDb.class.getName(), Log.getStackTraceString(e));
            }
        }else if (currentVersion < 20){
            //for update ContentBundle table (add FrontUrl column)
            ITableDescription table = ContentBundle.getTableDescription();
            int updateColumnIndex = getColumnIndex(table, ContentBundle.FrontUrlColumn);
            if (updateColumnIndex != -1) {
                db.execSQL("ALTER TABLE " + table.getTableName() + " ADD COLUMN " + table.getColumns()[updateColumnIndex] + " " + table.getColumnsTypes()[updateColumnIndex] + ";");
                db.execSQL(String.format("UPDATE %s SET %s = 0, %s = NULL;", ContentBundle.TableName, ContentBundle.ModifiedColumn, ContentBundle.ETagColumn));
            }
        }

        if (currentVersion < 24) {
            createTable(db, NotificationTable.getTableDescription());
        }

        if (currentVersion < 25) {
            createTable(db, RecentSection.getTableDescription());
        }

        if (currentVersion < 26 && currentVersion >= 24) {
            ITableDescription table = NotificationTable.getTableDescription();
            int updateColumnIndex = getColumnIndex(table, NotificationTable.NotifIdColumn);
            if (updateColumnIndex != -1) {
                    db.execSQL("ALTER TABLE " + table.getTableName() + " ADD COLUMN " + table.getColumns()[updateColumnIndex] + " " + table.getColumnsTypes()[updateColumnIndex] + ";");
            }
        }

        if (currentVersion < 27) {
            //Update the archive table and wipe the old archive content.
            ITableDescription table = Archive.getTableDescription();
            dropTable(db, table);
            createTable(db, table);

            String tableName = Archive.TableName;
            if (doesTableExist(db, tableName)) {
                db.delete(tableName, null, null);
            }
            Utils.deleteFileOrFolder(ArchiveManager.getPdfRootFolder(_context));
        }

        if (currentVersion < 28) {
            //wipe out alerts which had incorrect timestamps
            String tableName = NotificationTable.Name;
            if (doesTableExist(db, tableName)) {
                db.delete(tableName, null, null);
            }
        }

        if (currentVersion < 29) {
            ITableDescription table = FileMetaUserArticle.getTableDescription();
            createTable(db, table);
        }

        if (currentVersion < 30) {
            ITableDescription table = Archive.getTableDescription();
            int updateColumnIndex = getColumnIndex(table, Archive.LmtColumn);
            if (updateColumnIndex != -1) {
                try {
                    db.execSQL("ALTER TABLE " + table.getTableName() + " ADD COLUMN " + table.getColumns()[updateColumnIndex] + " " + table.getColumnsTypes()[updateColumnIndex] + ";");
                } catch (RuntimeException e) {
                    CrashWrapper.logExtras("Issue updating Archive table to add Lmt column.");
                    CrashWrapper.sendException(e);
                }
            }
        }

        if (currentVersion < 31) {
            ITableDescription table = FileMeta.getTableDescription();
            int updateColumnIndex = getColumnIndex(table, FileMeta.LastModifiedColumn);
            try {
                db.execSQL("ALTER TABLE " + table.getTableName() + " ADD COLUMN " + table.getColumns()[updateColumnIndex] + " " + table.getColumnsTypes()[updateColumnIndex] + ";");
            } catch (RuntimeException e) {
                CrashWrapper.logExtras("Issue updating FileMeta table to add Lmt column.");
                CrashWrapper.sendException(e);
            }
        }
    }

    public static boolean doesTableExist(@NonNull SQLiteDatabase db, @NonNull String tableName) {
        try {
            String[] columns = {"tbl_name"};
            String selection = String.format(Locale.US, "%s = '%s'", columns[0], tableName);
            Cursor cursor = db.query(true, "sqlite_master", columns, selection, null, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (SQLiteException e) {
            Logger.e(Name, "Query error", e);
        }
        return false;
    }

    private int getColumnIndex(ITableDescription table, String frontUrlColumn) {
        for (int i = 0; i < table.getColumns().length; i++){
            if (frontUrlColumn.equals(table.getColumns()[i]))
                return i;
        }
        return -1;
    }

    private static void dropTable(SQLiteDatabase db, ITableDescription table) {
        for(String preDeleteSql : table.getPreDeletionSql()){
            db.execSQL(preDeleteSql);
        }
        db.execSQL("DROP TABLE IF EXISTS " + table.getTableName() + ";");
    }

    private static void createTable(SQLiteDatabase db, ITableDescription table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ");
        sb.append(table.getTableName()).append(" (");
        String[] columns = table.getColumns();
        String[] types = table.getColumnsTypes();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(columns[i]).append(" ").append(types[i]);
        }
        for (String key : table.getKeys()) {
            sb.append(", ");
            sb.append(key);
        }
        sb.append(");");
        db.execSQL(sb.toString());
        for (String sql: table.getPostCreationSql()) {
            db.execSQL(sql);
        }
    }
}
