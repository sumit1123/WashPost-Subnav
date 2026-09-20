package com.washingtonpost.android.comics;

import com.washingtonpost.android.comics.model.ComicStrip;

import java.util.Date;
import java.util.Map;

/**
 * Created by elamgodilj on 4/26/16.
 */
public interface ComicsSpecificDatesLoadListener {

    void onSuccess(Map<Date, ComicStrip> comicsMap);

    void onError(String errorMessage);
}
