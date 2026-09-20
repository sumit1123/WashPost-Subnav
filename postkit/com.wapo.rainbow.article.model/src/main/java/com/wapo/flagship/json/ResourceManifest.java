package com.wapo.flagship.json;

import android.net.Uri;
import android.text.TextUtils;
import com.wapo.android.commons.util.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ResourceManifest {
    private static final String LMT = "lmt";
    private static final String URL = "url";
    private static final String SIZE = "size";
    private static final String CONTENT_TYPE = "contentType";
    private static final String HEADLINE = "headline";
    private static final String FILE_PATH = "filePath";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    private final String url;
    private final long lmt;
    private final Long size;
    private final String contentType;
    private final String headline;
    private final String filePath;

    public static ResourceManifest parseJson(String json) throws JSONException, ParseException {
        return ResourceManifest.parseJson(new JSONObject(json));
    }

    public static ResourceManifest parseJson(JSONObject jobj) throws JSONException, ParseException {
        String url = jobj.getString(URL);
        long lmt = 0;
        try {
            lmt = jobj.isNull(LMT) ? 0 : Long.parseLong(jobj.getString(LMT));//sdf.parse(jobj.getString(LMT));
        } catch (NumberFormatException e) {
            try {
                lmt = jobj.isNull(LMT) ? 0 : sdf.parse(jobj.getString(LMT)).getTime();
            } catch (ParseException pe) {
                Logger.e(ResourceManifest.class.getSimpleName(), "Unable to parse date: " + jobj.getString(LMT));
            }
        }

        Long size = jobj.has(SIZE) ? (jobj.isNull(SIZE) ? null :jobj.getLong(SIZE)) : null;
        String contentType = jobj.getString(CONTENT_TYPE);
        String headline = jobj.isNull(HEADLINE) ? null : jobj.getString(HEADLINE);
        String filePath = jobj.isNull(FILE_PATH) ? null : jobj.getString(FILE_PATH);

        return new ResourceManifest(
                url, lmt, size, contentType, headline, filePath
        );
    }

    public ResourceManifest(String url, long lmt, Long size, String contentType, String headline, String filePath) {
        this.url = url;
        this.lmt = lmt;
        this.size = size;
        this.contentType = contentType;
        this.headline = headline;
        this.filePath = filePath;
    }

    public String getUrl() {
        return url;
    }

    public long getLmt() {
        return lmt;
    }

    public long getSize() {
        return size == null ? 0 : size;
    }

    public String getContentType() {
        return contentType;
    }

    public String getHeadline() {
        return headline;
    }

    public String getFilePath() {
        return filePath;
    }

    public ArrayList<String> getPath() {
        ArrayList<String> paths = new ArrayList<String>();
        Uri uri = Uri.parse(getUrl());
        String path = getFilePath();
        if (path == null) {
            String port=uri.getPort()!=-1?(":"+uri.getPort()):"";
            path = uri.getHost() + port + uri.getEncodedPath();
            String query = uri.getQuery();
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            if (!TextUtils.isEmpty(query)) {
                paths.add(path + "?" + query);
            }
        }
        paths.add(path.startsWith("/") ? path.substring(1) : path);
        return paths;
    }

    public boolean hasPath(String entryName) {
        for (String path : getPath()){
            if (entryName.equals(path))
                return true;
        }
        return false;
    }
}
