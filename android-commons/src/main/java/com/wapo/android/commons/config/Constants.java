package com.wapo.android.commons.config;

public class Constants {

    public enum ConfigType {
        TEST_CONFIG, TEST_SECURE_CONFIG,
        CONFIG,
        SECURE_CONFIG,
        // Use SECTION_CONFIG when there is only one site service json for both top bar and menu
        // or else use below SECTIONS_BAR_CONFIG and SECTIONS_MENU_CONFIG config constants.
        SECTION_CONFIG,
        SECTIONS_BAR_CONFIG, SECTIONS_FEATURED_CONFIG, SECTIONS_AZ_CONFIG, SECTIONS_BAR_TEST_CONFIG,
        SECTIONS_UNLISTED_CONFIG, SECTIONS_RECOMMENDED_CONFIG,
        SETTINGS_CONFIG, SETTINGS_OFFLINE_CONFIG,
        ELECTION_SUB_NAV_CONFIG,
        UNKNOWN
    }

    public static String GENERAL_PREFERENCES = "GeneralPreferences";
    public static String PREF_KEY_CURRENT_VERSION_CODE = "pref.CurrentVersionCode";
    public static String VERSION_KEY_NAME = "version";
}
