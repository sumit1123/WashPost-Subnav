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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.wapo.flagship.content.notifications.NotificationData;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class CacheManager implements com.washingtonpost.android.volley.Cache {
    public static long getHashCode(String url) {
        int i = url == null ? -1 : url.indexOf('#');
        long hash = (i < 0 ? url : url.substring(0, i)).hashCode();
        hash = hash & 0x00000000ffffffffL;
        assert hash != 0;
        return hash;
    }

    public abstract SQLiteDatabase getDb();

    public abstract File getDatabasePath(String name);

    public abstract ContentBundle getSuperBundle();

    public abstract ContentBundle getBundleByName(String name);

    public abstract ContentBundle getBundleById(long id);

    public abstract List<ContentBundle> getBundles(String query);

    public abstract List<ContentBundle> getBundles(String query, int limit);

    public abstract ContentBundle getArchiveBundleByLabel(long label);

    public abstract List<ContentBundle> getArchiveBundles();

    public abstract ContentBundle createBundle(ContentBundle bundle) throws SQLException;

    public abstract void updateBundle(ContentBundle bundle);

    public abstract void deleteBundle(ContentBundle bundle, boolean recursive);

    public abstract FileMeta getFileMetaByUrl(String url);

    public abstract FileMeta getFileMetaById(long id);

    public abstract FileMeta getFileMetaById(SQLiteDatabase db, long id);

    public abstract List<FileMeta> getFileMetas(@NonNull String selection, @Nullable String orderBy, @Nullable String limit);

    public abstract long getTotalUserArticlesByStatusType();

    public abstract File getTargetDir();

    public abstract FileMeta createFileMeta(FileMeta meta, String url, byte[] data) throws IOException;

    public abstract FileMeta createOrMergeFileMeta(FileMeta meta);

    public abstract void updateFileMeta(FileMeta meta);

    public abstract void updateFileMeta(FileMeta meta, byte[] data) throws IOException;

    public abstract void deleteFileMeta(long id);

    public abstract List<RecentSection> getRecentSections();

    public abstract void updateRecentSections(List<RecentSection> recentSections);

    public abstract int getRecentSectionsSize();

    public abstract List<NotificationData> getNotifications();

    public abstract NotificationData readNotification(int id);

    public abstract NotificationData getNotification(int index);

    public abstract void addNotification(NotificationData notification);

    public abstract void deleteNotification(NotificationData notification);

    public abstract void cleanUp();

    abstract Context getContext();

    abstract List<Archive> getArchivesByLabel(long label);

    abstract Archive getArchiveByLabelAndSection(long label, String sectionLetter);

    abstract Archive createArchive(Archive a);

    abstract Archive getArchiveById(long id);

    abstract Archive getArchiveByDownloadId(long id);

    abstract List<Archive> getArchives(String query);

    abstract void deleteArchive(long id);

    abstract void updateArchive(Archive archive);

    abstract void wipeArchives();

    abstract FileMeta getFileMetaByHash(long hash);

    public abstract String getPathByUrl(String url);

    public abstract String getPathByHash(long hash);

    public abstract void updateNotifications(@NonNull List<NotificationData> notifications);

    public abstract void updateNotification(@NonNull NotificationData notification);

    public abstract void deleteNotifications(@NonNull List<NotificationData> notifications);

    public abstract void dropFileMetaSoftTtl(String url);

    public abstract void dropFileMeta(String url);
}
