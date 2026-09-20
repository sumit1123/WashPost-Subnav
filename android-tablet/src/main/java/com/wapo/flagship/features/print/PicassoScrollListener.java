package com.wapo.flagship.features.print;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.wapo.android.commons.util.Logger;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

/**
 * Created by curacamalitod on 7/6/17.
 */

public class PicassoScrollListener extends RecyclerView.OnScrollListener {
    private final Context context;

    private static final String TAG = PicassoScrollListener.class.getName();

    public PicassoScrollListener(Context context) {
        this.context = context;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        final Picasso picasso = Picasso.get();
        if (newState == SCROLL_STATE_IDLE) {
            Logger.d(TAG, "Resuming picasso load.");
            picasso.resumeTag(context);
        } else {
            picasso.pauseTag(context);
            Logger.d(TAG, "Pausing picasso load.");
        }
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    }
}

