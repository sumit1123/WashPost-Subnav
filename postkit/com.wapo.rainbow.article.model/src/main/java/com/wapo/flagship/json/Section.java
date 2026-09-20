package com.wapo.flagship.json;

import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Section {
    private static final String MANIFEST = "manifest";
    private static final String NAME = "name";
    private static final String BUNDLE = "bundle";
    private static final String FRONT = "front";
    private static final String ARTICLES = "articles";

    private final String _name;
    private final transient String _nameCompat;
    private final ResourceManifest _manifest;
    private final ResourceManifest _bundle;
    private final ResourceManifest _front;
    private final List<ResourceManifest> _articles;

    public Section(
            String name,
            ResourceManifest manifest,
            ResourceManifest bundle,
            ResourceManifest front,
            List<ResourceManifest> articles
    ) {
        _name = name;
        _manifest = manifest;
        _bundle = bundle;
        _front = front;
        _articles = articles;

        //
        // new (iOs) bundles use names without '.json' suffix and, by some historical reasons,
        // old android bundles uses names with '.json' suffix. So, for consistency, adding it if there is no one.
        _nameCompat = _name == null || _name.endsWith(".json") ? _name : _name + ".json";
    }

    public static Section parseJson(String json) throws JSONException, ParseException {
        return Section.parseJson(new JSONObject(json));
    }

    public static Section parseJson(JSONObject jsonObj) throws JSONException, ParseException {
        String name = jsonObj.getString(NAME);

        ResourceManifest manifest = jsonObj.isNull(MANIFEST) ? null : ResourceManifest.parseJson(jsonObj.getJSONObject(MANIFEST));
        ResourceManifest bundle = jsonObj.isNull(BUNDLE) ? null : ResourceManifest.parseJson(jsonObj.getJSONObject(BUNDLE));
        ResourceManifest front = jsonObj.isNull(FRONT) ? null : ResourceManifest.parseJson(jsonObj.getJSONObject(FRONT));

        List<ResourceManifest> articles = new ArrayList<>();

        if (!jsonObj.isNull(ARTICLES)) {
            JSONArray arr = jsonObj.getJSONArray(ARTICLES);
            int len = arr.length();
            for (int i = 0; i < len; i++) {
                articles.add(ResourceManifest.parseJson(arr.getJSONObject(i)));
            }
        }

        return new Section(name, manifest, bundle, front, articles);
    }

    public String getName() {
        return _name;
    }

    public String getCompatName() {
        return _nameCompat;
    }

    public ResourceManifest getManifest() {
        return _manifest;
    }

    public ResourceManifest getBundle() {
        return _bundle;
    }

    public ResourceManifest getFront() {
        return _front;
    }

    public List<ResourceManifest> getArticles() {
        return _articles == null ? Collections.<ResourceManifest>emptyList() : _articles;
    }
}
