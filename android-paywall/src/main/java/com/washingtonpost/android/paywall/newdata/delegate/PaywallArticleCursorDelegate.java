package com.washingtonpost.android.paywall.newdata.delegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.database.Cursor;

import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.helper.PaywallDbHelper;
import com.washingtonpost.android.paywall.newdata.model.ArticleStub;

public class PaywallArticleCursorDelegate extends CursorDelegate<ArticleStub> {
    static final String TAG = PaywallArticleCursorDelegate.class.getSimpleName();

    public PaywallArticleCursorDelegate(Cursor cursor) {
        super(cursor);
    }

    @Override
    public ArticleStub getObject() {
        ArticleStub article = new ArticleStub();
        article.setUrl(getString(PaywallDbHelper.PW_ARTICLE_LINK));
        article.setTitle(getString(PaywallDbHelper.PW_ARTICLE_TITLE));
        article.setTimeStamp(getLong(PaywallDbHelper.PW_ARTICLE_TIME));
        return article;
    }

    @Override
    public List<ArticleStub> getObjectList() {
        List<ArticleStub> articleList = new ArrayList<ArticleStub>();
        if (cursor.moveToFirst()) {
            do {
                articleList.add(getObject());
            } while (cursor.moveToNext());
        }
        return articleList;
    }

    // Key must be of type string in the cursor for now
    public HashMap<String, ArticleStub> getObjectMap(String key) {
        HashMap<String, ArticleStub> articleMap = new HashMap<String, ArticleStub>();
        if (cursor.moveToFirst()) {
            do {
                articleMap.put(getString(key), getObject());
            } while (cursor.moveToNext());
        }
        return articleMap;
    }

    public ArticleStub getSingle() {
        if(cursor.moveToFirst()) {
            return getObject();
        }
        return null;
    }

    public String getType() {
        if(cursor.moveToFirst()) {
            return getString(PaywallDbHelper.CATEGORY_CONTENT_TYPE);
        }
        return null;
    }

    public Long getArticleTime() {
        if(cursor.moveToFirst()) {
            return getLong(PaywallDbHelper.PW_ARTICLE_TIME);
        }
        return null;
    }

    public void close() {
        cursor.close();
    }
}