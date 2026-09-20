package com.washingtonpost.android.volley;

public interface BypassCachePolicy {

    boolean shouldBypassCache(Cache.Entry entry);
}
