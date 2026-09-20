package com.wapo.flagship.network.request;

import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.JsonRequest;
import com.wapo.android.commons.util.Logger;

public abstract class PolicyJsonRequest<T> extends JsonRequest<PolicyJsonRequest.Data<T>> {
    private static final String TAG = "[cache]";
    private final Policy _policy;
    private boolean _cacheHit;
    private Data<T> _cachedResult;
    private boolean _isRefreshNeeded = false;

    public PolicyJsonRequest(int method, String url, Policy policy, Response.Listener<Data<T>> listener, Response.ErrorListener errorListener) {
        super(method, url, null, listener, errorListener);
        _policy = policy;
    }

    public Policy getPolicy() {
        return _policy;
    }

    @Override
    protected void deliverResponse(Data<T> data) {
        Logger.d(TAG, "delivering response: " + getUrl());
        //
        // if we returning case as fallback & and we hit the cache => remember the cached result & do not report;
        if (_policy == Policy.Network && _cacheHit && _cachedResult == null && _isRefreshNeeded) {
            _cachedResult = data;
            Logger.d(TAG, "remember cached response, do not notify; " + getUrl());
            return;
        }

        if (_policy == Policy.Cache && hasHadResponseDelivered()) {
            Logger.d(TAG, "response has been delivered: do not notify; " + getUrl());
            return;
        }

        super.markDelivered();
        super.deliverResponse(new Data<T>(data.response, _policy == Policy.Cache && _cacheHit, data.serverDate));
        Logger.d(TAG, "response delivered: " + getUrl());
        if (_policy == Policy.Cache) {
            cancel();
        }
    }

    @Override
    public void deliverError(VolleyError error) {
        if (_cachedResult != null) {
            Logger.d(TAG, "an error occurred: delivery cached data; " + getUrl());
            super.deliverResponse(new Data<T>(_cachedResult.response, true, _cachedResult.serverDate));
            return;
        }
        Logger.d(TAG, "delivery an error");
        super.deliverError(error);
    }

    @Override
    public void markDelivered() {
        Logger.d(TAG, String.format("markDelivered: %s", getUrl()));
    }

    @Override
    public void addMarker(String tag) {
        Logger.d(TAG, String.format("add marker: %s, url: %s", tag, getUrl()));
        if (!_cacheHit && tag != null && tag.startsWith("cache-hit")) {
            Logger.d(TAG, "mark cache hit: " + getUrl());
            _cacheHit = true;
        }

        if ("cache-hit-refresh-needed".equals(tag)) {
            Logger.d(TAG, "mark refresh needed: " + getUrl());
            _isRefreshNeeded = true;
        }

        super.addMarker(tag);
    }




    public enum Policy {
        Cache, Network
    }

    public static class Data<T> {
        public final boolean isCached;
        public final T response;
        public final long serverDate;

        public Data(T response, boolean isCahced, long serverDate) {
            this.response = response;
            this.isCached = isCahced;
            this.serverDate = serverDate;
        }
    }
}
