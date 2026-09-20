package com.wapo.flagship.json;

import java.io.Serializable;

public class AdSetConfig implements Serializable {

    private String adSetZone;
    private AdSetUrls adSetUrls;

    public String getAdSetZone() {
        return adSetZone;
    }

    public AdSetUrls getAdSetUrls() {
        return adSetUrls;
    }
}
