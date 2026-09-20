package com.wapo.text;

import android.content.Context;
import android.graphics.Typeface;
import androidx.collection.LruCache;
import android.text.TextUtils;
import com.wapo.android.commons.util.Logger;

public class TypefaceCache {

    private static final int CACHE_SIZE = 12;

    /**
     * An LruCache for previously loaded typefaces.
     */
    private static final LruCache<String, Typeface> mTypefaceCache = new LruCache<>(CACHE_SIZE);

    private static boolean isFirePhone = false;

    public static synchronized void configure(boolean isFirePhone) {
        TypefaceCache.isFirePhone = isFirePhone;
    }

    public static synchronized Typeface getTypeface(Context context, String typeFaceName) {
        if (typeFaceName == null || TextUtils.isEmpty(typeFaceName)) {
            return null;
        }

        Typeface typeface = mTypefaceCache.get(typeFaceName);
        if (typeface == null && !isFirePhone) {
            try {
                typeface = Typeface.createFromAsset(context.getAssets(), typeFaceName);
                //Cache the typeface object
                mTypefaceCache.put(typeFaceName, typeface);
            } catch (Exception e) {
                Logger.w("TypefaceCache", "Can not get typface: " + typeFaceName);
            }
        }

        return typeface;
    }

    public static void putTypeface(String fontName, Typeface typeface) {
        mTypefaceCache.put(fontName, typeface);
    }
}
