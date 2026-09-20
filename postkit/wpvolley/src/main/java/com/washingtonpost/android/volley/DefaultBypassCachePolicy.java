package com.washingtonpost.android.volley;

public class DefaultBypassCachePolicy implements BypassCachePolicy {
    @Override
    public boolean shouldBypassCache(Cache.Entry entry) {
        return false;
    }
}
