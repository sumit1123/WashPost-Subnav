package com.washingtonpost.android.wapocontent;

public class ImageResponse {
    private final Object data;
    private final String key;
    private final String url;

    public ImageResponse(String url, String key, Object data) {
        this.url = url;
        this.key = key;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public String getKey() {
        return key;
    }

    public String getUrl() {
        return url;
    }
}
