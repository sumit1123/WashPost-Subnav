package com.wapo.flagship.config;

import com.wapo.android.commons.config.BaseConfig;

/**
 * Created by curacamalitod on 12/4/17.
 */

public abstract class DefaultConfigManager implements IConfigManager {
    //TODO: This needs to be rewritten when we move Wapo's existing config into this new config format.
    @Override
    public abstract BaseConfig getAppConfig();

    @Override
    public SiteServiceConfig getSiteServiceConfig() {
        return null;
    }

    @Override
    public SiteServiceConfig getSectionsBarConfig() {
        return null;
    }

    @Override
    public SiteServiceConfig getSectionsFeaturedConfig() {
        return null;
    }

    @Override
    public SiteServiceConfig getSectionsAZConfig() {
        return null;
    }

    @Override
    public SiteServiceConfig getSectionsUnlistedConfig() {
        return null;
    }

    @Override
    public SiteServiceConfig getSectionsRecommendedConfig() {
        return null;
    }

    public SiteServiceConfig getSectionsBarTestConfig() {
        return null;
    }

    public SiteServiceConfig getElectionConfig() {return null;}
}
