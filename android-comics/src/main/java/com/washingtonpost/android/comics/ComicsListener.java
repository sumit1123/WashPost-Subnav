package com.washingtonpost.android.comics;

import com.washingtonpost.android.comics.model.ComicStrip;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by muppallav on 4/13/16.
 */
public interface ComicsListener {
    void onSuccess(Map<Date, List<ComicStrip>> comicsMap);

    void onError(String errorMessage);
}
