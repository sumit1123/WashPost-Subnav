package com.captechconsulting.captechbuzz.model.images;

import android.graphics.Bitmap;

import androidx.collection.LruCache;

import com.washingtonpost.android.volley.toolbox.ImageLoader.ImageCache;

import org.jetbrains.annotations.NotNull;

/**
 * Basic LRU Memory cache.
 *
 * @author Trey Robinson
 */
public class BitmapLruImageCache extends LruCache<String, Bitmap> implements ImageCache {

    private final String TAG = this.getClass().getSimpleName();

    public BitmapLruImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(@NotNull String key, @NotNull Bitmap value) {
        return value.getByteCount() / 1024;
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url, bitmap);
    }
}
