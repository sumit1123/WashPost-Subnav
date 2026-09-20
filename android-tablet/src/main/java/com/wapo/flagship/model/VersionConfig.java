package com.wapo.flagship.model;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by kilarib on 9/19/13.
 */
public class VersionConfig {

    // TODO: Expand this to make it more dynamic

    private static final String TAG_APP_NAME = "com.washingtonpost.android";
    private static final String TAG_VERSION_SERVICE = "VersionService";
    private static final String TAG_MAX_VERSION_TAG = "max_available_version_tag";
    private static final String TAG_FORCE_UPDATE = "force_update";
    private static final String TAG_UPDATE_PROMT_DISABLED = "upgrade_prompt_disabled";
    private static final String TAG_ADS_DISABLED = "ads_disabled";
    private static final String TAG_FEED_CONFIG_NUMBER = "feed_config_build_number";
    private static final String TAG_PAYWALL_SERVICE = "PaywallService";
    private static final String TAG_PAYWALL_LIMIT = "limit";
    private static final String TAG_PAYWALL_TURNED_ON = "turned_on";
    private static final String TAG_PAYWALL_START_DATE = "start_date";
    private static final String TAG_PAYWALL_EXPIRY_DATE = "expiry_date";
    private static final String TAG_PAYWALL_WARNING_POINTS = "warning_points";

    // VersionService variables
    private String maxVersionTag = null;
    private boolean forceUpdate = false;
    private String maxVersion = null;
    private String minVersion = null;
    private String minVersionTag = null;

    public boolean isPromptDisabled() {
        return promptDisabled;
    }

    public void setPromptDisabled(boolean promptDisabled) {
        this.promptDisabled = promptDisabled;
    }

    private boolean promptDisabled = true;
    private boolean adsDisabled = false;
    private int feedConfigNumber=0;
    private String warningPoints=null;

    public boolean isAdsDisabled() {
        return adsDisabled;
    }

    public void setAdsDisabled(boolean adsDisabled) {
        this.adsDisabled = adsDisabled;
    }
    // PaywallService variables
    private int limit=20;
    private Date startDate;
    private Date endDate;
    private boolean pwTurnedOn=false;

    public boolean isPwTurnedOn() {
        return pwTurnedOn;
    }

    public void setPwTurnedOn(boolean pwTurnedOn) {
        this.pwTurnedOn = pwTurnedOn;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getFeedConfigNumber() {
        return feedConfigNumber;
    }

    public void setFeedConfigNumber(int feedConfigNumber) {
        this.feedConfigNumber = feedConfigNumber;
    }

    public String getMaxVersionTag() {
        return maxVersionTag;
    }

    public void setMaxVersionTag(String maxVersionTag) {
        this.maxVersionTag = maxVersionTag;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public String getMaxVersion() {
        return maxVersion;
    }

    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }

    public String getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    public String getMinVersionTag() {
        return minVersionTag;
    }

    public void setMinVersionTag(String minVersionTag) {
        this.minVersionTag = minVersionTag;
    }

    public String getWarningPoints() {
        return warningPoints;
    }

    public void setWarningPoints(String warningPoints) {
        this.warningPoints = warningPoints;
    }

    public static VersionConfig configFromJSONObject(JSONObject obj, Context context) throws JSONException {
        VersionConfig config = new VersionConfig();

        JSONObject appObject = obj.getJSONObject(TAG_APP_NAME);

        JSONObject versionService = appObject
                .getJSONObject(TAG_VERSION_SERVICE);

        //todo: when paywall is turned on uncomment these
//        JSONObject paywallService = appObject
//                .getJSONObject(TAG_PAYWALL_SERVICE);

//        int paywallLimit = paywallService.getInt(TAG_PAYWALL_LIMIT);

//        boolean pwTurnedOn = paywallService.getBoolean(TAG_PAYWALL_TURNED_ON);

        String maxVersion = versionService.getString(TAG_MAX_VERSION_TAG);

        boolean forceUpdate = versionService.getBoolean(TAG_FORCE_UPDATE);

        boolean adsDisabled = versionService.getBoolean(TAG_ADS_DISABLED);

        boolean promptDisabled = versionService.getBoolean(TAG_UPDATE_PROMT_DISABLED);

//        String reminderPoints = paywallService.getString(TAG_PAYWALL_WARNING_POINTS);

//        if(reminderPoints!=null)
//        {
//            config.setWarningPoints(reminderPoints);
//        }


        int feedConfigNumber = versionService
                .getInt(TAG_FEED_CONFIG_NUMBER);

        if (maxVersion != null) {
            config.setMaxVersionTag(maxVersion);
        }



        config.setAdsDisabled(adsDisabled);
        config.setPromptDisabled(promptDisabled);
        config.setFeedConfigNumber(feedConfigNumber);
//        config.setPwTurnedOn(pwTurnedOn);
//        config.setLimit(paywallLimit);
        config.setForceUpdate(forceUpdate);

        return config;
    }

}
