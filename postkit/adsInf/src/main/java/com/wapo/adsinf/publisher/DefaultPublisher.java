package com.wapo.adsinf.publisher;

import android.content.Context;

import com.wapo.adsinf.R;

public class DefaultPublisher implements IPublisher {

    private Context context;

    public DefaultPublisher(Context context) {
        this.context = context;
    }

    @Override
    public String getAdUnitId() {
        return context.getString(R.string.ad_unit_id);
    }

    @Override
    public String getPublisherId() {
        return context.getString(R.string.ad_publisher_id);
    }

    @Override
    public String getAdKeyRootPathForMob() {
        return context.getString(R.string.ad_key_mob_root_path);
    }

    @Override
    public String getAdKeyRootPathForTab() {
        return context.getString(R.string.ad_key_tab_root_path);
    }

    @Override
    public String getAdKey() {
        return context.getString(R.string.ad_key);
    }
}
