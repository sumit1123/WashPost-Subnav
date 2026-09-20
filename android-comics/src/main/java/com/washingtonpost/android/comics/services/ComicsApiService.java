package com.washingtonpost.android.comics.services;


import com.washingtonpost.android.comics.model.ComicStrip;

import java.util.Date;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by muppallav on 4/12/16.
 */
public interface ComicsApiService {
    @GET("/api/v1/comics?platform=native")
    Call<Map<Date, List<ComicStrip>>> getComics(@Query("from") String fromDate, @Query("to") String toDate);

    @GET("/api/v1/comics/{comicID}?platform=native")
    Call<Map<Date, ComicStrip>> getSpecificComics(@Path("comicID") String comicID, @Query("from") String fromDate, @Query("to") String toDate);

    @GET("/api/v1/comics/{comicID}?platform=native")
    Observable<Map<Date, ComicStrip>> getSpecificComicsObs(@Path("comicID") String comicID, @Query("from") String fromDate, @Query("to") String toDate);

    @GET("/api/v1/comics?platform=native")
    Observable<Map<Date, List<ComicStrip>>> getComicsObservable(@Query("from") String fromDate, @Query("to") String toDate);
}
