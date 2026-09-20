package com.wapo.flagship.content;

import android.content.Context;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.config.BaseConfig;
import com.wapo.android.commons.config.ConfigHelper;
import com.wapo.android.commons.config.Constants;
import com.wapo.android.domain.repository.RemoteLogRepo;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.config.DefaultConfigManager;
import com.wapo.flagship.config.SiteServiceConfig;
import com.wapo.flagship.util.UIUtil;
import com.washingtonpost.android.BuildConfig;
import com.washingtonpost.android.R;
import com.wapo.android.commons.config.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.SiteServiceConfigStub;

public class WapoConfigManager extends DefaultConfigManager {

    private static boolean D = BuildConfig.DEBUG;
    private static final String TAG = WapoConfigManager.class.getName();
    private ConfigManager configManager = ConfigManager.instance();
    private RemoteLogRepo remoteLogRepo;

    private final ConfigManager.ConfigFailureListener configFailureListener = (type, e) -> {
        Logger.e(TAG, "Config - Remote ( " + type.name() + " ) failed to load: " + e.getMessage());
        remoteLogRepo.e(new EventLog.Builder()
                .setModule(LogModules.CONFIG)
                .setMessage("Failed to load remote config: " + type.name())
                .setErrorMessage(e.getMessage())
                .build());
    };

    private final long ELEX_SUBNAV_TIME_INTERVAL = 60 * 1000 ;

    public WapoConfigManager(Context context, RemoteLogRepo remoteLogRepo) {
        this.remoteLogRepo = remoteLogRepo;
        loadConfigs(context);
    }

    public void loadConfigs(Context context) {
        //loadConfig(context, Constants.ConfigType.CONFIG);
        loadConfig(context, Constants.ConfigType.SECTIONS_BAR_CONFIG);
        loadConfig(context, Constants.ConfigType.SECTIONS_FEATURED_CONFIG);
        loadConfig(context, Constants.ConfigType.SECTIONS_AZ_CONFIG);
        loadConfig(context, Constants.ConfigType.SECTIONS_UNLISTED_CONFIG);
        loadConfig(context, Constants.ConfigType.SECTIONS_RECOMMENDED_CONFIG);
        if (D) {
            loadConfig(context, Constants.ConfigType.SECTIONS_BAR_TEST_CONFIG);
        }
    }

    private void loadConfig(Context context, Constants.ConfigType type) {
        ConfigManager.ConfigModel configModel = null;
        //TODO: This needs to be rewritten when we move Wapo's existing config into this new config format.
        //if (type == Constants.ConfigType.CONFIG) {
        //    configModel = new ConfigManager.ConfigModel(Config.class, R.raw.config, context.getString(R.string.configRemoteLocation));
        // } else

        SiteServiceConfigStub siteServiceConfigStub = com.washingtonpost.android.config.domain.manager.ConfigManager.Companion.getInstance().getConfig().getSiteServiceConfig();
        boolean isPhone = UIUtil.isPhone(context);
        //Fill in default values in case of config update issue.
        String sectionsBarLocation = siteServiceConfigStub.getSectionsBarConfigRemoteLocation();
        String sectionsFeaturedLocation = siteServiceConfigStub.getSectionsFeaturedConfigRemoteLocation();
        String sectionsAZLocation = siteServiceConfigStub.getSectionsAZConfigRemoteLocation();
        String sectionsBarTestLocation = siteServiceConfigStub.getSectionsBarTestConfigRemoteLocation();
        String sectionsUnlistedLocation = siteServiceConfigStub.getSectionsUnlistedConfigRemoteLocation();
        String sectionsRecommendedLocation = siteServiceConfigStub.getSectionsRecommendedConfigRemoteLocation();

        if (D) {
            Logger.d(TAG, String.format(
                    "Sections bar location: %s Featured location: %s Sections AZ location: %s Recommended location: %s, Is Phone: %b",
                    sectionsBarLocation, sectionsFeaturedLocation, sectionsAZLocation, sectionsRecommendedLocation, isPhone
            ));
        }

        //TODO: Confirm that this has the latest values for local files.
        if (type == Constants.ConfigType.SECTIONS_BAR_CONFIG) {
            configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.sections_bar_config, sectionsBarLocation);
        } else if (type == Constants.ConfigType.SECTIONS_FEATURED_CONFIG) {
            configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.sections_featured_config, sectionsFeaturedLocation);
        } else if (type == Constants.ConfigType.SECTIONS_AZ_CONFIG) {
            configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.sections_az_config, sectionsAZLocation);
        } else if (type == Constants.ConfigType.SECTIONS_BAR_TEST_CONFIG) {
            configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.sections_bar_test_config,  sectionsBarTestLocation);
        } else if (type == Constants.ConfigType.SECTIONS_UNLISTED_CONFIG) {
            configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.sections_unlisted_config,  sectionsUnlistedLocation);
        } else if (type == Constants.ConfigType.SECTIONS_RECOMMENDED_CONFIG) {
            configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.sections_recommended_config,  sectionsRecommendedLocation);
        } else {
            return;
        }
        configManager.addConfigModel(type, configModel);
        configManager.loadLocalConfig(context, type);
        configManager.loadRemoteConfig(context, type, configFailureListener);
    }

    public void loadElectionConfig(Context context ,Constants.ConfigType type, String siteMapURL)
    {
        ConfigManager.ConfigModel configModel = new ConfigManager.ConfigModel(SiteServiceConfig.class, R.raw.section_election_config,  siteMapURL);
        configManager.addConfigModel(type, configModel);
        configManager.loadLocalConfig(context, type);
        ContentManager contentManager = FlagshipApplication.getInstance().getContentManager();
        long savedTime = contentManager.getSubNavElectionLoadTime();
        if(getTimeDifference(savedTime))
        {
            contentManager.setSubNavElectionLoadTime();
            configManager.loadRemoteConfig(context, type, configFailureListener);
        }
    }

   private boolean getTimeDifference(Long givenTimeInMillis) {
        long currentTimeInMillis = System.currentTimeMillis();
        long timeDifference = currentTimeInMillis - givenTimeInMillis;
        return timeDifference >= ELEX_SUBNAV_TIME_INTERVAL;
    }

    //TODO: This needs to be rewritten when we move Wapo's existing config into this new config format.
    @Override
    public BaseConfig getAppConfig() {
        return null;
        //return (BaseConfig) configManager.getConfigOfType(Constants.ConfigType.CONFIG);
    }

    @Override
    public SiteServiceConfig getSectionsBarConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.SECTIONS_BAR_CONFIG);
    }

    @Override
    public SiteServiceConfig getSectionsFeaturedConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.SECTIONS_FEATURED_CONFIG);
    }

    @Override
    public SiteServiceConfig getSectionsAZConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.SECTIONS_AZ_CONFIG);
    }

    @Override
    public SiteServiceConfig getSectionsBarTestConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.SECTIONS_BAR_TEST_CONFIG);
    }

    @Override
    public SiteServiceConfig getSectionsUnlistedConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.SECTIONS_UNLISTED_CONFIG);
    }

    @Override
    public SiteServiceConfig getSectionsRecommendedConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.SECTIONS_RECOMMENDED_CONFIG);
    }

    @Override
    public SiteServiceConfig getElectionConfig() {
        return (SiteServiceConfig) configManager.getConfigOfType(Constants.ConfigType.ELECTION_SUB_NAV_CONFIG);
    }

    public void deleteLocalConfigs(Context context) {
        if (D) {
            ConfigHelper.clearOldConfig(context, Constants.ConfigType.SECTIONS_BAR_CONFIG);
            ConfigHelper.clearOldConfig(context, Constants.ConfigType.SECTIONS_FEATURED_CONFIG);
            ConfigHelper.clearOldConfig(context, Constants.ConfigType.SECTIONS_AZ_CONFIG);
            ConfigHelper.clearOldConfig(context, Constants.ConfigType.SECTIONS_UNLISTED_CONFIG);
            ConfigHelper.clearOldConfig(context, Constants.ConfigType.SECTIONS_RECOMMENDED_CONFIG);
        }
    }
}
