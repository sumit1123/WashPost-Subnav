package com.wapo.flagship.network.request;

import com.washingtonpost.android.volley.Cache;
import com.washingtonpost.android.volley.NetworkResponse;
import com.washingtonpost.android.volley.ParseError;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.JsonSyntaxException;
import com.wapo.flagship.json.LiveBlogFeed;

import java.io.UnsupportedEncodingException;

/**
 * Created by elamgodilj on 8/25/15.
 */
public class LiveBlogFeedRequest extends PolicyJsonRequest<LiveBlogFeed> {

    // To be moved to config.json once it is confirmed.
    public static final String _url = "https://grid.wpdigital.net/gridservice/grid_id/gql?q=$and:%5B%7B%22addedTimestamp%22:%7B%22$gt%22:timestamp%7D%7D,%7B%22onGrid%22:true%7D,%7B%22className%22:%22com.washingtonpost.webapps.grid.entities.MethodeContent%22%7D%5D&limit=3";
    public static final String GRID_ID = "grid_id";
    public static final String TIMESTAMP = "timestamp";
    public static final String LAST_UPDATED = "LAST UPDATED";

    public LiveBlogFeedRequest(String url, Response.Listener<Data<LiveBlogFeed>> listener, Response.ErrorListener errorListener) {
        this(Method.GET, url, listener, errorListener);
    }

    public LiveBlogFeedRequest(int method, String url, Response.Listener<Data<LiveBlogFeed>> listener, Response.ErrorListener errorListener) {
        super(method, url, Policy.Network, listener, errorListener);
    }

    public LiveBlogFeedRequest(int method, String url, Policy policy, Response.Listener<Data<LiveBlogFeed>> listener, Response.ErrorListener errorListener) {
        super(method, url, policy, listener, errorListener);
    }

    @Override
    protected Response<Data<LiveBlogFeed>> parseNetworkResponse(NetworkResponse response) {

        try {
            String jsonStr = new String(response.data, "UTF-8");
            LiveBlogFeed result = LiveBlogFeed.parseJson(jsonStr);
            Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);
            if (entry != null && entry.softTtl == 0) {
                entry.softTtl = System.currentTimeMillis() + 30000;
            }
            return Response.success(new Data<>(result, false, entry.serverDate), entry);
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException jse){
            return Response.error(new ParseError(new Exception("error while processing: " + this.getUrl(), jse)));
        }
    }
}
