/*
 * Copyright (c) 2015. Washington Post Android Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.flagship.data;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.wapo.android.commons.util.Logger;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.content.notifications.NotificationData;
import com.wapo.flagship.content.notifications.NotificationTable;
import com.wapo.flagship.features.amazonunification.MigrationHelper;
import com.washingtonpost.android.BuildConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.cookie.DateUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.wapo.flagship.json.MenuSection.SECTION_TYPE_AUTHOR;

import javax.inject.Inject;

public class CacheManagerImpl extends CacheManager {
    private static final boolean D = BuildConfig.DEBUG;
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final int METADATA_DB_VERSION = 31;

    private static final Pattern METHODE_IMAGE_PATTERN = Pattern.compile("http://www\\.washingtonpost\\.com/rf/image_[^/]+(/.+)", Pattern.CASE_INSENSITIVE);

    private static final String TAG = CacheManagerImpl.class.getName();
    private static final String DTAG = "[d][cache]";
    public static final ITableDescription[] Tables = new ITableDescription[]{
            ContentBundle.getTableDescription(),
            FileMeta.getTableDescription(),
            FileMetaUserArticle.getTableDescription(),
            Archive.getTableDescription(),
            NotificationTable.getTableDescription(),
            RecentSection.getTableDescription()
    };

    //
    // this might be changed through reflection
    private static String FilesRoot = "files";

    private static CacheMetadataDb DbHelper;
    private static final Object DbHelperSync = new Object();
    private static File TargetDir;
    private final Context _context;

    @Override
    public SQLiteDatabase getDb() {
        synchronized (DbHelperSync) {
            if (DbHelper == null) {
                DbHelper = new CacheMetadataDb(_context, CacheManagerImpl.METADATA_DB_VERSION, CacheManagerImpl.Tables, this);
            }

            return DbHelper.getWritableDatabase();
        }
    }

    @Override
    public File getDatabasePath(String name) {
        return _context.getDatabasePath(name);
    }

    public String getPathByUrl(String url) {
        return getPathByHash(_context, getHashCode(url));
    }

    public String getPathByHash(long hash) {
        return getPathByHash(_context, hash);
    }

    public String getPathByHash(Context context, long hash) {
        return makePath(Long.toString(hash));
    }

    @Override
    public File getTargetDir() {
        if (TargetDir == null) {
            TargetDir = _context.getDir(FilesRoot, Context.MODE_PRIVATE);
        }
        return TargetDir;
    }

    private String makePath(String path) {
        String p = new File(getTargetDir(), path).getPath();
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    public static long writeFile(String path, byte[] data) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        OutputStream ofs = new BufferedOutputStream(new FileOutputStream(path));
        ofs.write(data);
        ofs.close();
        return data.length;
    }

    public static long writeFile(String path, InputStream is) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        OutputStream ofs = new BufferedOutputStream(new FileOutputStream(path));
        byte[] buff = new byte[8192];
        int len;
        long size = 0;
        while ((len = is.read(buff)) > 0) {
            ofs.write(buff, 0, len);
            size += len;
        }
        ofs.close();
        return size;
    }
    @Inject
    public CacheManagerImpl(Context context) {
        this._context = context;
        // Amazon Unification - if platform is amazon and this is the first time
        // Classic is launched with (com.washingtonpost.rainbow) id, we need to rename
        // the Rainbow's CacheMetadataDb and related file(s) as Classic is also using the same file name(s).
        // FIXME: https://arcpublishing.atlassian.net/browse/AWA-5987
        MigrationHelper.INSTANCE.renameRainbowDatabaseIfNotDoneYet(context.getApplicationContext());
    }

    @Override
    public void cleanUp() {
        if (D) { Logger.d(DTAG, "starting clean up"); }
        FlagshipApplication.getInstance().getSavedArticleManager().cleanup();
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            String query = String.format(Locale.US,
                    "DELETE FROM %1$s WHERE %1$s.%2$s < %3$s ",
                    FileMeta.TableName,
                    FileMeta.TtlColumn,
                    System.currentTimeMillis()
            );
            db.execSQL(query);
            SQLiteStatement stmt = db.compileStatement("SELECT changes()");
            long affectedRows = stmt.simpleQueryForLong();
            if (affectedRows > 0) {
                if (D) Logger.d(DTAG, String.format(Locale.US, "removed %d fileMeta(s)", affectedRows));
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            if (D) Logger.e(TAG, "clean up error", e);
        } finally {
            db.endTransaction();
        }

        for (File file : getTargetDir().listFiles()) {
            db.beginTransaction();
            try {
                List<FileMeta> fm = getFileMetas(db, FileMeta.PathColumn + "=" + DatabaseUtils.sqlEscapeString(file.getPath()), null, "1");
                // hold transaction until we delete the file
                if (fm.isEmpty()) {
                    boolean res = file.delete();
                    if (D) { Logger.d(DTAG, "removing file "+file.getPath()+"; result: "+res); }
                }
            } finally {
                db.endTransaction();
            }
        }
        if (D) { Logger.d(DTAG, "clean up completed"); }
    }

    public Context getContext() {
        return _context;
    }

    private long convertWebPToJpg(String path, InputStream in) {
        Bitmap bmp = null;
        File file = null;
        try {
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try {
                bmp = BitmapFactory.decodeStream(in, null, decodeOptions);
                if (bmp == null) {
                    return 0;
                }
                file = new File(path);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(path);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    //Try to recycle to help GC get back the mem faster
                    Logger.d(TAG, "Converting image. Location is [" + path + "]");
                    bmp.recycle();
                } catch (Exception e) {
                    Logger.e(TAG, "Error Converting image. Location is [" + path + "]", e);
                } finally {
                    if (out != null) {
                        IOUtils.closeQuietly(out);
                    }
                }
            } catch (Exception e) {
                Logger.e(TAG, "Got an exception while trying to decode an image. This will happen for animated gifs:", e);
                IOUtils.closeQuietly(in);
                //Couldn't decode the image so save the original to the file system
                FileUtils.copyInputStreamToFile(in, file);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Got an exception while processing image [" + path + "]", e);
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }
        return file != null && file.exists() ? FileUtils.sizeOf(file) : 0;
    }

    @Override
    public ContentBundle getBundleByName(String name) {
        List<ContentBundle> bundles = getBundles(
                String.format(
                        Locale.US,
                        "%s like %s",
                        ContentBundle.NameColumn,
                        DatabaseUtils.sqlEscapeString(name)
                ),
                1
        );
        return bundles.isEmpty() ? null : bundles.get(0);
    }

    @Override
    public ContentBundle getBundleById(long id) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            Cursor cursor = db.query(ContentBundle.TableName, ContentBundle.Columns, String.format(Locale.US, "%s = %d", ContentBundle.IdColumn, id), null, null, null, null);
            try {
                ContentBundle result = cursor.moveToNext() ? new ContentBundle(cursor) : null;
                db.setTransactionSuccessful();
                return result;
            } finally {
                cursor.close();
            }
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<ContentBundle> getBundles(String query) {
        return getBundles(query, -1);
    }

    @Override
    public List<ContentBundle> getBundles(String query, int limit) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<ContentBundle> result = getBundles(db, query, limit);

            db.setTransactionSuccessful();

            return result;
        } finally {
            db.endTransaction();
        }
    }

    public List<ContentBundle> getBundles(SQLiteDatabase db, String query, int limit) {
        Cursor cursor = db.query(ContentBundle.TableName, ContentBundle.Columns, query, null, null, null, null, limit > 0 ? Integer.toString(limit) : null);
        try {
            List<ContentBundle> result = new ArrayList<ContentBundle>();

            while (cursor.moveToNext()) {
                result.add(new ContentBundle(cursor));
            }

            return result;
        } finally {
            cursor.close();
        }
    }

    @Override
    public ContentBundle getArchiveBundleByLabel(long label) {
        List<ContentBundle> bundles = getBundles(
                String.format(
                        Locale.US,
                        "%s = '%d' and %s = %d",
                        ContentBundle.NameColumn,
                        label,
                        ContentBundle.TypeColumn,
                        ContentBundle.Type.Archive.getValue()
                ),
                1
        );
        return bundles.isEmpty() ? null : bundles.get(0);
    }

    @Override
    public List<ContentBundle> getArchiveBundles() {
        List<ContentBundle> bundles = getBundles(
                String.format(
                        Locale.US,
                        "%s = %d",
                        ContentBundle.TypeColumn,
                        ContentBundle.Type.Archive.getValue()
                ),
                1
        );
        return bundles.isEmpty() ? null : bundles;
    }

    @Override
    public ContentBundle createBundle(ContentBundle bundle) throws SQLException {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            long id = db.insertOrThrow(ContentBundle.TableName, null, bundle.getContentValues());
            Cursor cursor = db.query(ContentBundle.TableName, ContentBundle.Columns, String.format(Locale.US, "%s = %d", ContentBundle.IdColumn, id), null, null, null, null);
            ContentBundle result;
            try {
                result = cursor.moveToNext() ? new ContentBundle(cursor) : null;
            } finally {
                cursor.close();
            }

            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void updateBundle(ContentBundle bundle) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.update(ContentBundle.TableName, bundle.getContentValues(), String.format(Locale.US, "%s = %d", ContentBundle.IdColumn, bundle.getId()), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteBundle(ContentBundle bundle, boolean recursive) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            if (recursive) {
                //
                // delete the associated file
                for (FileMeta meta : getBundleFileMetas(db, bundle.getId())) {
                    final String path = meta.getPath();
                    deleteFileMeta(db, meta.getId());

                    File file = new File(path);
                    if (file.exists() && !file.delete()) {
                        meta.droptTtl();
                        meta.setBundleId(null);
                        createOrMergeFileMeta(db, meta);
                        Logger.w(TAG, "File is NOT deleted: (will be delete later)" + path);
                    }
                }
            } else {
                db.execSQL(
                        String.format(
                                Locale.US,
                                "update %s set %s=null where %s=%d",
                                FileMeta.TableName,
                                FileMeta.BundleIdColumn,
                                FileMeta.BundleIdColumn,
                                bundle.getId()
                        )
                );
            }
            db.delete(ContentBundle.TableName, String.format(Locale.US, "%s = %d", ContentBundle.IdColumn, bundle.getId()), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public ContentBundle getSuperBundle() {
        List<ContentBundle> bundles = getBundles(
                String.format(Locale.US, "%s = %d", ContentBundle.TypeColumn, ContentBundle.Type.SuperJson.getValue()),
                1
        );
        return bundles.isEmpty() ? null : bundles.get(0);
    }

    @Override
    public FileMeta getFileMetaByUrl(String url) {
        long hash = getHashCode(url);
        List<FileMeta> metas = getFileMetas(String.format(Locale.US, "%s = %d", FileMeta.HashColumn, hash), null, null);
        return metas.isEmpty() ? null : metas.get(0);
    }

    @Override
    public FileMeta getFileMetaById(long id) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            FileMeta result = getFileMetaById(db, id);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public FileMeta getFileMetaById(SQLiteDatabase db, long id) {
        List<FileMeta> fileMetas = getFileMetas(db, String.format(Locale.US, "%s = %d", FileMeta.IdColumn, id), null, "1");
        return fileMetas.isEmpty() ? null : fileMetas.get(0);
    }

    FileMeta getFileMetaByHash(long hash) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            FileMeta result = getFileMetaByHash(db, hash);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private FileMeta getFileMetaByHash(SQLiteDatabase db, long hash) {
        List<FileMeta> fileMetas = getFileMetas(db, String.format(Locale.US, "%s = %d", FileMeta.HashColumn, hash), null, "1");
        return fileMetas.isEmpty() ? null : fileMetas.get(0);
    }

    @Override
    public List<FileMeta> getFileMetas(@NonNull String selection, @Nullable String orderBy, @Nullable String limit) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<FileMeta> result = getFileMetas(db, selection, orderBy, limit);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public long getTotalUserArticlesByStatusType() {
        SQLiteDatabase db = getDb();
        return DatabaseUtils.queryNumEntries(db, FileMetaUserArticle.TableName, null);
    }

    private List<FileMeta> getFileMetas(SQLiteDatabase db, String selection, @Nullable String orderBy, @Nullable String limit) {
        Cursor cursor = db.query(FileMeta.TableName, FileMeta.Columns, selection, null, null, null, orderBy, limit);
        try {
            ArrayList<FileMeta> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                result.add(new FileMeta(cursor));
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    @Override
    public FileMeta createFileMeta(FileMeta meta, String url, byte[] data) throws IOException {
        long hash = getHashCode(url);
        File file = new File(getPathByHash(getContext(), hash));
        File parent = file.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                return null;
            }
        }
        String path = file.getPath();
        meta.setPath(path);
        long size = writeFile(path, data);
        if (size == 0) {
            return null;
        }
        FileMeta newMeta = createOrMergeFileMeta(meta);
        newMeta.setClientDate(System.currentTimeMillis());
        updateFileMeta(newMeta);
        return newMeta;
    }

    @Override
    public FileMeta createOrMergeFileMeta(FileMeta meta) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            FileMeta result = createOrMergeFileMeta(db, meta);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private FileMeta createOrMergeFileMeta(SQLiteDatabase db, FileMeta meta) {
        long id = -1;
        FileMeta existing = getFileMetaByHash(db, meta.getHash());
        if (existing != null) {
            meta.setStatus(existing.getStatus());
            db.update(FileMeta.TableName, meta.getContentValues(), String.format(Locale.US, "%s = %d", FileMeta.IdColumn, existing.getId()), null);
            id = existing.getId();
        } else {
            try {
                id = db.insert(FileMeta.TableName, null, meta.getContentValues());
            } catch (SQLiteConstraintException e) {
                Logger.w(TAG, "Conflict inserting: " + meta.getPath() + "\n" + Utils.exceptionToString(e));
            }
        }

        if (id < 0) {
            // if we were unable to create the record - update the existing one
            existing = getFileMetaByHash(db, meta.getHash());
            if (existing == null) {
                Logger.e(TAG, "Unable to create metadata record for file: " + meta.getPath());
                id = db.insertWithOnConflict(FileMeta.TableName, null, meta.getContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
            } else {
                id = existing.getId();
            }
        }

        return id < 0 ? null : getFileMetaById(db, id);
    }

    @Override
    public void updateFileMeta(FileMeta meta) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.update(FileMeta.TableName, meta.getContentValues(), String.format(Locale.US, "%s = %d", FileMeta.IdColumn, meta.getId()), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void updateFileMeta(FileMeta meta, byte[] data) throws IOException {
        writeFile(meta.getPath(), data);
        meta.setClientDate(System.currentTimeMillis());
        updateFileMeta(meta);
    }

    @Override
    public void deleteFileMeta(long id) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            deleteFileMeta(db, id);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void deleteFileMeta(SQLiteDatabase db, long id) {
        db.delete(FileMeta.TableName, String.format(Locale.US, "%s = %d", FileMeta.IdColumn, id), null);
    }

    private List<FileMeta> getBundleFileMetas(SQLiteDatabase db, long id) {
        return getFileMetas(db, String.format(Locale.US, "%s = %d", FileMeta.BundleIdColumn, id), null, null);
    }

    @Override
    public int getRecentSectionsSize() {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        int size = -1;
        try {
            Cursor cursor = db.query(RecentSection.TableName, RecentSection.Columns, null, null, null, null, null, null);
            try {
                size = cursor.getCount();
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
        return size;
    }

    @Override
    public List<RecentSection> getRecentSections() {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        List<RecentSection> result = new ArrayList<>();
        try {
            Cursor cursor = db.query(RecentSection.TableName, RecentSection.Columns, null, null, null, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    do {
                        RecentSection recentSection = new RecentSection(cursor);
                        result.add(recentSection);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
        return result;
    }

    @Override
    public void updateRecentSections(List<RecentSection> recentSections) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            for (RecentSection recentSection : recentSections) {
                if (recentSection.isDeleteUpdateStatus()) {
                    if (SECTION_TYPE_AUTHOR.equals(recentSection.getSectionType())) {
                        db.delete(RecentSection.TableName, String.format(Locale.US, "%s = '%s'", RecentSection.MenuItemId, recentSection.getMenuItemId()), null);
                    } else {
                        int id = db.delete(RecentSection.TableName, String.format(Locale.US, "%s = %d", RecentSection.IdColumn, recentSection.getId()), null);
                    }
                }
                if (recentSection.isInsertUpdateStatus()) {
                    long id = db.insert(RecentSection.TableName, null, recentSection.getContentValues());
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<NotificationData> getNotifications() {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        List<NotificationData> result = new ArrayList<>();
        try {
            Cursor cursor = db.query(
                    NotificationTable.Name,
                    NotificationTable.ColumnNames,
                    null, null, null, null, null, null
            );

            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.add(NotificationTable.create(cursor));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
        return result;
    }

    @Override
    public NotificationData getNotification(int index) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        List<NotificationData> result = new ArrayList<>();
        try {
            Cursor cursor = db.query(
                    NotificationTable.Name,
                    NotificationTable.ColumnNames,
                    null, null, null, null, null, null
            );

            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.add(NotificationTable.create(cursor));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
        if (result.size() == index) {
            return result.get(index - 1 < 0 ? 0 : index - 1);
        }
        return result.get(index);
    }

    @Override
    public NotificationData readNotification(int id){
        NotificationData notification;
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {

            Cursor cursor = db.query(NotificationTable.Name, NotificationTable.ColumnNames, String.format(Locale.US, "%s = %d", NotificationTable.NotifIdColumn, id), null, null, null, null);

            try {
                notification = cursor.moveToNext() ? NotificationTable.create(cursor) : null;
                if (notification != null){
                    notification.setRead(true);
                    updateNotification(notification);
                }
                db.setTransactionSuccessful();
                return notification;
            } finally {
                cursor.close();
            }

        } finally {
            db.endTransaction();
        }

    }

    @Override
    public void updateNotifications(@NonNull List<NotificationData> notifications) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            for (NotificationData notification : notifications) {
                NotificationTable notificationTable = new NotificationTable();

                db.update(
                        NotificationTable.Name,
                        notificationTable.createContentValues(notification),
                        String.format(Locale.US, "%s = %d", NotificationTable.IdColumn, notification.getId()),
                        null
                );
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void updateNotification(@NonNull NotificationData notification){
        SQLiteDatabase db = getDb();
        db.beginTransaction();

        try {
            NotificationTable notificationTable = new NotificationTable();

            db.update(NotificationTable.Name, notificationTable.createContentValues(notification),
                    String.format(Locale.US, "%s = %d", NotificationTable.NotifIdColumn, Integer.valueOf(notification.getNotifId())),
                    null);

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteNotifications(@NonNull List<NotificationData> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(NotificationTable.IdColumn).append(" IN (");
        boolean isFirst = true;
        for (NotificationData notification : notifications) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }

            sb.append(Long.toString(notification.getId()));
        }
        sb.append(")");

        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.delete(NotificationTable.Name, sb.toString(), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    @Override
    public void addNotification(NotificationData notification) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            NotificationTable notificationTable = new NotificationTable();
            notificationTable.createContentValues(notification);

            db.insert(NotificationTable.Name, null, notificationTable.createContentValues(notification));

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteNotification(NotificationData notification) {
        StringBuilder sb = new StringBuilder();
        sb.append(NotificationTable.IdColumn).append(" IN (");
        sb.append(Long.toString(notification.getId()));
        sb.append(")");

        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.delete(NotificationTable.Name, sb.toString(), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    List<Archive> getArchivesByLabel(long label) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<Archive> list = getArchives(db, String.format(Locale.US, "%s = %d", Archive.DateColumn, label));
            db.setTransactionSuccessful();
            return list;
        } finally {
            db.endTransaction();
        }
    }

    Archive getArchiveByLabelAndSection(long label, String sectionLetter) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<Archive> list = getArchives(db, String.format(Locale.US, "%s = %d AND %s = %s", Archive.DateColumn, label, Archive.SectionColumn, DatabaseUtils.sqlEscapeString(sectionLetter)));
            Archive result = list.isEmpty() ? null : list.get(0);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    Archive createArchive(Archive a) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            long id = db.replace(Archive.TableName, null, a.getContentValues());
            Cursor cursor = db.query(Archive.TableName, Archive.Columns, String.format(Locale.US, "%s = %d", Archive.IdColumn, id), null, null, null, Archive.DateColumn + " desc", "1");
            Archive result;

            try {
                result = cursor.moveToFirst() ? new Archive(cursor) : null;
            } finally {
                cursor.close();
            }

            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    Archive getArchiveById(long id) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<Archive> list = getArchives(db, String.format(Locale.US, "%s = %d", Archive.IdColumn, id));
            Archive result = list.isEmpty() ? null : list.get(0);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    Archive getArchiveByDownloadId(long id) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<Archive> list = getArchives(db, String.format(Locale.US, "%s = %d", Archive.DownloadIdColumn, id));
            Archive result = list.isEmpty() ? null : list.get(0);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    List<Archive> getArchives(String query) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            List<Archive> result = getArchives(db, query);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private List<Archive> getArchives(SQLiteDatabase db, String query) {
        List<Archive> result = new ArrayList<Archive>();
        Cursor cursor = db.query(Archive.TableName, Archive.Columns, query, null, null, null, Archive.DateColumn + " desc");
        ;
        try {
            while (cursor.moveToNext()) {
                result.add(new Archive(cursor));
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    void deleteArchive(long id) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.delete(Archive.TableName, String.format(Locale.US, "%s = %d", Archive.IdColumn, id), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void updateArchive(Archive archive) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.update(Archive.TableName, archive.getContentValues(), String.format(Locale.US, "%s = %d", Archive.IdColumn, archive.getId()), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void wipeArchives() {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.delete(Archive.TableName, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void dropFileMetaSoftTtl(String url) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE "+FileMeta.TableName+" SET "+FileMeta.ExpiredColumn+" = 0 WHERE "+FileMeta.UrlColumn+" = ?", new String[] { url });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void dropFileMeta(String url) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            long hash = getHashCode(url);
            db.delete(FileMeta.TableName, String.format(Locale.US, "%s = %d", FileMeta.HashColumn, hash), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Entry get(String url) {
        try {
            FileMeta fm = getFileMetaByUrl(url);
            if (fm != null) {
                File file = new File(fm.getPath());
                if (!file.exists()) {
                    return null;
                }
                fm.updateTtl();
                updateFileMeta(fm);
                if (isImage(fm)) {
                    return new ImageEntry(fm);
                }
                return new CacheEntry(fm);
            }
            return null;
        } catch (IOException e) {
            Logger.w(TAG, Utils.exceptionToString(e));
            return null;
        }
    }

    private boolean isImage(FileMeta fm) {
        if (fm == null) {
            return false;
        }
        String contentType = fm.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.equals("image/png") ||
                contentType.equals("image/jpeg") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/gif");
    }

    @Override
    public void put(String url, Entry entry) {
        FileMeta fm = getFileMetaByUrl(url);
        if (fm == null) {
            long hash = getHashCode(url);
            String path = getPathByHash(_context, hash);
            String[] contentType = new String[2];
            Utils.parseContentType(entry.responseHeaders.get("Content-Type"), contentType);

            fm = new FileMeta(null, path, url, hash, contentType[0], contentType[1], entry.serverDate, entry.lastModified);
        } else {
            fm.setServerDate(entry.serverDate);
        }
        fm.setExpired(entry.softTtl);
        fm.updateTtl();
        fm.setEntityTag(entry.etag);
        fm.setLastModified(entry.lastModified);
        try {
            if (fm.isNew()) {
                createFileMeta(fm, url, entry.data);
            } else {
                updateFileMeta(fm, entry.data);
            }
        } catch (IOException e) {
            Logger.e(TAG, Utils.exceptionToString(e));
        }
    }

    @Override
    public void initialize() {
    }

    @Override
    public void invalidate(String key, boolean fullExpire) {
    }

    @Override
    public void remove(String url) {
    }

    @Override
    public void clear() {
    }

    class CacheEntry extends Entry {
        private final FileMeta _fileMeta;

        public CacheEntry(FileMeta fm) throws IOException {
            _fileMeta = fm;
            InputStream is = new FileInputStream(_fileMeta.getPath());
            try {
                this.data = inputStreamToByteArray(is);
            } catch (OutOfMemoryError e) {
                this.data = new byte[0];
                Logger.e(TAG, Utils.exceptionToString(e));
            } finally {
                is.close();
            }

            this.etag = fm.getEntityTag();
            this.responseHeaders = new HashMap<String, String>();
            if (fm.getContentType() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(fm.getContentType());
                if (fm.getEncoding() != null) {
                    sb.append("; charset=").append(fm.getEncoding());
                }
                this.responseHeaders.put("Content-Type", sb.toString());
            }
            if (fm.getEntityTag() != null) {
                this.responseHeaders.put("ETag", fm.getEntityTag());
            }
            this.responseHeaders.put("Date", DateUtils.formatDate(new Date(fm.getServerDate())));

            this.serverDate = _fileMeta.getServerDate();
            this.softTtl = _fileMeta.getExpired();
            this.ttl = _fileMeta.getTtl();
            this.lastModified = _fileMeta.getLastModified();
        }

        @Override
        public boolean refreshNeeded() {
            return _fileMeta.shouldRefresh();
        }

        @Override
        public boolean isExpired() {
            return ttl < System.currentTimeMillis();
        }

        private byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return inputStream.readAllBytes();
            } else {
                // Rely on a buffered stream if not already buffered
                InputStream stream = inputStream instanceof BufferedInputStream ?
                        inputStream : new BufferedInputStream(inputStream);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buff = new byte[8192];
                int len;
                // Always check != -1 for EOF
                while ((len = stream.read(buff)) != -1) {
                    out.write(buff, 0, len);
                }
                return out.toByteArray();
            }
        }
    }

    class ImageEntry extends Entry {

        private final FileMeta _fileMeta;

        public ImageEntry(FileMeta fm) throws IOException {
            _fileMeta = fm;
            this.data = _fileMeta.getPath().getBytes();
            this.etag = fm.getEntityTag();
            this.responseHeaders = new HashMap<>();
            if (fm.getContentType() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(fm.getContentType());
                if (fm.getEncoding() != null) {
                    sb.append("; charset=").append(fm.getEncoding());
                }
                this.responseHeaders.put("Content-Type", sb.toString());
                this.responseHeaders.put("Volley-Location", "disk");
            }
            if (fm.getEntityTag() != null) {
                this.responseHeaders.put("ETag", fm.getEntityTag());
            }
            this.responseHeaders.put("Date", DateUtils.formatDate(new Date(fm.getServerDate())));

            this.serverDate = _fileMeta.getServerDate();
            this.softTtl = _fileMeta.getExpired();
            this.ttl = _fileMeta.getTtl();
            this.lastModified = _fileMeta.getLastModified();
        }
    }
}