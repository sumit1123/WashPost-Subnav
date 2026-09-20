package com.wapo.flagship.features.print;

import android.view.View;

/**
 * Created by curacamalitod on 6/28/17.
 */

interface ThumbnailClickListener {

    View.OnClickListener getThumbnailClickListener();
    int getPageNo();
}
