package com.wapo.flagship.config;

/**
 * Created by curacamalitod on 12/4/17.
 */

import com.wapo.android.commons.config.BaseConfig;

public interface IConfigManager {
    BaseConfig getAppConfig();

    BaseConfig getSiteServiceConfig();

    BaseConfig getSectionsBarConfig();

    BaseConfig getSectionsFeaturedConfig();

    BaseConfig getSectionsAZConfig();

    BaseConfig getSectionsUnlistedConfig();

    BaseConfig getSectionsRecommendedConfig();

    BaseConfig getElectionConfig();
}
