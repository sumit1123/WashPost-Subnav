package com.washingtonpost.android.comics.model;

import android.graphics.Point;
import com.wapo.android.commons.util.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.washingtonpost.android.comics.ComicsService;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by muppallav
 */
public class ComicsDeserializer implements JsonDeserializer<ComicStrip> {

    private static final String TAG = ComicsDeserializer.class.getName();
    private final Point point;

    public ComicsDeserializer(Point point) {
        this.point = point;
    }

    @Override
    public ComicStrip deserialize(JsonElement elem, Type type, JsonDeserializationContext context) throws JsonParseException {
        while (elem instanceof JsonArray) {
            JsonArray a = (JsonArray) elem;
            elem = a.size() > 0 ? a.get(0) : null;
        }
        ComicStrip comicStrip = null;

        try {
            if (elem != null) {
                JsonObject obj = elem.getAsJsonObject();
                comicStrip = new ComicStrip();
                comicStrip.setAuthor(obj.has("author") ? obj.get("author").getAsString() : null);
                comicStrip.setId(obj.has("_id") ? obj.get("_id").getAsString() : null);
                if (comicStrip.getId() == null) {
                    return null;
                }
                comicStrip.setName(obj.has("name") ? obj.get("name").getAsString() : "");
                comicStrip.setProvider(obj.has("provider") ? obj.get("provider").getAsString() : null);
                comicStrip.setPublished(obj.has("published") ? formatDate(obj.get("published").getAsString()) : null);
                if (obj.has("formats")) {
                    JsonObject formatObj = obj.get("formats").getAsJsonObject();
                    comicStrip= getAssignedFormat(formatObj,comicStrip);
                }
            }
            if (comicStrip == null || comicStrip.getId() == null || comicStrip.getUrl() == null) {
                return null;
            } else {
                return comicStrip;
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed parsing: \"" + (elem == null ? "null" : elem.toString()) + "\"", e);
            return null;
        } catch (IncompatibleClassChangeError e) {
            Logger.e(TAG, "Failed parsing: \"" + (elem == null ? "null" : elem.toString()) + "\"", e);
            return null;
        }
    }

    private Date formatDate(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        try {
            return dateString != null ? dateFormat.parse(dateString) : null;
        } catch (ParseException e) {
            Logger.e(TAG, "Failed parsing: \"" + (dateString == null ? "null" : dateString.toString()) + "\"", e);
            return null;
        }
    }

    private ComicStrip getAssignedFormat(JsonObject formatObj, ComicStrip strip){
        final int screenWidth = point.x;
        JsonObject stripObj=null;
        JsonObject result = null;

        for (String s: ComicsService.BUNDLE_CONFIG) {
            if(formatObj.has(s)){
                 stripObj= formatObj.getAsJsonObject(s);
                 if(stripObj.has("width")){
                     if(stripObj.get("width").getAsInt() >= screenWidth){
                         result= stripObj;
                         break;
                     }else{
                         result= stripObj;
                     }
                 }
            }
        }
        if(result!=null){
            strip.setWidth(stripObj.has("width") ? stripObj.get("width").getAsInt() : 0);
            strip.setHeight(stripObj.has("height") ? stripObj.get("height").getAsInt() : 0);
            strip.setUrl(stripObj.has("url") ? stripObj.get("url").getAsString() : null);
        }
        return strip;
    }

}
