package com.washingtonpost.android.paywall.helper;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.newdata.delegate.PaywallArticleCursorDelegate;
import com.washingtonpost.android.paywall.newdata.model.ArticleStub;

import java.util.List;

import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ARTICLE_RULE1_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ARTICLE_RULE2_TABLE;


/**
 * Counter helper
 *
 * @author Bkilari
 */
public class PaywallCounterHelper {
    public static String TAG = PaywallCounterHelper.class.getSimpleName();

    /**
     * Insert read article to list and set [PW_TETRO_SYNCED] value to false
     * @param article
     * @param tableName
     */
    public static void insertArticle(ArticleStub article, String tableName) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = new ContentValues();
        args.put(PaywallDbHelper.PW_ARTICLE_LINK, article.getUrl());
        args.put(PaywallDbHelper.PW_ARTICLE_TITLE, article.getTitle());
        args.put(PaywallDbHelper.PW_TETRO_SYNCED, false);
        args.put(PaywallDbHelper.PW_ARTICLE_TIME, System.currentTimeMillis());
        db.insert(tableName, null, args);
    }

    /**
     * Update read article in list with [PW_TETRO_SYNCED] value to false
     * @param article
     * @param tableName
     */
    public static void updateArticle(ArticleStub article, String tableName) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = new ContentValues();
        args.put(PaywallDbHelper.PW_TETRO_SYNCED, false);
        String[] whereVars = new String[]{article.getUrl()};
        String where = PaywallDbHelper.PW_ARTICLE_LINK + " = ?";
        db.update(tableName, args, where, whereVars);
    }

    /**
     * Update all articles with [PW_TETRO_SYNCED] = false to true upon successful Tetro sync
     * @param tableName
     */
    public static void updateArticlesSynced(String tableName) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = new ContentValues();
        args.put(PaywallDbHelper.PW_TETRO_SYNCED, true);
        String[] whereVars = new String[]{"0"};
        String where = PaywallDbHelper.PW_TETRO_SYNCED + " = ?";
        db.update(tableName, args, where, whereVars);
    }

    /**
     * Return a list of articles that are not synced -> [PW_TETRO_SYNCED] = false
     * @param tableName
     * @return
     */
    public static List<ArticleStub> getArticleListNotSynced(String tableName){
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        String[] whereVars = new String[]{"0"};
        String where = PaywallDbHelper.PW_TETRO_SYNCED + " = ?";
        PaywallArticleCursorDelegate cursor = new PaywallArticleCursorDelegate(
                db.rawQuery("SELECT * from " + tableName
                        + " where " + where, whereVars));
        List<ArticleStub> article;
        try {
            article = cursor.getObjectList();
        } finally {
            cursor.close();
        }
        return article;
    }

    public static long insertArticleForRule1(ArticleStub article, int groupId) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = new ContentValues();
        args.put(PaywallDbHelper.PW_ARTICLE_LINK, article.getUrl());
        args.put(PaywallDbHelper.PW_ARTICLE_TITLE, article.getTitle());
        args.put(PaywallDbHelper.PW_ARTICLE_SECTION, article.getSection());
        args.put(PaywallDbHelper.PW_ARTICLE_GROUP_ID, groupId);

        return db.insert(PW_ARTICLE_RULE1_TABLE, null, args);
    }

    public static ArticleStub getArticleByUrl(String articleUrl, String tableName) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        String[] whereVars = new String[]{articleUrl};
        String where = PaywallDbHelper.PW_ARTICLE_LINK + " = ?";

        PaywallArticleCursorDelegate cursor = new PaywallArticleCursorDelegate(
                db.rawQuery("SELECT * from " + tableName
                        + " where " + where, whereVars));
        ArticleStub article = null;
        try {
            article = cursor.getSingle();
        } finally {
            cursor.close();
        }
        return article;
    }
    public static void removeArticleByUrl(String articleUrl, String tableName) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        try {
            db.delete(tableName, PaywallDbHelper.PW_ARTICLE_LINK + "=?", new String[]{articleUrl});
        } catch (Exception e) {
        }

    }

    public static void cleanArticles() {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        db.delete(PaywallDbHelper.PW_ARTICLE_TABLE, null, null);
        db.delete(PW_ARTICLE_RULE1_TABLE, null, null);
        db.delete(PW_ARTICLE_RULE2_TABLE, null, null);
    }

    public static void removeArticleByFrequency(long articleFrequency, String tableName) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        try {
            long articleTimeDifference = System.currentTimeMillis() - articleFrequency;
            db.delete(tableName, PaywallDbHelper.PW_ARTICLE_TIME + "<?", new String[]{String.valueOf(articleTimeDifference)});
        } catch (Exception e) {
        }
    }
}
